package it.bosler.polyphoneme.ui.ipa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.bosler.polyphoneme.di.AppDependencies
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One word's IPA in a specific language. */
data class IpaWord(
    val word: String,
    val ipa: String?,
)

/** All IPA results for one language. */
data class LanguageResult(
    val code: String,
    val name: String,
    val hitRate: Float,
    val ipaWords: List<IpaWord>,
    val fullIpa: String,
)

private val LANGUAGE_NAMES = mapOf(
    "en" to "English", "de" to "German", "fr" to "French",
    "es" to "Spanish", "it" to "Italian", "pt" to "Portuguese",
    "nl" to "Dutch", "ru" to "Russian", "ja" to "Japanese",
    "zh" to "Chinese",
)

class LiveIpaViewModel : ViewModel() {

    private val ipaService get() = AppDependencies.ipaService

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    /** All language results, sorted by hit rate descending. Only languages with >0 hits. */
    private val _results = MutableStateFlow<List<LanguageResult>>(emptyList())
    val results: StateFlow<List<LanguageResult>> = _results

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing

    private var transcribeJob: Job? = null

    fun updateText(text: String) {
        _inputText.value = text
        transcribeJob?.cancel()
        transcribeJob = viewModelScope.launch {
            delay(300)
            processText(text)
        }
    }

    private suspend fun processText(text: String) {
        if (text.isBlank()) {
            _results.value = emptyList()
            return
        }

        val words = tokenize(text)
        val uniqueWords = words.map { it.lowercase() }.distinct()

        // Step 1: Bloom filter — instant check which languages have any matches
        val hitRates = ipaService.detectLanguageHitRates(uniqueWords)
        val langsWithHits = hitRates.filter { it.value > 0f }

        if (langsWithHits.isEmpty()) {
            _results.value = emptyList()
            return
        }

        _isTranscribing.value = true
        // Clear previous results
        _results.value = emptyList()
        try {
            // Step 2: Transcribe one language at a time, best bloom score first
            val sorted = langsWithHits.entries.sortedByDescending { it.value }
            for ((lang, _) in sorted) {
                if (!ipaService.isLanguageSupported(lang)) continue
                val ipaMap = ipaService.transcribe(uniqueWords, lang)
                if (ipaMap.isEmpty()) continue

                val ipaWords = words.map { w -> IpaWord(w, ipaMap[w.lowercase()]) }
                val actualHits = ipaWords.count { it.ipa != null }
                if (actualHits == 0) continue

                val actualRate = actualHits.toFloat() / words.size
                val fullIpa = ipaWords.mapNotNull { it.ipa }.joinToString(" ")

                val result = LanguageResult(
                    code = lang,
                    name = LANGUAGE_NAMES[lang] ?: lang,
                    hitRate = actualRate,
                    ipaWords = ipaWords,
                    fullIpa = fullIpa,
                )
                // Append and re-sort by actual hit rate
                _results.value = (_results.value + result).sortedByDescending { it.hitRate }
            }
        } finally {
            _isTranscribing.value = false
        }
    }

    private fun tokenize(text: String): List<String> {
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    fun speak(text: String, language: String) {
        AppDependencies.audioPlayer.speak(text, language)
    }
}
