package it.bosler.polyphoneme.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.bosler.polyphoneme.model.IpaPosition
import it.bosler.polyphoneme.model.Paragraph
import it.bosler.polyphoneme.model.Token
import it.bosler.polyphoneme.ui.theme.LocalExtendedColors
import it.bosler.polyphoneme.ui.theme.LocalReaderStyle
import it.bosler.polyphoneme.ui.theme.rememberIpaFontFamily

@Composable
fun ParagraphRow(
    paragraph: Paragraph,
    ipaPosition: IpaPosition,
    ipaEnabled: Boolean = true,
    fontSize: Int,
    lineSpacing: Float,
    letterSpacing: Float = 0f,
    wordSpacing: Float = 0f,
    translationFrequency: Float = 0f,
    paragraphIndex: Int = -1,
    selectedPhraseRange: IntRange? = null,
    onWordTap: (Token) -> Unit = {},
    onWordTapIndexed: ((Token, Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    StackedParagraph(
        paragraph = paragraph,
        ipaPosition = ipaPosition,
        ipaEnabled = ipaEnabled,
        fontSize = fontSize,
        lineSpacing = lineSpacing,
        letterSpacing = letterSpacing,
        wordSpacing = wordSpacing,
        translationFrequency = translationFrequency,
        paragraphIndex = paragraphIndex,
        selectedPhraseRange = selectedPhraseRange,
        onWordTap = onWordTap,
        onWordTapIndexed = onWordTapIndexed,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StackedParagraph(
    paragraph: Paragraph,
    ipaPosition: IpaPosition,
    ipaEnabled: Boolean,
    fontSize: Int,
    lineSpacing: Float,
    letterSpacing: Float,
    wordSpacing: Float,
    translationFrequency: Float,
    paragraphIndex: Int,
    selectedPhraseRange: IntRange?,
    onWordTap: (Token) -> Unit,
    onWordTapIndexed: ((Token, Int, Int) -> Unit)?,
    modifier: Modifier,
) {
    val readerStyle = LocalReaderStyle.current
    val textColor = readerStyle.textColor ?: MaterialTheme.colorScheme.onSurface
    val ipaColor = readerStyle.ipaColor ?: LocalExtendedColors.current.ipa
    val translationColor = LocalExtendedColors.current.translation
    val ipaFontSize = (fontSize * 0.6f).sp
    val translationFontSize = (fontSize * 0.55f).sp
    val ipaLineHeight = with(LocalDensity.current) { (ipaFontSize * 1.1f).toDp() }
    val translationLineHeight = with(LocalDensity.current) { (translationFontSize * 1.1f).toDp() }
    val ipaFont = rememberIpaFontFamily()
    val bodyFont = readerStyle.fontFamily

    val hasAnyIpa = ipaEnabled && paragraph.tokens.any { it.ipa != null }
    val hasAnyTranslation = translationFrequency > 0.01f && paragraph.tokens.any { it.translation != null }
    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 10.dp),
    ) {
        for ((tokenIndex, token) in paragraph.tokens.withIndex()) {
            // Show translation based on word rarity: slider low = only rare words, high = everything
            // commonness 1.0 = very common (e.g. "the"), 0.0 = very rare
            // At translationFrequency 0.3, show words with commonness < 0.3 (rare words only)
            val showTranslation = token.translation != null &&
                translationFrequency > 0.01f &&
                (translationFrequency >= 0.99f || token.commonness < translationFrequency)
            val isHighlighted = selectedPhraseRange != null && tokenIndex in selectedPhraseRange
            WordBlock(
                token = token,
                ipaPosition = ipaPosition,
                ipaEnabled = ipaEnabled,
                fontSize = fontSize,
                lineSpacing = lineSpacing,
                letterSpacing = letterSpacing,
                wordSpacing = wordSpacing,
                ipaFontSize = ipaFontSize,
                ipaLineHeight = ipaLineHeight,
                ipaColor = ipaColor,
                ipaFont = ipaFont,
                bodyFont = bodyFont,
                textColor = textColor,
                hasAnyIpa = hasAnyIpa,
                showTranslation = showTranslation,
                hasAnyTranslation = hasAnyTranslation,
                translationFontSize = translationFontSize,
                translationLineHeight = translationLineHeight,
                translationColor = translationColor,
                isHighlighted = isHighlighted,
                highlightColor = highlightColor,
                onClick = {
                    if (onWordTapIndexed != null) {
                        onWordTapIndexed(token, paragraphIndex, tokenIndex)
                    } else {
                        onWordTap(token)
                    }
                },
            )
        }
    }
}

@Composable
private fun WordBlock(
    token: Token,
    ipaPosition: IpaPosition,
    ipaEnabled: Boolean,
    fontSize: Int,
    lineSpacing: Float,
    letterSpacing: Float,
    wordSpacing: Float,
    ipaFontSize: androidx.compose.ui.unit.TextUnit,
    ipaLineHeight: androidx.compose.ui.unit.Dp,
    ipaColor: Color,
    ipaFont: FontFamily,
    bodyFont: FontFamily,
    textColor: Color,
    hasAnyIpa: Boolean,
    showTranslation: Boolean,
    hasAnyTranslation: Boolean,
    translationFontSize: androidx.compose.ui.unit.TextUnit,
    translationLineHeight: androidx.compose.ui.unit.Dp,
    translationColor: Color,
    isHighlighted: Boolean,
    highlightColor: Color,
    onClick: () -> Unit,
) {
    val display = token.leadingPunctuation + token.word + token.trailingPunctuation
    val ipa = if (ipaEnabled) token.ipa else null
    val interactionSource = remember { MutableInteractionSource() }

    val lineGapHalf = ((lineSpacing - 1f) * fontSize / 2f).dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = (2f + wordSpacing).dp, vertical = lineGapHalf)
            .then(
                if (isHighlighted) Modifier.background(highlightColor, RoundedCornerShape(4.dp))
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        if (ipaPosition == IpaPosition.ABOVE && hasAnyIpa) {
            BasicText(
                text = ipa ?: "",
                style = TextStyle(
                    fontSize = ipaFontSize,
                    color = ipaColor,
                    textAlign = TextAlign.Center,
                    fontFamily = ipaFont,
                ),
                modifier = Modifier.defaultMinSize(minHeight = ipaLineHeight),
            )
        }

        Box {
            if (ipaPosition == IpaPosition.REPLACE) {
                BasicText(
                    text = if (ipa != null) ipa else display,
                    style = TextStyle(
                        fontSize = ipaFontSize,
                        color = if (ipa != null) ipaColor else textColor.copy(alpha = 0.45f),
                        fontFamily = ipaFont,
                        textAlign = TextAlign.Center,
                    ),
                )
            } else if (ipaPosition == IpaPosition.BEFORE && ipa != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = "/${ipa}/ ",
                        style = TextStyle(fontSize = ipaFontSize, color = ipaColor, fontFamily = ipaFont),
                    )
                    BasicText(
                        text = display,
                        style = TextStyle(fontSize = fontSize.sp, lineHeight = (fontSize * lineSpacing).sp, color = textColor, fontFamily = bodyFont, letterSpacing = letterSpacing.sp),
                    )
                }
            } else if (ipaPosition == IpaPosition.BEHIND && ipa != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = display,
                        style = TextStyle(fontSize = fontSize.sp, lineHeight = (fontSize * lineSpacing).sp, color = textColor, fontFamily = bodyFont, letterSpacing = letterSpacing.sp),
                    )
                    BasicText(
                        text = " /${ipa}/",
                        style = TextStyle(fontSize = ipaFontSize, color = ipaColor, fontFamily = ipaFont),
                    )
                }
            } else {
                BasicText(
                    text = display,
                    style = TextStyle(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * lineSpacing).sp,
                        color = textColor,
                        fontFamily = bodyFont,
                        letterSpacing = letterSpacing.sp,
                    ),
                )
            }
        }

        if (ipaPosition == IpaPosition.BELOW && hasAnyIpa) {
            BasicText(
                text = ipa ?: "",
                style = TextStyle(
                    fontSize = ipaFontSize,
                    color = ipaColor,
                    textAlign = TextAlign.Center,
                    fontFamily = ipaFont,
                ),
                modifier = Modifier.defaultMinSize(minHeight = ipaLineHeight),
            )
        }

        if (hasAnyTranslation) {
            BasicText(
                text = if (showTranslation) token.translation ?: "" else "",
                style = TextStyle(
                    fontSize = translationFontSize,
                    color = translationColor,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                modifier = Modifier.defaultMinSize(minHeight = translationLineHeight),
            )
        }
    }
}
