package it.bosler.polyphoneme.data.ipa

import android.content.Context
import android.speech.tts.TextToSpeech
import it.bosler.polyphoneme.model.LanguageRegions
import java.util.Locale

class AndroidAudioPlayer(context: Context) : AudioPlayer {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var regionOverrides: Map<String, String> = emptyMap()

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
        }
    }

    override fun setRegionOverrides(regions: Map<String, String>) {
        this.regionOverrides = regions
    }

    override fun speak(text: String, language: String) {
        if (!ready) return
        val lang = language.lowercase().split("-", "_").first()
        val locale = buildLocale(lang)
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    private fun buildLocale(lang: String): Locale {
        // Check if there's a region override with a TTS country code
        val regionCode = regionOverrides[lang]
        if (regionCode != null) {
            val countryCode = LanguageRegions.getTtsCountryCode(lang, regionCode)
            if (countryCode.isNotEmpty()) {
                return Locale(lang, countryCode)
            }
        }

        // Fall back to language-only locale
        return when (lang) {
            "de" -> Locale.GERMAN
            "fr" -> Locale.FRENCH
            "it" -> Locale.ITALIAN
            "ja" -> Locale.JAPANESE
            "zh" -> Locale.CHINESE
            "ko" -> Locale.KOREAN
            "en" -> Locale.ENGLISH
            else -> Locale(lang)
        }
    }

    override fun stop() {
        tts?.stop()
    }

    override fun release() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
