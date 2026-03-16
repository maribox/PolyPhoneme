package it.bosler.polyphoneme.data.ipa

interface IpaService {
    suspend fun transcribe(words: List<String>, language: String): Map<String, String>
    fun isLanguageSupported(language: String): Boolean
}
