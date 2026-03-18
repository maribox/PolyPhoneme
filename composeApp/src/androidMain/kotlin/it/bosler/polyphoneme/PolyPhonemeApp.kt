package it.bosler.polyphoneme

import android.app.Application
import it.bosler.polyphoneme.data.ipa.AndroidAudioPlayer
import it.bosler.polyphoneme.data.ipa.DictionaryIpaService
import it.bosler.polyphoneme.data.library.AndroidBookRepository
import it.bosler.polyphoneme.data.settings.AndroidSettingsRepository
import it.bosler.polyphoneme.di.AppDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PolyPhonemeApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val ipaService = DictionaryIpaService(this)
        val settingsRepo = AndroidSettingsRepository(this)
        AppDependencies.ipaService = ipaService
        AppDependencies.bookRepository = AndroidBookRepository(this, ipaService)
        AppDependencies.settingsRepository = settingsRepo
        val audioPlayer = AndroidAudioPlayer(this)
        AppDependencies.audioPlayer = audioPlayer

        // Keep IPA service and TTS region overrides in sync with settings
        appScope.launch {
            settingsRepo.settingsFlow()
                .map { it.languageRegions }
                .distinctUntilChanged()
                .collectLatest { regions ->
                    ipaService.setRegionOverrides(regions)
                    audioPlayer.setRegionOverrides(regions)
                }
        }
    }
}
