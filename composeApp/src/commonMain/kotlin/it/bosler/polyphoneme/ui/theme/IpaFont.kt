package it.bosler.polyphoneme.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import it.bosler.polyphoneme.model.ReaderFont

@Composable
expect fun rememberIpaFontFamily(): FontFamily

@Composable
expect fun rememberReaderFontFamily(font: ReaderFont): FontFamily
