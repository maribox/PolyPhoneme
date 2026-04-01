package it.bosler.polyphoneme.ui.reader

import androidx.activity.compose.BackHandler as ActivityBackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(onBack: () -> Unit) {
    ActivityBackHandler(onBack = onBack)
}
