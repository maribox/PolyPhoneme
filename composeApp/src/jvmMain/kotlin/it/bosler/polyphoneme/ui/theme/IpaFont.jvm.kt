package it.bosler.polyphoneme.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import polyphoneme.composeapp.generated.resources.Res
import polyphoneme.composeapp.generated.resources.noto_sans_regular

@Composable
actual fun rememberIpaFontFamily(): FontFamily {
    val font = Font(Res.font.noto_sans_regular)
    return remember(font) { FontFamily(font) }
}
