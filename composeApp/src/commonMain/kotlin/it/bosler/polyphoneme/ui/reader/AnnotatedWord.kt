package it.bosler.polyphoneme.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.bosler.polyphoneme.model.IpaPosition
import it.bosler.polyphoneme.model.Token
import it.bosler.polyphoneme.ui.theme.LocalExtendedColors

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

    Column(
        modifier = modifier.padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (ipaPosition == IpaPosition.ABOVE && token.ipa != null) {
            Text(
                text = token.ipa,
                style = TextStyle(
                    fontSize = ipaSize,
                    color = extColors.ipa,
                    textAlign = TextAlign.Center,
                    lineHeight = ipaSize * 1.2f,
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
                text = token.ipa,
                style = TextStyle(
                    fontSize = ipaSize,
                    color = extColors.ipa,
                    textAlign = TextAlign.Center,
                    lineHeight = ipaSize * 1.2f,
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
}
