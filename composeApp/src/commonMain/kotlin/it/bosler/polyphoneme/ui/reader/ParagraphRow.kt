package it.bosler.polyphoneme.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.bosler.polyphoneme.model.IpaPosition
import it.bosler.polyphoneme.model.Paragraph
import it.bosler.polyphoneme.model.Token
import it.bosler.polyphoneme.ui.theme.LocalExtendedColors
import it.bosler.polyphoneme.ui.theme.rememberIpaFontFamily

@Composable
fun ParagraphRow(
    paragraph: Paragraph,
    ipaPosition: IpaPosition,
    fontSize: Int,
    lineSpacing: Float,
    onWordTap: (Token) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (ipaPosition) {
        IpaPosition.BEFORE, IpaPosition.BEHIND, IpaPosition.REPLACE -> InlineParagraph(
            paragraph = paragraph,
            ipaPosition = ipaPosition,
            fontSize = fontSize,
            lineSpacing = lineSpacing,
            onWordTap = onWordTap,
            modifier = modifier,
        )
        IpaPosition.ABOVE, IpaPosition.BELOW -> StackedParagraph(
            paragraph = paragraph,
            ipaPosition = ipaPosition,
            fontSize = fontSize,
            lineSpacing = lineSpacing,
            onWordTap = onWordTap,
            modifier = modifier,
        )
    }
}

@Composable
private fun InlineParagraph(
    paragraph: Paragraph,
    ipaPosition: IpaPosition,
    fontSize: Int,
    lineSpacing: Float,
    onWordTap: (Token) -> Unit,
    modifier: Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val ipaColor = LocalExtendedColors.current.ipa
    val ipaSize = (fontSize * 0.6f).sp
    val ipaFont = rememberIpaFontFamily()

    val annotatedString: AnnotatedString = remember(paragraph, ipaPosition, fontSize, textColor, ipaColor) {
        buildAnnotatedString {
            for ((index, token) in paragraph.tokens.withIndex()) {
                when (ipaPosition) {
                    IpaPosition.REPLACE -> {
                        append(token.leadingPunctuation)
                        val wordStart = length
                        if (token.ipa != null) {
                            withStyle(SpanStyle(color = ipaColor)) {
                                append(token.ipa)
                            }
                        } else {
                            append(token.word)
                        }
                        val wordEnd = length
                        addStringAnnotation("token", index.toString(), wordStart, wordEnd)
                        append(token.trailingPunctuation)
                    }
                    IpaPosition.BEFORE -> {
                        if (token.ipa != null) {
                            withStyle(SpanStyle(fontSize = ipaSize, color = ipaColor)) {
                                append("/${token.ipa}/ ")
                            }
                        }
                        append(token.leadingPunctuation)
                        val wordStart = length
                        append(token.word)
                        val wordEnd = length
                        addStringAnnotation("token", index.toString(), wordStart, wordEnd)
                        append(token.trailingPunctuation)
                    }
                    else -> { // BEHIND
                        append(token.leadingPunctuation)
                        val wordStart = length
                        append(token.word)
                        val wordEnd = length
                        addStringAnnotation("token", index.toString(), wordStart, wordEnd)
                        append(token.trailingPunctuation)
                        if (token.ipa != null) {
                            withStyle(SpanStyle(fontSize = ipaSize, color = ipaColor)) {
                                append(" /${token.ipa}/")
                            }
                        }
                    }
                }
                append(" ")
            }
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    BasicText(
        text = annotatedString,
        style = TextStyle(
            fontSize = fontSize.sp,
            lineHeight = (fontSize * lineSpacing).sp,
            color = textColor,
            fontFamily = ipaFont,
        ),
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(paragraph) {
                detectTapGestures { offset ->
                    layoutResult?.let { result ->
                        val charOffset = result.getOffsetForPosition(offset)
                        annotatedString.getStringAnnotations("token", charOffset, charOffset)
                            .firstOrNull()?.let { annotation ->
                                val tokenIndex = annotation.item.toInt()
                                if (tokenIndex in paragraph.tokens.indices) {
                                    onWordTap(paragraph.tokens[tokenIndex])
                                }
                            }
                    }
                }
            },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StackedParagraph(
    paragraph: Paragraph,
    ipaPosition: IpaPosition,
    fontSize: Int,
    lineSpacing: Float,
    onWordTap: (Token) -> Unit,
    modifier: Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val ipaColor = LocalExtendedColors.current.ipa
    val ipaFontSize = (fontSize * 0.6f).sp
    val ipaLineHeight = with(LocalDensity.current) { (ipaFontSize * 1.3f).toDp() }
    val ipaFont = rememberIpaFontFamily()

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        for (token in paragraph.tokens) {
            WordBlock(
                token = token,
                ipaPosition = ipaPosition,
                fontSize = fontSize,
                ipaFontSize = ipaFontSize,
                ipaLineHeight = ipaLineHeight,
                ipaColor = ipaColor,
                ipaFont = ipaFont,
                textColor = textColor,
                onClick = { onWordTap(token) },
            )
        }
    }
}

@Composable
private fun WordBlock(
    token: Token,
    ipaPosition: IpaPosition,
    fontSize: Int,
    ipaFontSize: androidx.compose.ui.unit.TextUnit,
    ipaLineHeight: androidx.compose.ui.unit.Dp,
    ipaColor: Color,
    ipaFont: FontFamily,
    textColor: Color,
    onClick: () -> Unit,
) {
    val display = token.leadingPunctuation + token.word + token.trailingPunctuation
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 1.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        if (ipaPosition == IpaPosition.ABOVE) {
            BasicText(
                text = token.ipa ?: "",
                style = TextStyle(
                    fontSize = ipaFontSize,
                    color = ipaColor,
                    textAlign = TextAlign.Center,
                    fontFamily = ipaFont,
                ),
                modifier = Modifier.defaultMinSize(minHeight = ipaLineHeight),
            )
        }

        BasicText(
            text = display,
            style = TextStyle(
                fontSize = fontSize.sp,
                color = textColor,
                fontFamily = ipaFont,
            ),
        )

        if (ipaPosition == IpaPosition.BELOW) {
            BasicText(
                text = token.ipa ?: "",
                style = TextStyle(
                    fontSize = ipaFontSize,
                    color = ipaColor,
                    textAlign = TextAlign.Center,
                    fontFamily = ipaFont,
                ),
                modifier = Modifier.defaultMinSize(minHeight = ipaLineHeight),
            )
        }
    }
}
