package it.bosler.polyphoneme.data.ipa

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DictionaryIpaService(private val context: Context) : IpaService {

    private val dictionaries = mutableMapOf<String, Map<String, String>>()
    private val mutex = Mutex()

    private val languageFiles = mapOf(
        "en" to "ipa-en.txt",
        "de" to "ipa-de.txt",
        "fr" to "ipa-fr.txt",
        "es" to "ipa-es.txt",
    )

    override fun isLanguageSupported(language: String): Boolean {
        return normalizeLanguage(language) in languageFiles
    }

    override suspend fun transcribe(words: List<String>, language: String): Map<String, String> {
        val lang = normalizeLanguage(language)
        Log.d("IpaService", "Transcribing ${words.size} words for language: $language (normalized: $lang)")
        val dict = loadDictionary(lang) ?: run {
            Log.w("IpaService", "No dictionary found for language: $lang")
            return emptyMap()
        }
        Log.d("IpaService", "Dictionary loaded for $lang with ${dict.size} entries")
        val suffixes = suffixesFor(lang)
        val result = mutableMapOf<String, String>()
        for (word in words) {
            val key = word.lowercase()
            val ipa = lookupWithFallback(dict, key, lang, suffixes)
            if (ipa != null) {
                result[key] = ipa
            }
        }
        // Log a few sample results for debugging
        result.entries.take(5).forEach { (word, ipa) ->
            Log.d("IpaService", "  $word → $ipa")
        }
        return result
    }

    private fun lookupWithFallback(
        dict: Map<String, String>,
        word: String,
        lang: String,
        suffixes: List<String>,
    ): String? {
        // Direct lookup
        dict[word]?.let { raw ->
            val result = extractFirst(raw)
            if (word == "die" || word == "der" || word == "und") {
                Log.d("IpaService", "LOOKUP '$word': raw='$raw' → extracted='$result' (codepoints: ${result.map { it.code.toString(16) }})")
            }
            return result
        }

        // Try stripping suffixes (longest first)
        for (suffix in suffixes) {
            if (word.endsWith(suffix) && word.length > suffix.length + 1) {
                val stem = word.dropLast(suffix.length)
                dict[stem]?.let { return extractFirst(it) }
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
        // Handle poetic/archaic elisions: consonant + "ne" → consonant + "ene"
        val elisionPatterns = listOf("ne" to "ene", "ner" to "ener", "nes" to "enes", "nem" to "enem")
        for ((suffix, expanded) in elisionPatterns) {
            if (word.endsWith(suffix) && word.length > suffix.length + 2) {
                val stem = word.dropLast(suffix.length)
                val expanded_word = stem + expanded
                dict[expanded_word]?.let { return extractFirst(it) }
            }
        }
        return null
    }

    private fun trySplitCompound(dict: Map<String, String>, word: String): String? {
        // Try splitting at each position (min 3 chars per part)
        for (i in 3..(word.length - 3)) {
            val left = word.substring(0, i)
            val right = word.substring(i)

            val leftIpa = dict[left]?.let { extractFirst(it) }
            val rightIpa = dict[right]?.let { extractFirst(it) }
            if (leftIpa != null && rightIpa != null) {
                return "$leftIpa$rightIpa"
            }

            // Try with Fugen-s: "Donner|s|gang" → split removes the 's'
            if (right.startsWith("s") && right.length > 3) {
                val rightAfterS = right.substring(1)
                val rightAfterSIpa = dict[rightAfterS]?.let { extractFirst(it) }
                if (leftIpa != null && rightAfterSIpa != null) {
                    return "${leftIpa}s$rightAfterSIpa"
                }
            }

            // Try with Fugen-n: "Sonne|n|schein"
            if (right.startsWith("n") && right.length > 3) {
                val rightAfterN = right.substring(1)
                val rightAfterNIpa = dict[rightAfterN]?.let { extractFirst(it) }
                if (leftIpa != null && rightAfterNIpa != null) {
                    return "${leftIpa}n$rightAfterNIpa"
                }
            }

            // Try with Fugen-en: "Straße|n|bahn"
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
        // Handle multiple pronunciations: "/pron1/, /pron2/" or "pron1, pron2"
        val first = ipa.split(",").first().trim()
        return first.removePrefix("/").removeSuffix("/").trim()
    }

    private suspend fun loadDictionary(lang: String): Map<String, String>? {
        mutex.withLock {
            dictionaries[lang]?.let { return it }
        }

        val fileName = languageFiles[lang] ?: return null

        val dict = withContext(Dispatchers.IO) {
            try {
                val map = HashMap<String, String>(130_000)
                context.assets.open(fileName).bufferedReader().useLines { lines ->
                    for (line in lines) {
                        val tab = line.indexOf('\t')
                        if (tab > 0 && tab < line.length - 1) {
                            val word = line.substring(0, tab).lowercase()
                            val ipa = line.substring(tab + 1).trim()
                            map[word] = ipa
                        }
                    }
                }
                map as Map<String, String>
            } catch (_: Exception) {
                null
            }
        } ?: return null

        mutex.withLock {
            dictionaries[lang] = dict
        }
        return dict
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
