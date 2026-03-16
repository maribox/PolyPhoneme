package it.bosler.polyphoneme

import androidx.compose.runtime.Composable
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
    PolyPhonemeTheme {
        PolyPhonemeNavHost(
            filePicker = filePicker,
            pendingEpubUri = pendingEpubUri,
            buildInfo = buildInfo,
        )
    }
}
