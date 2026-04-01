package it.bosler.polyphoneme.di

import it.bosler.polyphoneme.data.ipa.AudioPlayer
import it.bosler.polyphoneme.data.ipa.IpaService
import it.bosler.polyphoneme.data.library.BookRepository
import it.bosler.polyphoneme.data.settings.SettingsRepository
import it.bosler.polyphoneme.data.translation.TranslationService

object AppDependencies {
    lateinit var bookRepository: BookRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var ipaService: IpaService
    lateinit var audioPlayer: AudioPlayer
    var translationService: TranslationService? = null

    val isInitialized: Boolean
        get() = ::bookRepository.isInitialized && ::settingsRepository.isInitialized
}
