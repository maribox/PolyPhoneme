package it.bosler.polyphoneme.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import it.bosler.polyphoneme.R
import it.bosler.polyphoneme.model.ReaderFont

@Composable
actual fun rememberIpaFontFamily(): FontFamily {
    return remember {
        FontFamily(Font(R.font.noto_sans_regular))
    }
}

@Composable
actual fun rememberReaderFontFamily(font: ReaderFont): FontFamily {
    return remember(font) {
        when (font) {
            ReaderFont.LORA         -> FontFamily(Font(R.font.lora_regular), Font(R.font.lora_italic, style = FontStyle.Italic))
            ReaderFont.MERRIWEATHER -> FontFamily(Font(R.font.merriweather_regular), Font(R.font.merriweather_italic, style = FontStyle.Italic))
            ReaderFont.SERIF        -> FontFamily.Serif
            ReaderFont.MONO         -> FontFamily.Monospace
            ReaderFont.CURSIVE      -> FontFamily.Cursive
            ReaderFont.DEFAULT      -> FontFamily(Font(R.font.noto_sans_regular))
        }
    }
}
