package it.bosler.polyphoneme.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.bosler.polyphoneme.model.Token
import it.bosler.polyphoneme.ui.theme.LocalExtendedColors
import it.bosler.polyphoneme.ui.theme.rememberIpaFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhraseDetailSheet(
    tokens: List<Token>,
    phraseTranslation: String?,
    bookLanguage: String,
    onDismiss: () -> Unit,
    onSpeak: (text: String, language: String) -> Unit,
) {
    val extColors = LocalExtendedColors.current
    val ipaFont = rememberIpaFontFamily()
    val phrase = tokens.joinToString(" ") { it.leadingPunctuation + it.word + it.trailingPunctuation }
    val ipa = tokens.mapNotNull { it.ipa }.joinToString(" ")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
        ) {
            Text(
                text = phrase,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            if (ipa.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "/$ipa/",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = ipaFont),
                    color = extColors.ipa,
                )
            }

            if (phraseTranslation != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = phraseTranslation,
                    style = MaterialTheme.typography.titleMedium,
                    color = extColors.translation,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { onSpeak(phrase, bookLanguage) },
                    label = { Text("Play") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(18.dp)) },
                )
            }

            // Individual word translations
            if (tokens.any { it.translation != null }) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Word by word",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))

                for (token in tokens) {
                    if (token.translation != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = token.word,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = token.translation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = extColors.translation,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
