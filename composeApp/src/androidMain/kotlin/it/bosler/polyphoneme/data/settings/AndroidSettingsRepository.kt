package it.bosler.polyphoneme.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.bosler.polyphoneme.model.AppSettings
import it.bosler.polyphoneme.model.AppTheme
import it.bosler.polyphoneme.model.DarkModePreference
import it.bosler.polyphoneme.model.IpaPosition
import it.bosler.polyphoneme.model.ReaderBackground
import it.bosler.polyphoneme.model.ReaderFont
import it.bosler.polyphoneme.model.ReadingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AndroidSettingsRepository(private val context: Context) : SettingsRepository {

    private object Keys {
        val NATIVE_LANGUAGE = stringPreferencesKey("native_language")
        val IPA_ENABLED = booleanPreferencesKey("ipa_enabled")
        val IPA_POSITION = stringPreferencesKey("ipa_position")
        val TRANSLATION_FREQUENCY = floatPreferencesKey("translation_frequency")
        val FONT_SIZE = intPreferencesKey("font_size")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val LETTER_SPACING = floatPreferencesKey("letter_spacing")
        val WORD_SPACING = floatPreferencesKey("word_spacing")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val HAS_SEEN_PAGE_TUTORIAL = booleanPreferencesKey("has_seen_page_tutorial")
        val LANGUAGE_REGIONS = stringPreferencesKey("language_regions")
        val APP_THEME = stringPreferencesKey("app_theme")
        val READER_BACKGROUND = stringPreferencesKey("reader_background")
        val READER_FONT = stringPreferencesKey("reader_font")
        val DARK_MODE = stringPreferencesKey("dark_mode")
    }

    override fun settingsFlow(): Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            nativeLanguage = prefs[Keys.NATIVE_LANGUAGE] ?: "en",
            ipaEnabled = prefs[Keys.IPA_ENABLED] ?: true,
            ipaPosition = prefs[Keys.IPA_POSITION]?.let {
                try { IpaPosition.valueOf(it) } catch (_: Exception) { IpaPosition.BELOW }
            } ?: IpaPosition.BELOW,
            translationFrequency = prefs[Keys.TRANSLATION_FREQUENCY] ?: 0.5f,
            fontSize = prefs[Keys.FONT_SIZE] ?: 16,
            lineSpacing = prefs[Keys.LINE_SPACING] ?: 1.5f,
            letterSpacing = prefs[Keys.LETTER_SPACING] ?: 0f,
            wordSpacing = prefs[Keys.WORD_SPACING] ?: 3f,
            readingMode = prefs[Keys.READING_MODE]?.let {
                try { ReadingMode.valueOf(it) } catch (_: Exception) { ReadingMode.PAGE }
            } ?: ReadingMode.PAGE,
            hasSeenPageModeTutorial = prefs[Keys.HAS_SEEN_PAGE_TUTORIAL] ?: false,
            languageRegions = prefs[Keys.LANGUAGE_REGIONS]?.let { raw ->
                raw.split(";").filter { it.contains("=") }.associate {
                    val (k, v) = it.split("=", limit = 2)
                    k to v
                }
            } ?: emptyMap(),
            appTheme = prefs[Keys.APP_THEME]?.let {
                try { AppTheme.valueOf(it) } catch (_: Exception) { AppTheme.INDIGO }
            } ?: AppTheme.INDIGO,
            readerBackground = prefs[Keys.READER_BACKGROUND]?.let {
                try { ReaderBackground.valueOf(it) } catch (_: Exception) { ReaderBackground.DEFAULT }
            } ?: ReaderBackground.DEFAULT,
            readerFont = prefs[Keys.READER_FONT]?.let {
                try { ReaderFont.valueOf(it) } catch (_: Exception) { ReaderFont.DEFAULT }
            } ?: ReaderFont.DEFAULT,
            darkModePreference = prefs[Keys.DARK_MODE]?.let {
                try { DarkModePreference.valueOf(it) } catch (_: Exception) { DarkModePreference.SYSTEM }
            } ?: DarkModePreference.SYSTEM,
        )
    }

    override suspend fun updateNativeLanguage(lang: String) {
        context.dataStore.edit { it[Keys.NATIVE_LANGUAGE] = lang }
    }

    override suspend fun updateIpaEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IPA_ENABLED] = enabled }
    }

    override suspend fun updateIpaPosition(position: IpaPosition) {
        context.dataStore.edit { it[Keys.IPA_POSITION] = position.name }
    }

    override suspend fun updateTranslationFrequency(value: Float) {
        context.dataStore.edit { it[Keys.TRANSLATION_FREQUENCY] = value }
    }

    override suspend fun updateFontSize(size: Int) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = size.coerceIn(12, 28) }
    }

    override suspend fun updateLineSpacing(spacing: Float) {
        context.dataStore.edit { it[Keys.LINE_SPACING] = spacing.coerceIn(1.0f, 3.0f) }
    }

    override suspend fun updateLetterSpacing(spacing: Float) {
        context.dataStore.edit { it[Keys.LETTER_SPACING] = spacing.coerceIn(0f, 4f) }
    }

    override suspend fun updateWordSpacing(spacing: Float) {
        context.dataStore.edit { it[Keys.WORD_SPACING] = spacing.coerceIn(0f, 12f) }
    }

    override suspend fun updateReadingMode(mode: ReadingMode) {
        context.dataStore.edit { it[Keys.READING_MODE] = mode.name }
    }

    override suspend fun updateHasSeenPageModeTutorial(seen: Boolean) {
        context.dataStore.edit { it[Keys.HAS_SEEN_PAGE_TUTORIAL] = seen }
    }

    override suspend fun updateAppTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.APP_THEME] = theme.name }
    }

    override suspend fun updateReaderBackground(bg: ReaderBackground) {
        context.dataStore.edit { it[Keys.READER_BACKGROUND] = bg.name }
    }

    override suspend fun updateReaderFont(font: ReaderFont) {
        context.dataStore.edit { it[Keys.READER_FONT] = font.name }
    }

    override suspend fun updateDarkModePreference(pref: DarkModePreference) {
        context.dataStore.edit { it[Keys.DARK_MODE] = pref.name }
    }

    override suspend fun updateLanguageRegion(lang: String, region: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.LANGUAGE_REGIONS]?.let { raw ->
                raw.split(";").filter { it.contains("=") }.associate {
                    val (k, v) = it.split("=", limit = 2)
                    k to v
                }
            }?.toMutableMap() ?: mutableMapOf()
            current[lang] = region
            prefs[Keys.LANGUAGE_REGIONS] = current.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }
}
