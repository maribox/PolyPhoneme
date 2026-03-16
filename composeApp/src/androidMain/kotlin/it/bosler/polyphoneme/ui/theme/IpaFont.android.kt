package it.bosler.polyphoneme.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import it.bosler.polyphoneme.R

@Composable
actual fun rememberIpaFontFamily(): FontFamily {
    return remember {
        FontFamily(Font(R.font.noto_sans_regular))
    }
}
