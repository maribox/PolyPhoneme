package it.bosler.polyphoneme.data.ipa

/**
 * Lightweight language detector based on stop-word frequencies and script detection.
 * Works on short text (even a single sentence).
 */
object LanguageDetector {

    data class Detection(val language: String, val confidence: Float)

    /** Minimum text length for reasonable detection. */
    private const val MIN_LENGTH = 10

    fun detect(text: String): Detection? {
        if (text.length < MIN_LENGTH) return null
        val sample = text.take(3000).lowercase()

        // Script detection — fast and reliable
        val cyrillic = sample.count { it in '\u0400'..'\u04FF' }
        val hiragana = sample.count { it in '\u3040'..'\u309F' }
        val katakana = sample.count { it in '\u30A0'..'\u30FF' }
        val cjk = sample.count { it in '\u4E00'..'\u9FFF' || it in '\u3400'..'\u4DBF' }
        val total = sample.count { !it.isWhitespace() }.coerceAtLeast(1)
        if (cyrillic.toFloat() / total > 0.2f) return Detection("ru", 0.9f)
        if ((hiragana + katakana).toFloat() / total > 0.04f) return Detection("ja", 0.9f)
        if (cjk.toFloat() / total > 0.15f) return Detection("zh", 0.9f)

        // Latin — score by language-discriminative stop words + diacritics
        fun hits(vararg words: String) = words.sumOf { w ->
            Regex("(?<![a-zA-ZÀ-ÿ])${Regex.escape(w)}(?![a-zA-ZÀ-ÿ])").findAll(sample).count()
        }

        val scores = mapOf(
            "de" to (hits("der", "die", "das", "ein", "eine", "nicht", "ist", "und", "auch", "aber", "werden", "haben")
                    + sample.count { it in "äöüß" } * 3),
            "fr" to (hits("les", "des", "une", "dans", "cette", "leur", "aussi", "même", "être", "ont", "comme", "nous")
                    + sample.count { it in "éèêëàâùûôœç" } * 3),
            "es" to (hits("los", "las", "una", "con", "por", "para", "pero", "como", "son", "hay", "del", "sus")
                    + sample.count { it in "ñáéíóúü" } * 4),
            "it" to (hits("gli", "che", "per", "sono", "questo", "della", "anche", "come", "però", "quando", "lui", "loro")
                    + sample.count { it in "àèéìòù" } * 2),
            "pt" to (hits("uma", "com", "por", "para", "mas", "não", "são", "isso", "este", "ela", "pelo", "foi")
                    + sample.count { it in "ãõâêôáéíóú" } * 3),
            "nl" to hits("het", "van", "zijn", "worden", "heeft", "kunnen", "naar", "door", "deze", "werd", "wordt", "over"),
            "en" to hits("the", "and", "that", "this", "with", "was", "have", "from", "they", "would", "been", "were"),
        )

        val max = scores.maxByOrNull { it.value } ?: return null
        if (max.value <= 0) return null

        val totalScore = scores.values.sum().coerceAtLeast(1)
        val confidence = max.value.toFloat() / totalScore
        return Detection(max.key, confidence)
    }

    /**
     * Score all supported languages against [text].
     * Returns list of (languageCode, score) pairs sorted descending by score.
     * Score is the fraction of stop-word hits relative to total hits across all languages.
     */
    fun scoreAll(text: String): List<Pair<String, Float>> {
        if (text.length < MIN_LENGTH) return emptyList()
        val sample = text.take(3000).lowercase()

        fun hits(vararg words: String) = words.sumOf { w ->
            Regex("(?<![a-zA-ZÀ-ÿ])${Regex.escape(w)}(?![a-zA-ZÀ-ÿ])").findAll(sample).count()
        }

        // Script-based languages
        val cyrillic = sample.count { it in '\u0400'..'\u04FF' }
        val total = sample.count { !it.isWhitespace() }.coerceAtLeast(1)

        val scores = mutableMapOf<String, Int>()
        if (cyrillic.toFloat() / total > 0.2f) {
            scores["ru"] = 100
        } else {
            scores["de"] = hits("der", "die", "das", "ein", "eine", "nicht", "ist", "und", "auch", "aber", "werden", "haben") +
                sample.count { it in "äöüß" } * 3
            scores["fr"] = hits("les", "des", "une", "dans", "cette", "leur", "aussi", "même", "être", "ont", "comme", "nous") +
                sample.count { it in "éèêëàâùûôœç" } * 3
            scores["es"] = hits("los", "las", "una", "con", "por", "para", "pero", "como", "son", "hay", "del", "sus") +
                sample.count { it in "ñáéíóúü" } * 4
            scores["it"] = hits("gli", "che", "per", "sono", "questo", "della", "anche", "come", "però", "quando", "lui", "loro") +
                sample.count { it in "àèéìòù" } * 2
            scores["pt"] = hits("uma", "com", "por", "para", "mas", "não", "são", "isso", "este", "ela", "pelo", "foi") +
                sample.count { it in "ãõâêôáéíóú" } * 3
            scores["nl"] = hits("het", "van", "zijn", "worden", "heeft", "kunnen", "naar", "door", "deze", "werd", "wordt", "over")
            scores["en"] = hits("the", "and", "that", "this", "with", "was", "have", "from", "they", "would", "been", "were")
        }

        val totalScore = scores.values.sum().coerceAtLeast(1)
        return scores.entries
            .map { (code, s) -> code to s.toFloat() / totalScore }
            .sortedByDescending { it.second }
    }

    /** All languages that can be detected and have IPA dictionaries. */
    val supportedLanguages = listOf(
        "en" to "English",
        "de" to "German",
        "fr" to "French",
        "es" to "Spanish",
        "it" to "Italian",
        "pt" to "Portuguese",
        "nl" to "Dutch",
        "ru" to "Russian",
    )
}
