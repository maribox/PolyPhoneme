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
import it.bosler.polyphoneme.model.IpaPosition
import it.bosler.polyphoneme.model.ReadingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AndroidSettingsRepository(private val context: Context) : SettingsRepository {

    private object Keys {
        val NATIVE_LANGUAGE = stringPreferencesKey("native_language")
        val IPA_POSITION = stringPreferencesKey("ipa_position")
        val TRANSLATION_FREQUENCY = floatPreferencesKey("translation_frequency")
        val FONT_SIZE = intPreferencesKey("font_size")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val HAS_SEEN_PAGE_TUTORIAL = booleanPreferencesKey("has_seen_page_tutorial")
        val LANGUAGE_REGIONS = stringPreferencesKey("language_regions")
    }

    override fun settingsFlow(): Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            nativeLanguage = prefs[Keys.NATIVE_LANGUAGE] ?: "en",
            ipaPosition = prefs[Keys.IPA_POSITION]?.let {
                try { IpaPosition.valueOf(it) } catch (_: Exception) { IpaPosition.BELOW }
            } ?: IpaPosition.BELOW,
            translationFrequency = prefs[Keys.TRANSLATION_FREQUENCY] ?: 0.5f,
            fontSize = prefs[Keys.FONT_SIZE] ?: 16,
            lineSpacing = prefs[Keys.LINE_SPACING] ?: 1.5f,
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
        )
    }

    override suspend fun updateNativeLanguage(lang: String) {
        context.dataStore.edit { it[Keys.NATIVE_LANGUAGE] = lang }
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

    override suspend fun updateReadingMode(mode: ReadingMode) {
        context.dataStore.edit { it[Keys.READING_MODE] = mode.name }
    }

    override suspend fun updateHasSeenPageModeTutorial(seen: Boolean) {
        context.dataStore.edit { it[Keys.HAS_SEEN_PAGE_TUTORIAL] = seen }
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
