package it.bosler.polyphoneme.di

import it.bosler.polyphoneme.data.ipa.AudioPlayer
import it.bosler.polyphoneme.data.ipa.IpaService
import it.bosler.polyphoneme.data.library.BookRepository
import it.bosler.polyphoneme.data.settings.SettingsRepository

object AppDependencies {
    lateinit var bookRepository: BookRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var ipaService: IpaService
    lateinit var audioPlayer: AudioPlayer

    val isInitialized: Boolean
        get() = ::bookRepository.isInitialized && ::settingsRepository.isInitialized
}
