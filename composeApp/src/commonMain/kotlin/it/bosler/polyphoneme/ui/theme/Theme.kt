package it.bosler.polyphoneme.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import it.bosler.polyphoneme.model.AppTheme
import it.bosler.polyphoneme.model.ReaderBackground
import it.bosler.polyphoneme.model.ReaderFont

// ── Extended colors (IPA / translation) ──────────────────────────────────────

data class ExtendedColors(
    val ipa: Color,
    val translation: Color,
    val disambiguatedIpa: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(ipa = IpaColor, translation = TranslationColor, disambiguatedIpa = DisambiguatedIpaColor)
}

// ── Reader style override ─────────────────────────────────────────────────────

data class ReaderStyle(
    val background: Color?,   // null = use MaterialTheme surface
    val textColor: Color?,    // null = use MaterialTheme onSurface
    val ipaColor: Color?,     // null = use ExtendedColors.ipa
    val fontFamily: FontFamily,
)

val LocalReaderStyle = staticCompositionLocalOf {
    ReaderStyle(background = null, textColor = null, ipaColor = null, fontFamily = FontFamily.Default)
}

// ── Color schemes ─────────────────────────────────────────────────────────────

private fun indigoLight() = lightColorScheme(
    primary = Indigo_light_primary, onPrimary = Indigo_light_onPrimary,
    primaryContainer = Indigo_light_primaryContainer, onPrimaryContainer = Indigo_light_onPrimaryContainer,
    secondary = Indigo_light_secondary, onSecondary = Indigo_light_onSecondary,
    secondaryContainer = Indigo_light_secondaryContainer, onSecondaryContainer = Indigo_light_onSecondaryContainer,
    tertiary = Indigo_light_tertiary, onTertiary = Indigo_light_onTertiary,
    tertiaryContainer = Indigo_light_tertiaryContainer, onTertiaryContainer = Indigo_light_onTertiaryContainer,
    background = Indigo_light_background, onBackground = Indigo_light_onBackground,
    surface = Indigo_light_surface, onSurface = Indigo_light_onSurface,
    surfaceVariant = Indigo_light_surfaceVariant, onSurfaceVariant = Indigo_light_onSurfaceVariant,
    outline = Indigo_light_outline, outlineVariant = Indigo_light_outlineVariant,
    inverseSurface = Indigo_light_inverseSurface, inverseOnSurface = Indigo_light_inverseOnSurface,
    inversePrimary = Indigo_light_inversePrimary,
    surfaceContainerLowest = Indigo_light_surfaceContainerLowest,
    surfaceContainerLow = Indigo_light_surfaceContainerLow, surfaceContainer = Indigo_light_surfaceContainer,
    surfaceContainerHigh = Indigo_light_surfaceContainerHigh, surfaceContainerHighest = Indigo_light_surfaceContainerHighest,
    error = Indigo_light_error, onError = Indigo_light_onError,
    errorContainer = Indigo_light_errorContainer, onErrorContainer = Indigo_light_onErrorContainer,
)

private fun indigoDark() = darkColorScheme(
    primary = Indigo_dark_primary, onPrimary = Indigo_dark_onPrimary,
    primaryContainer = Indigo_dark_primaryContainer, onPrimaryContainer = Indigo_dark_onPrimaryContainer,
    secondary = Indigo_dark_secondary, onSecondary = Indigo_dark_onSecondary,
    secondaryContainer = Indigo_dark_secondaryContainer, onSecondaryContainer = Indigo_dark_onSecondaryContainer,
    tertiary = Indigo_dark_tertiary, onTertiary = Indigo_dark_onTertiary,
    tertiaryContainer = Indigo_dark_tertiaryContainer, onTertiaryContainer = Indigo_dark_onTertiaryContainer,
    background = Indigo_dark_background, onBackground = Indigo_dark_onBackground,
    surface = Indigo_dark_surface, onSurface = Indigo_dark_onSurface,
    surfaceVariant = Indigo_dark_surfaceVariant, onSurfaceVariant = Indigo_dark_onSurfaceVariant,
    outline = Indigo_dark_outline, outlineVariant = Indigo_dark_outlineVariant,
    inverseSurface = Indigo_dark_inverseSurface, inverseOnSurface = Indigo_dark_inverseOnSurface,
    inversePrimary = Indigo_dark_inversePrimary,
    surfaceContainerLowest = Indigo_dark_surfaceContainerLowest,
    surfaceContainerLow = Indigo_dark_surfaceContainerLow, surfaceContainer = Indigo_dark_surfaceContainer,
    surfaceContainerHigh = Indigo_dark_surfaceContainerHigh, surfaceContainerHighest = Indigo_dark_surfaceContainerHighest,
    error = Indigo_dark_error, onError = Indigo_dark_onError,
    errorContainer = Indigo_dark_errorContainer, onErrorContainer = Indigo_dark_onErrorContainer,
)

private fun sageLight() = lightColorScheme(
    primary = Sage_light_primary, onPrimary = Sage_light_onPrimary,
    primaryContainer = Sage_light_primaryContainer, onPrimaryContainer = Sage_light_onPrimaryContainer,
    secondary = Sage_light_secondary, onSecondary = Sage_light_onSecondary,
    secondaryContainer = Sage_light_secondaryContainer, onSecondaryContainer = Sage_light_onSecondaryContainer,
    tertiary = Sage_light_tertiary, onTertiary = Sage_light_onTertiary,
    tertiaryContainer = Sage_light_tertiaryContainer, onTertiaryContainer = Sage_light_onTertiaryContainer,
    background = Sage_light_background, onBackground = Sage_light_onBackground,
    surface = Sage_light_surface, onSurface = Sage_light_onSurface,
    surfaceVariant = Sage_light_surfaceVariant, onSurfaceVariant = Sage_light_onSurfaceVariant,
    outline = Sage_light_outline, outlineVariant = Sage_light_outlineVariant,
    inverseSurface = Sage_light_inverseSurface, inverseOnSurface = Sage_light_inverseOnSurface,
    inversePrimary = Sage_light_inversePrimary,
    surfaceContainerLowest = Sage_light_surfaceContainerLowest,
    surfaceContainerLow = Sage_light_surfaceContainerLow, surfaceContainer = Sage_light_surfaceContainer,
    surfaceContainerHigh = Sage_light_surfaceContainerHigh, surfaceContainerHighest = Sage_light_surfaceContainerHighest,
)

private fun sageDark() = darkColorScheme(
    primary = Sage_dark_primary, onPrimary = Sage_dark_onPrimary,
    primaryContainer = Sage_dark_primaryContainer, onPrimaryContainer = Sage_dark_onPrimaryContainer,
    secondary = Sage_dark_secondary, onSecondary = Sage_dark_onSecondary,
    secondaryContainer = Sage_dark_secondaryContainer, onSecondaryContainer = Sage_dark_onSecondaryContainer,
    tertiary = Sage_dark_tertiary, onTertiary = Sage_dark_onTertiary,
    tertiaryContainer = Sage_dark_tertiaryContainer, onTertiaryContainer = Sage_dark_onTertiaryContainer,
    background = Sage_dark_background, onBackground = Sage_dark_onBackground,
    surface = Sage_dark_surface, onSurface = Sage_dark_onSurface,
    surfaceVariant = Sage_dark_surfaceVariant, onSurfaceVariant = Sage_dark_onSurfaceVariant,
    outline = Sage_dark_outline, outlineVariant = Sage_dark_outlineVariant,
    inverseSurface = Sage_dark_inverseSurface, inverseOnSurface = Sage_dark_inverseOnSurface,
    inversePrimary = Sage_dark_inversePrimary,
    surfaceContainerLowest = Sage_dark_surfaceContainerLowest,
    surfaceContainerLow = Sage_dark_surfaceContainerLow, surfaceContainer = Sage_dark_surfaceContainer,
    surfaceContainerHigh = Sage_dark_surfaceContainerHigh, surfaceContainerHighest = Sage_dark_surfaceContainerHighest,
)

private fun amberLight() = lightColorScheme(
    primary = Amber_light_primary, onPrimary = Amber_light_onPrimary,
    primaryContainer = Amber_light_primaryContainer, onPrimaryContainer = Amber_light_onPrimaryContainer,
    secondary = Amber_light_secondary, onSecondary = Amber_light_onSecondary,
    secondaryContainer = Amber_light_secondaryContainer, onSecondaryContainer = Amber_light_onSecondaryContainer,
    tertiary = Amber_light_tertiary, onTertiary = Amber_light_onTertiary,
    tertiaryContainer = Amber_light_tertiaryContainer, onTertiaryContainer = Amber_light_onTertiaryContainer,
    background = Amber_light_background, onBackground = Amber_light_onBackground,
    surface = Amber_light_surface, onSurface = Amber_light_onSurface,
    surfaceVariant = Amber_light_surfaceVariant, onSurfaceVariant = Amber_light_onSurfaceVariant,
    outline = Amber_light_outline, outlineVariant = Amber_light_outlineVariant,
    inverseSurface = Amber_light_inverseSurface, inverseOnSurface = Amber_light_inverseOnSurface,
    inversePrimary = Amber_light_inversePrimary,
    surfaceContainerLowest = Amber_light_surfaceContainerLowest,
    surfaceContainerLow = Amber_light_surfaceContainerLow, surfaceContainer = Amber_light_surfaceContainer,
    surfaceContainerHigh = Amber_light_surfaceContainerHigh, surfaceContainerHighest = Amber_light_surfaceContainerHighest,
)

private fun amberDark() = darkColorScheme(
    primary = Amber_dark_primary, onPrimary = Amber_dark_onPrimary,
    primaryContainer = Amber_dark_primaryContainer, onPrimaryContainer = Amber_dark_onPrimaryContainer,
    secondary = Amber_dark_secondary, onSecondary = Amber_dark_onSecondary,
    secondaryContainer = Amber_dark_secondaryContainer, onSecondaryContainer = Amber_dark_onSecondaryContainer,
    tertiary = Amber_dark_tertiary, onTertiary = Amber_dark_onTertiary,
    tertiaryContainer = Amber_dark_tertiaryContainer, onTertiaryContainer = Amber_dark_onTertiaryContainer,
    background = Amber_dark_background, onBackground = Amber_dark_onBackground,
    surface = Amber_dark_surface, onSurface = Amber_dark_onSurface,
    surfaceVariant = Amber_dark_surfaceVariant, onSurfaceVariant = Amber_dark_onSurfaceVariant,
    outline = Amber_dark_outline, outlineVariant = Amber_dark_outlineVariant,
    inverseSurface = Amber_dark_inverseSurface, inverseOnSurface = Amber_dark_inverseOnSurface,
    inversePrimary = Amber_dark_inversePrimary,
    surfaceContainerLowest = Amber_dark_surfaceContainerLowest,
    surfaceContainerLow = Amber_dark_surfaceContainerLow, surfaceContainer = Amber_dark_surfaceContainer,
    surfaceContainerHigh = Amber_dark_surfaceContainerHigh, surfaceContainerHighest = Amber_dark_surfaceContainerHighest,
)

private fun crimsonLight() = lightColorScheme(
    primary = Crimson_light_primary, onPrimary = Crimson_light_onPrimary,
    primaryContainer = Crimson_light_primaryContainer, onPrimaryContainer = Crimson_light_onPrimaryContainer,
    secondary = Crimson_light_secondary, onSecondary = Crimson_light_onSecondary,
    secondaryContainer = Crimson_light_secondaryContainer, onSecondaryContainer = Crimson_light_onSecondaryContainer,
    tertiary = Crimson_light_tertiary, onTertiary = Crimson_light_onTertiary,
    tertiaryContainer = Crimson_light_tertiaryContainer, onTertiaryContainer = Crimson_light_onTertiaryContainer,
    background = Crimson_light_background, onBackground = Crimson_light_onBackground,
    surface = Crimson_light_surface, onSurface = Crimson_light_onSurface,
    surfaceVariant = Crimson_light_surfaceVariant, onSurfaceVariant = Crimson_light_onSurfaceVariant,
    outline = Crimson_light_outline, outlineVariant = Crimson_light_outlineVariant,
    inverseSurface = Crimson_light_inverseSurface, inverseOnSurface = Crimson_light_inverseOnSurface,
    inversePrimary = Crimson_light_inversePrimary,
    surfaceContainerLowest = Crimson_light_surfaceContainerLowest,
    surfaceContainerLow = Crimson_light_surfaceContainerLow, surfaceContainer = Crimson_light_surfaceContainer,
    surfaceContainerHigh = Crimson_light_surfaceContainerHigh, surfaceContainerHighest = Crimson_light_surfaceContainerHighest,
)

private fun crimsonDark() = darkColorScheme(
    primary = Crimson_dark_primary, onPrimary = Crimson_dark_onPrimary,
    primaryContainer = Crimson_dark_primaryContainer, onPrimaryContainer = Crimson_dark_onPrimaryContainer,
    secondary = Crimson_dark_secondary, onSecondary = Crimson_dark_onSecondary,
    secondaryContainer = Crimson_dark_secondaryContainer, onSecondaryContainer = Crimson_dark_onSecondaryContainer,
    tertiary = Crimson_dark_tertiary, onTertiary = Crimson_dark_onTertiary,
    tertiaryContainer = Crimson_dark_tertiaryContainer, onTertiaryContainer = Crimson_dark_onTertiaryContainer,
    background = Crimson_dark_background, onBackground = Crimson_dark_onBackground,
    surface = Crimson_dark_surface, onSurface = Crimson_dark_onSurface,
    surfaceVariant = Crimson_dark_surfaceVariant, onSurfaceVariant = Crimson_dark_onSurfaceVariant,
    outline = Crimson_dark_outline, outlineVariant = Crimson_dark_outlineVariant,
    inverseSurface = Crimson_dark_inverseSurface, inverseOnSurface = Crimson_dark_inverseOnSurface,
    inversePrimary = Crimson_dark_inversePrimary,
    surfaceContainerLowest = Crimson_dark_surfaceContainerLowest,
    surfaceContainerLow = Crimson_dark_surfaceContainerLow, surfaceContainer = Crimson_dark_surfaceContainer,
    surfaceContainerHigh = Crimson_dark_surfaceContainerHigh, surfaceContainerHighest = Crimson_dark_surfaceContainerHighest,
)

private fun slateLight() = lightColorScheme(
    primary = Slate_light_primary, onPrimary = Slate_light_onPrimary,
    primaryContainer = Slate_light_primaryContainer, onPrimaryContainer = Slate_light_onPrimaryContainer,
    secondary = Slate_light_secondary, onSecondary = Slate_light_onSecondary,
    secondaryContainer = Slate_light_secondaryContainer, onSecondaryContainer = Slate_light_onSecondaryContainer,
    tertiary = Slate_light_tertiary, onTertiary = Slate_light_onTertiary,
    tertiaryContainer = Slate_light_tertiaryContainer, onTertiaryContainer = Slate_light_onTertiaryContainer,
    background = Slate_light_background, onBackground = Slate_light_onBackground,
    surface = Slate_light_surface, onSurface = Slate_light_onSurface,
    surfaceVariant = Slate_light_surfaceVariant, onSurfaceVariant = Slate_light_onSurfaceVariant,
    outline = Slate_light_outline, outlineVariant = Slate_light_outlineVariant,
    inverseSurface = Slate_light_inverseSurface, inverseOnSurface = Slate_light_inverseOnSurface,
    inversePrimary = Slate_light_inversePrimary,
    surfaceContainerLowest = Slate_light_surfaceContainerLowest,
    surfaceContainerLow = Slate_light_surfaceContainerLow, surfaceContainer = Slate_light_surfaceContainer,
    surfaceContainerHigh = Slate_light_surfaceContainerHigh, surfaceContainerHighest = Slate_light_surfaceContainerHighest,
)

private fun slateDark() = darkColorScheme(
    primary = Slate_dark_primary, onPrimary = Slate_dark_onPrimary,
    primaryContainer = Slate_dark_primaryContainer, onPrimaryContainer = Slate_dark_onPrimaryContainer,
    secondary = Slate_dark_secondary, onSecondary = Slate_dark_onSecondary,
    secondaryContainer = Slate_dark_secondaryContainer, onSecondaryContainer = Slate_dark_onSecondaryContainer,
    tertiary = Slate_dark_tertiary, onTertiary = Slate_dark_onTertiary,
    tertiaryContainer = Slate_dark_tertiaryContainer, onTertiaryContainer = Slate_dark_onTertiaryContainer,
    background = Slate_dark_background, onBackground = Slate_dark_onBackground,
    surface = Slate_dark_surface, onSurface = Slate_dark_onSurface,
    surfaceVariant = Slate_dark_surfaceVariant, onSurfaceVariant = Slate_dark_onSurfaceVariant,
    outline = Slate_dark_outline, outlineVariant = Slate_dark_outlineVariant,
    inverseSurface = Slate_dark_inverseSurface, inverseOnSurface = Slate_dark_inverseOnSurface,
    inversePrimary = Slate_dark_inversePrimary,
    surfaceContainerLowest = Slate_dark_surfaceContainerLowest,
    surfaceContainerLow = Slate_dark_surfaceContainerLow, surfaceContainer = Slate_dark_surfaceContainer,
    surfaceContainerHigh = Slate_dark_surfaceContainerHigh, surfaceContainerHighest = Slate_dark_surfaceContainerHighest,
)

private fun colorSchemeFor(theme: AppTheme, dark: Boolean): ColorScheme = when (theme) {
    AppTheme.INDIGO  -> if (dark) indigoDark()   else indigoLight()
    AppTheme.SAGE    -> if (dark) sageDark()     else sageLight()
    AppTheme.AMBER   -> if (dark) amberDark()    else amberLight()
    AppTheme.CRIMSON -> if (dark) crimsonDark()  else crimsonLight()
    AppTheme.SLATE   -> if (dark) slateDark()    else slateLight()
}

// ── Typography ────────────────────────────────────────────────────────────────

private val AppTypography = Typography(
    displayLarge  = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Normal, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Normal, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Normal, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    titleLarge  = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge   = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall   = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// ── Theme entry point ─────────────────────────────────────────────────────────

@Composable
fun PolyPhonemeTheme(
    appTheme: AppTheme = AppTheme.INDIGO,
    readerBackground: ReaderBackground = ReaderBackground.DEFAULT,
    readerFont: ReaderFont = ReaderFont.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = colorSchemeFor(appTheme, darkTheme)

    val extendedColors = if (darkTheme) {
        ExtendedColors(ipa = IpaColorDark, translation = TranslationColorDark, disambiguatedIpa = DisambiguatedIpaColorDark)
    } else {
        ExtendedColors(ipa = IpaColor, translation = TranslationColor, disambiguatedIpa = DisambiguatedIpaColor)
    }

    val ipaFont = rememberIpaFontFamily()
    val bodyFont = when (readerFont) {
        ReaderFont.DEFAULT -> ipaFont
        ReaderFont.SERIF   -> FontFamily.Serif
    }

    val readerStyle = when (readerBackground) {
        ReaderBackground.DEFAULT -> ReaderStyle(null, null, null, bodyFont)
        ReaderBackground.SEPIA   -> ReaderStyle(ReaderSepiaBg, ReaderSepiaText, ReaderSepiaIpa, FontFamily.Serif)
        ReaderBackground.DARK    -> ReaderStyle(ReaderDarkBg, ReaderDarkText, ReaderDarkIpa, bodyFont)
        ReaderBackground.AMOLED  -> ReaderStyle(ReaderAmoledBg, ReaderAmoledText, ReaderAmoledIpa, bodyFont)
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalReaderStyle provides readerStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
