package it.bosler.polyphoneme.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.bosler.polyphoneme.model.IpaPosition
import it.bosler.polyphoneme.model.Token
import it.bosler.polyphoneme.ui.theme.LocalExtendedColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AnnotatedWord(
    token: Token,
    ipaPosition: IpaPosition,
    fontSize: Int,
    lineSpacing: Float,
    modifier: Modifier = Modifier,
) {
    val displayText = token.leadingPunctuation + token.word + token.trailingPunctuation
    val extColors = LocalExtendedColors.current
    val ipaSize = (fontSize * 0.6f).sp
    val ipaColor = if (token.isDisambiguated) extColors.disambiguatedIpa else extColors.ipa

    val hasAlternatives = token.isDisambiguated && token.alternativePronunciations.isNotEmpty()

    if (hasAlternatives) {
        val tooltipState = rememberTooltipState()
        val scope = rememberCoroutineScope()

        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    val altsText = token.alternativePronunciations.joinToString(", ")
                    Text("Also: /$altsText/")
                }
            },
            state = tooltipState,
        ) {
            Column(
                modifier = modifier
                    .padding(horizontal = 1.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { scope.launch { tooltipState.show() } },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IpaContent(token, ipaPosition, ipaSize, ipaColor, fontSize, lineSpacing, displayText, extColors)
            }
        }
    } else {
        Column(
            modifier = modifier.padding(horizontal = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IpaContent(token, ipaPosition, ipaSize, ipaColor, fontSize, lineSpacing, displayText, extColors)
        }
    }
}

@Composable
private fun IpaContent(
    token: Token,
    ipaPosition: IpaPosition,
    ipaSize: androidx.compose.ui.unit.TextUnit,
    ipaColor: androidx.compose.ui.graphics.Color,
    fontSize: Int,
    lineSpacing: Float,
    displayText: String,
    extColors: it.bosler.polyphoneme.ui.theme.ExtendedColors,
) {
    if (ipaPosition == IpaPosition.ABOVE && token.ipa != null) {
        Text(
            text = if (token.isDisambiguated && token.alternativePronunciations.isNotEmpty()) {
                "${token.ipa}*"
            } else {
                token.ipa
            },
            style = TextStyle(
                fontSize = ipaSize,
                color = ipaColor,
                textAlign = TextAlign.Center,
                lineHeight = ipaSize * 1.2f,
                fontStyle = if (token.isDisambiguated && token.alternativePronunciations.isNotEmpty()) {
                    FontStyle.Italic
                } else FontStyle.Normal,
            ),
        )
    }

    Text(
        text = displayText,
        style = TextStyle(
            fontSize = fontSize.sp,
            lineHeight = (fontSize * lineSpacing).sp,
        ),
    )

    if (ipaPosition == IpaPosition.BELOW && token.ipa != null) {
        Text(
            text = if (token.isDisambiguated && token.alternativePronunciations.isNotEmpty()) {
                "${token.ipa}*"
            } else {
                token.ipa
            },
            style = TextStyle(
                fontSize = ipaSize,
                color = ipaColor,
                textAlign = TextAlign.Center,
                lineHeight = ipaSize * 1.2f,
                fontStyle = if (token.isDisambiguated && token.alternativePronunciations.isNotEmpty()) {
                    FontStyle.Italic
                } else FontStyle.Normal,
            ),
        )
    }

    if (token.translation != null) {
        Text(
            text = token.translation,
            style = TextStyle(
                fontSize = (fontSize * 0.5f).sp,
                color = extColors.translation,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
