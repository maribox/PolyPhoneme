package it.bosler.polyphoneme.data.ipa

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidAudioPlayer(context: Context) : AudioPlayer {

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
        }
    }

    override fun speak(text: String, language: String) {
        if (!ready) return
        val locale = when (language.lowercase().split("-", "_").first()) {
            "de" -> Locale.GERMAN
            "fr" -> Locale.FRENCH
            "es" -> Locale("es")
            "it" -> Locale.ITALIAN
            "pt" -> Locale("pt")
            "nl" -> Locale("nl")
            "ja" -> Locale.JAPANESE
            "zh" -> Locale.CHINESE
            "ko" -> Locale.KOREAN
            else -> Locale.ENGLISH
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
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
