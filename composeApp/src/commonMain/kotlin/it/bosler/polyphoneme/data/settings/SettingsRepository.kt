package it.bosler.polyphoneme.data.settings

import it.bosler.polyphoneme.model.AppSettings
import it.bosler.polyphoneme.model.IpaPosition
import it.bosler.polyphoneme.model.ReadingMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun settingsFlow(): Flow<AppSettings>
    suspend fun updateNativeLanguage(lang: String)
    suspend fun updateIpaPosition(position: IpaPosition)
    suspend fun updateTranslationFrequency(value: Float)
    suspend fun updateFontSize(size: Int)
    suspend fun updateLineSpacing(spacing: Float)
    suspend fun updateReadingMode(mode: ReadingMode)
    suspend fun updateHasSeenPageModeTutorial(seen: Boolean)
}
