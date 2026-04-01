package it.bosler.polyphoneme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import it.bosler.polyphoneme.di.AppDependencies
import it.bosler.polyphoneme.model.AppSettings
import it.bosler.polyphoneme.model.DarkModePreference
import it.bosler.polyphoneme.ui.about.BuildInfo
import it.bosler.polyphoneme.ui.navigation.PolyPhonemeNavHost
import it.bosler.polyphoneme.ui.theme.PolyPhonemeTheme
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun App(
    filePicker: @Composable ((String) -> Unit) -> (() -> Unit) = { { } },
    pendingEpubUri: MutableStateFlow<String?>? = null,
    buildInfo: BuildInfo = BuildInfo(),
) {
    val settings by AppDependencies.settingsRepository.settingsFlow()
        .collectAsState(initial = AppSettings())

    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.darkModePreference) {
        DarkModePreference.SYSTEM -> systemDark
        DarkModePreference.LIGHT  -> false
        DarkModePreference.DARK   -> true
    }

    PolyPhonemeTheme(
        appTheme = settings.appTheme,
        readerBackground = settings.readerBackground,
        readerFont = settings.readerFont,
        darkTheme = darkTheme,
    ) {
        PolyPhonemeNavHost(
            filePicker = filePicker,
            pendingEpubUri = pendingEpubUri,
            buildInfo = buildInfo,
        )
    }
}
