package it.bosler.polyphoneme.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.bosler.polyphoneme.di.AppDependencies
import it.bosler.polyphoneme.model.AppSettings
import it.bosler.polyphoneme.model.IpaPosition
import it.bosler.polyphoneme.model.ReadingMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val repo get() = AppDependencies.settingsRepository

    val settings: StateFlow<AppSettings> = repo.settingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateNativeLanguage(lang: String) {
        viewModelScope.launch { repo.updateNativeLanguage(lang) }
    }

    fun updateIpaPosition(position: IpaPosition) {
        viewModelScope.launch { repo.updateIpaPosition(position) }
    }

    fun updateTranslationFrequency(value: Float) {
        viewModelScope.launch { repo.updateTranslationFrequency(value) }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch { repo.updateFontSize(size) }
    }

    fun updateLineSpacing(spacing: Float) {
        viewModelScope.launch { repo.updateLineSpacing(spacing) }
    }

    fun updateReadingMode(mode: ReadingMode) {
        viewModelScope.launch { repo.updateReadingMode(mode) }
    }

    fun updateLanguageRegion(lang: String, region: String) {
        viewModelScope.launch { repo.updateLanguageRegion(lang, region) }
    }
}
