package it.bosler.polyphoneme.data.ipa

import android.content.Context
import android.util.Log
import it.bosler.polyphoneme.model.Paragraph
import it.bosler.polyphoneme.model.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import it.bosler.polyphoneme.model.LanguageRegions
import java.text.Normalizer

class DictionaryIpaService(private val context: Context) : IpaService {

    private val dictionaries = mutableMapOf<String, Map<String, String>>()
    private val accentNormalizedDicts = mutableMapOf<String, Map<String, String>>()
    private val classifiers = mutableMapOf<String, HomographClassifier>()
    private val mutex = Mutex()
    private var regionOverrides: Map<String, String> = emptyMap()

    private val languageFiles = mapOf(
        "en" to "ipa-en.txt",
        "de" to "ipa-de.txt",
        "fr" to "ipa-fr.txt",
        "es" to "ipa-es.txt",
        "it" to "ipa-it.txt",
        "pt" to "ipa-pt.txt",
        "nl" to "ipa-nl.txt",
        "ru" to "ipa-ru.txt",
        "ja" to "ipa-ja.txt",
        "zh" to "ipa-zh.txt",
    )

    private val classifierFiles = mapOf(
        "en" to "en_homograph_classifiers.json",
        "de" to "de_homograph_classifiers.json",
        "es" to "es_homograph_classifiers.json",
        "fr" to "fr_homograph_classifiers.json",
        "it" to "it_homograph_classifiers.json",
        "pt" to "pt_homograph_classifiers.json",
        "nl" to "nl_homograph_classifiers.json",
        "ru" to "ru_homograph_classifiers.json",
        "ja" to "ja_homograph_classifiers.json",
        "zh" to "zh_homograph_classifiers.json",
    )

    override fun isLanguageSupported(language: String): Boolean {
        return normalizeLanguage(language) in languageFiles
    }

    override fun setRegionOverrides(regions: Map<String, String>) {
        this.regionOverrides = regions
    }

    private fun stripDiacritics(s: String): String {
        val normalized = Normalizer.normalize(s, Normalizer.Form.NFD)
        return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
    }

    private fun applyRegion(ipa: String, lang: String): String {
        val region = regionOverrides[lang] ?: return ipa
        return LanguageRegions.applyIpaTransformations(ipa, lang, region)
    }

    override suspend fun transcribe(words: List<String>, language: String): Map<String, String> {
        val lang = normalizeLanguage(language)
        Log.d("IpaService", "Transcribing ${words.size} words for language: $language (normalized: $lang)")
        val dict = loadDictionary(lang) ?: run {
            Log.w("IpaService", "No dictionary found for language: $lang")
            return emptyMap()
        }
        val normalizedDict = accentNormalizedDicts[lang]
        Log.d("IpaService", "Dictionary loaded for $lang with ${dict.size} entries")
        val suffixes = suffixesFor(lang)
        val result = mutableMapOf<String, String>()
        for (word in words) {
            val key = word.lowercase()
            val ipa = lookupWithFallback(dict, normalizedDict, key, lang, suffixes)
            if (ipa != null) {
                result[key] = applyRegion(ipa, lang)
            }
        }
        result.entries.take(5).forEach { (word, ipa) ->
            Log.d("IpaService", "  $word → $ipa")
        }
        return result
    }

    override suspend fun transcribeInContext(
        paragraphs: List<Paragraph>,
        language: String,
    ): List<Paragraph> {
        val lang = normalizeLanguage(language)
        val dict = loadDictionary(lang) ?: return paragraphs
        val normalizedDict = accentNormalizedDicts[lang]
        val classifier = loadClassifier(lang)
        val suffixes = suffixesFor(lang)

        return paragraphs.map { paragraph ->
            val tokens = paragraph.tokens
            paragraph.copy(
                tokens = tokens.mapIndexed { index, token ->
                    val key = token.word.lowercase()
                    val rawIpa = dict[key]
                        ?: normalizedDict?.get(stripDiacritics(key))

                    if (rawIpa != null && classifier != null) {
                        // Check if this word is a homograph
                        val disambiguation = classifier.disambiguate(key, tokens, index)
                        if (disambiguation != null) {
                            token.copy(
                                ipa = applyRegion(disambiguation.ipa, lang),
                                isDisambiguated = true,
                                alternativePronunciations = disambiguation.alternatives.map { applyRegion(it, lang) },
                            )
                        } else {
                            // Not a homograph, use first pronunciation
                            token.copy(ipa = applyRegion(extractFirst(rawIpa), lang))
                        }
                    } else if (rawIpa != null) {
                        token.copy(ipa = applyRegion(extractFirst(rawIpa), lang))
                    } else {
                        // Try fallback lookups
                        val fallback = lookupWithFallback(dict, normalizedDict, key, lang, suffixes)
                        if (fallback != null) token.copy(ipa = applyRegion(fallback, lang)) else token
                    }
                }
            )
        }
    }

    private fun lookupWithFallback(
        dict: Map<String, String>,
        normalizedDict: Map<String, String>?,
        word: String,
        lang: String,
        suffixes: List<String>,
    ): String? {
        // Direct lookup
        dict[word]?.let { raw ->
            return extractFirst(raw)
        }

        // Accent-normalized fallback (e.g., "electronica" → "electrónica")
        normalizedDict?.get(stripDiacritics(word))?.let { raw ->
            return extractFirst(raw)
        }

        // Try stripping suffixes (longest first)
        for (suffix in suffixes) {
            if (word.endsWith(suffix) && word.length > suffix.length + 1) {
                val stem = word.dropLast(suffix.length)
                dict[stem]?.let { return extractFirst(it) }
                normalizedDict?.get(stripDiacritics(stem))?.let { return extractFirst(it) }
            }
        }

        // Try removing trailing 'e' for German-style words (e.g., "grosse" → "gross")
        if (word.length > 3 && word.last() == 'e') {
            val withoutE = word.dropLast(1)
            dict[withoutE]?.let { return extractFirst(it) }
        }

        // Try poetic elision: "vorgeschriebne" → "vorgeschriebene"
        tryUnelide(dict, word)?.let { return it }

        // Try compound word splitting (mainly useful for German)
        if (lang == "de" && word.length >= 6) {
            trySplitCompound(dict, word)?.let { return it }
        }

        // Fallback: built-in common words
        builtinIpa[lang]?.get(word)?.let { return it }

        return null
    }

    private fun tryUnelide(dict: Map<String, String>, word: String): String? {
        val elisionPatterns = listOf("ne" to "ene", "ner" to "ener", "nes" to "enes", "nem" to "enem")
        for ((suffix, expanded) in elisionPatterns) {
            if (word.endsWith(suffix) && word.length > suffix.length + 2) {
                val stem = word.dropLast(suffix.length)
                val expandedWord = stem + expanded
                dict[expandedWord]?.let { return extractFirst(it) }
            }
        }
        return null
    }

    private fun trySplitCompound(dict: Map<String, String>, word: String): String? {
        for (i in 3..(word.length - 3)) {
            val left = word.substring(0, i)
            val right = word.substring(i)

            val leftIpa = dict[left]?.let { extractFirst(it) }
            val rightIpa = dict[right]?.let { extractFirst(it) }
            if (leftIpa != null && rightIpa != null) {
                return "$leftIpa$rightIpa"
            }

            // Fugen-s
            if (right.startsWith("s") && right.length > 3) {
                val rightAfterS = right.substring(1)
                val rightAfterSIpa = dict[rightAfterS]?.let { extractFirst(it) }
                if (leftIpa != null && rightAfterSIpa != null) {
                    return "${leftIpa}s$rightAfterSIpa"
                }
            }

            // Fugen-n
            if (right.startsWith("n") && right.length > 3) {
                val rightAfterN = right.substring(1)
                val rightAfterNIpa = dict[rightAfterN]?.let { extractFirst(it) }
                if (leftIpa != null && rightAfterNIpa != null) {
                    return "${leftIpa}n$rightAfterNIpa"
                }
            }

            // Fugen-en
            if (right.startsWith("en") && right.length > 4) {
                val rightAfterEn = right.substring(2)
                val rightAfterEnIpa = dict[rightAfterEn]?.let { extractFirst(it) }
                if (leftIpa != null && rightAfterEnIpa != null) {
                    return "${leftIpa}ən$rightAfterEnIpa"
                }
            }
        }

        return null
    }

    private fun extractFirst(ipa: String): String {
        val first = ipa.split(",").first().trim()
        return first.removePrefix("/").removeSuffix("/").trim()
    }

    private suspend fun loadDictionary(lang: String): Map<String, String>? {
        mutex.withLock {
            dictionaries[lang]?.let { return it }
        }

        val fileName = languageFiles[lang] ?: return null

        val result = withContext(Dispatchers.IO) {
            try {
                val map = HashMap<String, String>(130_000)
                val normalizedMap = HashMap<String, String>(130_000)
                context.assets.open(fileName).bufferedReader().useLines { lines ->
                    for (line in lines) {
                        val tab = line.indexOf('\t')
                        if (tab > 0 && tab < line.length - 1) {
                            val word = line.substring(0, tab).lowercase()
                            val ipa = line.substring(tab + 1).trim()
                            map[word] = ipa
                            // Build accent-normalized lookup (only if different from original)
                            val stripped = stripDiacritics(word)
                            if (stripped != word && stripped !in normalizedMap) {
                                normalizedMap[stripped] = ipa
                            }
                        }
                    }
                }
                Pair(map as Map<String, String>, normalizedMap as Map<String, String>)
            } catch (_: Exception) {
                null
            }
        } ?: return null

        mutex.withLock {
            dictionaries[lang] = result.first
            accentNormalizedDicts[lang] = result.second
        }
        return result.first
    }

    private suspend fun loadClassifier(lang: String): HomographClassifier? {
        mutex.withLock {
            classifiers[lang]?.let { return it }
        }

        val fileName = classifierFiles[lang] ?: return null

        val classifier = withContext(Dispatchers.IO) {
            try {
                val jsonStr = context.assets.open(fileName).bufferedReader().readText()
                val json = Json.parseToJsonElement(jsonStr).jsonObject
                HomographClassifier.parse(json, lang)
            } catch (e: Exception) {
                Log.w("IpaService", "Failed to load classifier for $lang: ${e.message}")
                null
            }
        } ?: return null

        mutex.withLock {
            classifiers[lang] = classifier
        }
        return classifier
    }

    private fun normalizeLanguage(language: String): String {
        return language.lowercase().split("-", "_").first()
    }

    private fun suffixesFor(lang: String): List<String> = when (lang) {
        "de" -> listOf(
            "ungen", "eten", "heit", "keit", "lich", "isch", "chen",
            "lein", "sten", "ste", "est", "end", "ern", "ung",
            "ens", "tes", "ter", "ten", "tem", "ene",
            "en", "er", "es", "em", "et", "st", "te", "ig",
            "e", "s", "t", "n",
        )
        "en" -> listOf(
            "iness", "ation", "ness", "ment", "ting", "ling", "able", "ible",
            "ence", "ance", "eous", "ious",
            "ing", "ity", "ous", "ful", "ess", "ism", "ist",
            "ly", "ed", "er", "es", "al",
            "s", "y",
        )
        "fr" -> listOf(
            "ement", "ation", "ment", "tion", "ence", "ance",
            "aine", "enne", "elle", "euse", "eux",
            "ent", "ant", "ais", "ait", "aux",
            "es", "er", "ez", "ée",
            "e", "s",
        )
        "es" -> listOf(
            "mente", "ación", "iones", "ando", "endo", "idos", "adas",
            "ción", "idad", "mente",
            "ado", "ido", "aba", "ían",
            "ar", "er", "ir", "os", "as", "es",
            "o", "a", "s",
        )
        "it" -> listOf(
            "mente", "zione", "zioni", "ando", "endo", "ato", "ata",
            "ati", "ate", "ito", "ita", "iti", "ite",
            "are", "ere", "ire", "ono",
            "ai", "ei", "oi",
            "o", "a", "e", "i",
        )
        "pt" -> listOf(
            "mente", "ação", "ções", "ando", "endo", "ado", "ada",
            "ados", "adas", "ido", "ida", "idos", "idas",
            "ar", "er", "ir", "os", "as",
            "o", "a", "s",
        )
        "nl" -> listOf(
            "heid", "lijk", "isch", "baar", "ling",
            "sten", "ste", "end", "erd",
            "en", "er", "es", "te",
            "e", "s",
        )
        "ru" -> listOf(
            "ность", "ение", "ание", "ство", "ский", "ская", "ское",
            "ного", "ному", "ными", "ной", "ных",
            "ать", "ять", "ить", "еть",
            "ов", "ев", "ам", "ом", "ей",
            "а", "о", "е", "и", "у", "ы",
        )
        else -> emptyList()
    }

    // Built-in IPA for common words missing from ipa-dict
    private val builtinIpa: Map<String, Map<String, String>> = mapOf(
        "de" to mapOf(
            "bruder" to "ˈbʁuːdɐ",
            "brüder" to "ˈbʁyːdɐ",
            "schwester" to "ˈʃvɛstɐ",
            "sohn" to "zoːn",
            "söhne" to "ˈzøːnə",
            "tochter" to "ˈtɔxtɐ",
            "töchter" to "ˈtœçtɐ",
            "paradies" to "paʁaˈdiːs",
            "sphäre" to "ˈsfɛːʁə",
            "sphären" to "ˈsfɛːʁən",
            "engel" to "ˈɛŋl̩",
            "mensch" to "mɛnʃ",
            "herz" to "hɛʁts",
            "seele" to "ˈzeːlə",
            "geist" to "ɡaɪ̯st",
            "gott" to "ɡɔt",
            "wort" to "vɔʁt",
            "worte" to "ˈvɔʁtə",
            "kraft" to "kʁaft",
            "ding" to "dɪŋ",
            "weise" to "ˈvaɪ̯zə",
            "stille" to "ˈʃtɪlə",
            "stimme" to "ˈʃtɪmə",
            "klang" to "klaŋ",
            "glanz" to "ɡlants",
            "schatten" to "ˈʃatn̩",
            "flamme" to "ˈflamə",
            "erde" to "ˈeːɐ̯də",
            "traum" to "tʁaʊ̯m",
            "träume" to "ˈtʁɔɪ̯mə",
            "stern" to "ʃtɛʁn",
            "sterne" to "ˈʃtɛʁnə",
            "blume" to "ˈbluːmə",
            "blumen" to "ˈbluːmən",
            "freude" to "ˈfʁɔɪ̯də",
            "liebe" to "ˈliːbə",
            "leben" to "ˈleːbn̩",
            "tod" to "toːt",
            "himmlisch" to "ˈhɪmlɪʃ",
            "himmlischen" to "ˈhɪmlɪʃn̩",
            "ewig" to "ˈeːvɪç",
            "ewiger" to "ˈeːvɪɡɐ",
            "ewiges" to "ˈeːvɪɡəs",
            "göttlich" to "ˈɡœtlɪç",
            "heilig" to "ˈhaɪ̯lɪç",
            "dunkel" to "ˈdʊŋkl̩",
            "dunkle" to "ˈdʊŋklə",
            "schön" to "ʃøːn",
            "schöne" to "ˈʃøːnə",
            "schönen" to "ˈʃøːnən",
            "tief" to "tiːf",
            "tiefe" to "ˈtiːfə",
            "tiefen" to "ˈtiːfn̩",
            "sanft" to "zanft",
            "stark" to "ʃtaʁk",
            "groß" to "ɡʁoːs",
            "große" to "ˈɡʁoːsə",
            "großen" to "ˈɡʁoːsn̩",
            "klein" to "klaɪ̯n",
            "kleine" to "ˈklaɪ̯nə",
            "kleinen" to "ˈklaɪ̯nən",
        ),
    )
}

/**
 * Homograph disambiguation engine using context keyword matching.
 * Supports two JSON formats:
 * - "classes" format (EN, FR, DE, IT, PT, NL, RU, ZH): classes with pronunciation + keywords
 * - "readings" format (JA): readings with frequency + keywords
 */
class HomographClassifier private constructor(
    private val entries: Map<String, HomographEntry>,
) {

    data class DecisionRule(
        val score: Float,
        val feature: String,
        val classId: String,
    )

    data class HomographEntry(
        val classes: List<HomographClass>,
        val defaultPronunciation: String,
        val decisionRules: List<DecisionRule> = emptyList(),
    )

    data class HomographClass(
        val pronunciation: String,
        val keywords: List<String>,
        val frequency: Float = 0f,
        val classId: String = "",
    )

    data class DisambiguationResult(
        val ipa: String,
        val alternatives: List<String> = emptyList(),
        val selectedClassId: String = "",
    )

    fun getEntry(word: String): HomographEntry? = entries[word]

    /**
     * Try to disambiguate a word using surrounding context.
     * Returns null if the word is not a known homograph.
     */
    fun disambiguate(
        word: String,
        tokens: List<Token>,
        wordIndex: Int,
    ): DisambiguationResult? {
        val entry = entries[word] ?: return null
        if (entry.classes.size <= 1) {
            return DisambiguationResult(
                ipa = entry.defaultPronunciation,
                selectedClassId = entry.classes.firstOrNull()?.classId ?: "",
            )
        }

        // Helper to build result with alternatives and class info
        fun resultForClass(cls: HomographClass): DisambiguationResult {
            val alts = entry.classes.map { it.pronunciation }.filter { it != cls.pronunciation }
            return DisambiguationResult(ipa = cls.pronunciation, alternatives = alts, selectedClassId = cls.classId)
        }
        // Shorthand: find class by pronunciation (for existing call sites)
        fun result(selectedIpa: String): DisambiguationResult {
            val cls = entry.classes.firstOrNull { it.pronunciation == selectedIpa }
            return if (cls != null) resultForClass(cls)
            else DisambiguationResult(ipa = selectedIpa, selectedClassId = "")
        }

        // Compound word lookup for CJK: check if adjacent characters form a compound
        // Only applies when the target word is a CJK character (1-2 chars)
        val nextWord = if (wordIndex + 1 < tokens.size) tokens[wordIndex + 1].word else ""
        val prevWord = if (wordIndex > 0) tokens[wordIndex - 1].word.lowercase() else ""
        if (word.length <= 2 && word.any { it.code > 0x2E80 }) {
            val adjacentChars = setOf(prevWord, nextWord).filter { it.isNotEmpty() }
            // Find which classes have adjacent-character keyword matches
            val adjacentMatches = mutableListOf<HomographClass>()
            for (cls in entry.classes) {
                val hasAdjacentMatch = cls.keywords.any { kw ->
                    kw.length <= 2 && adjacentChars.any { adj -> kw in adj || adj in kw }
                }
                if (hasAdjacentMatch) adjacentMatches.add(cls)
            }
            // Only use compound lookup if exactly one class matches
            if (adjacentMatches.size == 1) {
                return resultForClass(adjacentMatches[0])
            }
        }

        // German/Dutch separable verb detection: only fire when we have
        // positive evidence that the prefix particle is separated
        val hasSepInsep = entry.classes.any { it.classId.lowercase() in setOf("separable", "inseparable") }
        if (hasSepInsep) {
            val prefixes = listOf("über", "unter", "wieder", "durch", "um", "vor")
            val prefix = prefixes.firstOrNull { word.startsWith(it) }
            if (prefix != null) {
                val allWords = tokens.map { it.word.lowercase() }
                val prefixSeparated = allWords.any { it == prefix && it != word }
                val hasZuInfix = word.contains("${prefix}zu")
                // Only return separable if we have positive evidence
                if (prefixSeparated || hasZuInfix) {
                    val sepClass = entry.classes.firstOrNull { it.classId == "separable" }
                    if (sepClass != null) return resultForClass(sepClass)
                }
                // Otherwise fall through to keyword scoring — don't assume inseparable
            }
        }

        // Yarowsky decision rules: scan top-to-bottom, first match wins
        if (entry.decisionRules.isNotEmpty()) {
            val prevToken = if (wordIndex > 0) tokens[wordIndex - 1].word.lowercase() else ""
            val nextToken = if (wordIndex + 1 < tokens.size) tokens[wordIndex + 1].word.lowercase() else ""
            val windowWords = mutableSetOf<String>()
            for (i in (wordIndex - 5)..(wordIndex + 5)) {
                if (i in tokens.indices && i != wordIndex) {
                    windowWords.add(tokens[i].word.lowercase())
                }
            }

            for (rule in entry.decisionRules) {
                val matched = when {
                    rule.feature.startsWith("-1:") -> prevToken == rule.feature.removePrefix("-1:")
                    rule.feature.startsWith("+1:") -> nextToken == rule.feature.removePrefix("+1:")
                    rule.feature.startsWith("bi:") -> {
                        val parts = rule.feature.removePrefix("bi:").split("_", limit = 2)
                        parts.size == 2 && prevToken == parts[0] && nextToken == parts[1]
                    }
                    rule.feature.startsWith("w:") -> rule.feature.removePrefix("w:") in windowWords
                    else -> false
                }
                if (matched) {
                    // Find the class matching this rule's classId
                    val matchedClass = entry.classes.firstOrNull { it.classId == rule.classId }
                    if (matchedClass != null) {
                        return resultForClass(matchedClass)
                    }
                }
            }
        }

        // POS-based disambiguation: fallback when no decision rule matched
        val hasPosTags = entry.classes.any { id ->
            val c = id.classId.lowercase()
            c.contains("nou") || c.contains("vrb") || c.contains("verb") ||
            c.contains("adj") || c.contains("noun") || c.contains("adverb")
        }
        if (hasPosTags) {
            val detectedPos = detectPos(prevWord)
            if (detectedPos != null) {
                // Only use POS when exactly one class matches — skip if ambiguous
                val posMatches = entry.classes.filter { classMatchesPos(it.classId, detectedPos) }
                if (posMatches.size == 1) {
                    return resultForClass(posMatches[0])
                }
            }
        }

        // Build position-indexed context: map from word -> distance from target
        val contextWordDistances = mutableMapOf<String, Int>()
        for (i in tokens.indices) {
            if (i != wordIndex) {
                val w = tokens[i].word.lowercase()
                val dist = kotlin.math.abs(i - wordIndex)
                // Keep closest distance for each word
                val existing = contextWordDistances[w]
                if (existing == null || dist < existing) {
                    contextWordDistances[w] = dist
                }
            }
        }

        // Also build a full context string for substring matching (CJK, compound words)
        val contextString = tokens.filterIndexed { i, _ -> i != wordIndex }
            .joinToString("") { it.word.lowercase() }

        // Build set of ALL keywords across all classes to identify shared/ambiguous ones
        val keywordToClasses = mutableMapOf<String, MutableSet<Int>>()
        for ((classIdx, cls) in entry.classes.withIndex()) {
            for (keyword in cls.keywords) {
                keywordToClasses.getOrPut(keyword.lowercase()) { mutableSetOf() }.add(classIdx)
            }
        }

        // Score each class by keyword matches with proximity weighting
        val maxFreq = entry.classes.maxOf { it.frequency }.coerceAtLeast(1f)
        var bestClass = entry.classes[0]
        var bestScore = -1f

        for ((classIdx, cls) in entry.classes.withIndex()) {
            var keywordScore = 0f
            for (keyword in cls.keywords) {
                val kw = keyword.lowercase()
                val dist = contextWordDistances[kw]
                val matched = dist != null || kw in contextString
                if (!matched) continue

                val isExclusive = keywordToClasses[kw]?.size == 1
                // Proximity weight: adjacent=3x, within 3=2x, within 6=1.5x, else=1x
                val actualDist = dist ?: tokens.size // substring match gets low proximity
                val proximity = when {
                    actualDist <= 1 -> 3f
                    actualDist <= 3 -> 2f
                    actualDist <= 6 -> 1.5f
                    else -> 1f
                }
                val exclusivity = if (isExclusive) 5f else 0.5f
                keywordScore += exclusivity * proximity
            }
            val score = keywordScore + cls.frequency / maxFreq
            if (score > bestScore) {
                bestScore = score
                bestClass = cls
            }
        }

        return resultForClass(bestClass)
    }

    private fun detectPos(precedingWord: String): String? {
        val nounSignals = setOf(
            "the", "a", "an", "this", "that", "these", "those",
            "my", "your", "his", "her", "its", "our", "their",
            "some", "any", "no", "each", "every", "much", "many",
            "fresh", "new", "old", "good", "bad", "great", "big",
            "world", "war", "front", // common noun modifiers
        )
        val verbSignals = setOf(
            "to", "will", "would", "shall", "should", "can", "could",
            "may", "might", "must", "do", "does", "did",
            "please", "let", "cannot", "don't", "doesn't", "didn't",
            "not", "also", "always", "never", "often",
        )
        return when (precedingWord) {
            in nounSignals -> "noun"
            in verbSignals -> "verb"
            else -> null
        }
    }

    private fun classMatchesPos(classId: String, pos: String): Boolean {
        val id = classId.lowercase()
        return when (pos) {
            "noun" -> id.contains("nou") || id.contains("adj") || id.contains("noun")
            "verb" -> id.contains("vrb") || id.contains("verb")
            else -> false
        }
    }

    companion object {
        fun parse(json: JsonObject, lang: String): HomographClassifier {
            val entries = mutableMapOf<String, HomographEntry>()

            for ((word, element) in json) {
                val obj = element.jsonObject
                val classes = mutableListOf<HomographClass>()

                if ("readings" in obj) {
                    // JA format: { readings: { reading: { frequency, keywords } }, default }
                    val readings = obj["readings"]!!.jsonObject
                    val default = obj["default"]?.jsonPrimitive?.content
                    for ((reading, readingObj) in readings) {
                        val r = readingObj.jsonObject
                        val freq = r["frequency"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f
                        val keywords = r["keywords"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                        classes.add(HomographClass(
                            pronunciation = reading,
                            keywords = keywords,
                            frequency = freq,
                            classId = reading,
                        ))
                    }
                    classes.sortByDescending { it.frequency }
                    val defaultPron = default ?: classes.firstOrNull()?.pronunciation ?: ""
                    val rules = parseDecisionRules(obj)
                    entries[word] = HomographEntry(classes, defaultPron, rules)
                } else if ("classes" in obj) {
                    // EN/FR/DE/IT/PT/NL/RU/ZH format: { classes: { id: { pronunciation, keywords, example_count? } } }
                    val classesObj = obj["classes"]!!.jsonObject
                    for ((classId, classObj) in classesObj) {
                        val c = classObj.jsonObject
                        val pronunciation = c["pronunciation"]?.jsonPrimitive?.content ?: continue
                        val keywords = c["keywords"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                        val exampleCount = c["example_count"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 1f
                        classes.add(HomographClass(
                            pronunciation = pronunciation,
                            keywords = keywords,
                            frequency = exampleCount,
                            classId = classId,
                        ))
                    }
                    classes.sortByDescending { it.frequency }
                    val defaultPron = classes.firstOrNull()?.pronunciation ?: ""
                    val rules = parseDecisionRules(obj)
                    entries[word] = HomographEntry(classes, defaultPron, rules)
                }
            }

            Log.d("IpaService", "Loaded classifier for $lang: ${entries.size} homographs")
            return HomographClassifier(entries)
        }

        private fun parseDecisionRules(obj: JsonObject): List<DecisionRule> {
            val rulesArray = obj["decision_rules"]?.jsonArray ?: return emptyList()
            return rulesArray.map { ruleElement ->
                val r = ruleElement.jsonObject
                DecisionRule(
                    score = r["score"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f,
                    feature = r["feature"]?.jsonPrimitive?.content ?: "",
                    classId = r["class"]?.jsonPrimitive?.content ?: "",
                )
            }
        }
    }
}
