package it.bosler.polyphoneme.data.ipa

import it.bosler.polyphoneme.model.Paragraph

data class IpaResult(
    val ipa: String,
    val isDisambiguated: Boolean = false,
)

interface IpaService {
    suspend fun transcribe(words: List<String>, language: String): Map<String, String>
    fun isLanguageSupported(language: String): Boolean
    fun setRegionOverrides(regions: Map<String, String>) {}

    /**
     * Check what fraction of [words] exist in each supported language dictionary.
     * Returns map of languageCode to hit rate (0.0-1.0), sorted descending.
     * Uses bloom filters for instant checks without loading full dictionaries.
     */
    suspend fun detectLanguageHitRates(words: List<String>): Map<String, Float> = emptyMap()

    /** Pre-load a language dictionary in the background so later transcribe() calls are instant. */
    suspend fun preloadLanguage(language: String) {}

    /** For each word, return which languages contain it (bloom filter check). */
    suspend fun detectPerWordLanguages(words: List<String>): Map<String, List<String>> = emptyMap()

    /**
     * Transcribe tokens in context, enabling homograph disambiguation.
     * Returns paragraphs with IPA and disambiguation flags applied.
     */
    suspend fun transcribeInContext(
        paragraphs: List<Paragraph>,
        language: String,
    ): List<Paragraph> {
        // Default implementation falls back to context-free transcription
        val allWords = paragraphs.flatMap { p -> p.tokens.map { it.word } }.distinct()
        val ipaMap = transcribe(allWords, language)
        return paragraphs.map { paragraph ->
            paragraph.copy(
                tokens = paragraph.tokens.map { token ->
                    val ipa = ipaMap[token.word.lowercase()]
                    if (ipa != null) token.copy(ipa = ipa) else token
                }
            )
        }
    }
}
