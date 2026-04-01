package it.bosler.polyphoneme.data.settings

import it.bosler.polyphoneme.model.AppSettings
import it.bosler.polyphoneme.model.AppTheme
import it.bosler.polyphoneme.model.DarkModePreference
import it.bosler.polyphoneme.model.IpaPosition
import it.bosler.polyphoneme.model.ReaderBackground
import it.bosler.polyphoneme.model.ReaderFont
import it.bosler.polyphoneme.model.ReadingMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun settingsFlow(): Flow<AppSettings>
    suspend fun updateNativeLanguage(lang: String)
    suspend fun updateIpaEnabled(enabled: Boolean)
    suspend fun updateIpaPosition(position: IpaPosition)
    suspend fun updateTranslationFrequency(value: Float)
    suspend fun updateFontSize(size: Int)
    suspend fun updateLineSpacing(spacing: Float)
    suspend fun updateLetterSpacing(spacing: Float)
    suspend fun updateWordSpacing(spacing: Float)
    suspend fun updateReadingMode(mode: ReadingMode)
    suspend fun updateHasSeenPageModeTutorial(seen: Boolean)
    suspend fun updateLanguageRegion(lang: String, region: String)
    suspend fun updateAppTheme(theme: AppTheme)
    suspend fun updateReaderBackground(bg: ReaderBackground)
    suspend fun updateReaderFont(font: ReaderFont)
    suspend fun updateDarkModePreference(pref: DarkModePreference)
}
