package it.bosler.polyphoneme.data.translation

import it.bosler.polyphoneme.model.Token

interface TranslationService {
    /**
     * Translate a phrase (range of tokens) with full sentence context.
     * Returns a human-readable translation string.
     */
    suspend fun translatePhrase(
        tokens: List<Token>,
        allTokens: List<Token>,
        startIndex: Int,
        bookLanguage: String,
        nativeLanguage: String,
    ): String?
}
