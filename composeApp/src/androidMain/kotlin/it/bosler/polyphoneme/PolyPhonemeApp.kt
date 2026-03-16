package it.bosler.polyphoneme

import android.app.Application
import it.bosler.polyphoneme.data.ipa.AndroidAudioPlayer
import it.bosler.polyphoneme.data.ipa.DictionaryIpaService
import it.bosler.polyphoneme.data.library.AndroidBookRepository
import it.bosler.polyphoneme.data.settings.AndroidSettingsRepository
import it.bosler.polyphoneme.di.AppDependencies

class PolyPhonemeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val ipaService = DictionaryIpaService(this)
        AppDependencies.ipaService = ipaService
        AppDependencies.bookRepository = AndroidBookRepository(this, ipaService)
        AppDependencies.settingsRepository = AndroidSettingsRepository(this)
        AppDependencies.audioPlayer = AndroidAudioPlayer(this)
    }
}
