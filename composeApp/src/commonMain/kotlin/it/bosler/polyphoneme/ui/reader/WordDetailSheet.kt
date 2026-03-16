package it.bosler.polyphoneme.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.bosler.polyphoneme.data.ipa.PhonemeDatabase
import it.bosler.polyphoneme.model.Token
import it.bosler.polyphoneme.ui.theme.LocalExtendedColors
import it.bosler.polyphoneme.ui.theme.rememberIpaFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordDetailSheet(
    token: Token,
    bookLanguage: String,
    nativeLanguage: String,
    onDismiss: () -> Unit,
    onSpeak: (text: String, language: String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val extColors = LocalExtendedColors.current
    val ipaFont = rememberIpaFontFamily()
    val phonemes = token.ipa?.let { PhonemeDatabase.tokenize(it) } ?: emptyList()
    val langName = languageDisplayName(bookLanguage)
    val wiktionaryLang = bookLanguage.lowercase().split("-", "_").first()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
        ) {
            // Word header
            Text(
                text = token.word,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )

            if (token.ipa != null) {
                Text(
                    text = "/${token.ipa}/",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = ipaFont),
                    color = extColors.ipa,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Action row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = { onSpeak(token.word, bookLanguage) },
                    label = { Text("Play") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(18.dp)) },
                )
                AssistChip(
                    onClick = {
                        val url = "https://$wiktionaryLang.wiktionary.org/wiki/${token.word.lowercase()}"
                        uriHandler.openUri(url)
                    },
                    label = { Text("Dictionary") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp)) },
                )
            }

            Spacer(Modifier.height(4.dp))

            // Language tag
            Text(
                text = langName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (phonemes.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Phonemes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))

                for (symbol in phonemes) {
                    val info = PhonemeDatabase.lookup(symbol)
                    if (info != null) {
                        PhonemeRow(
                            symbol = symbol,
                            name = info.name,
                            examples = info.examples,
                            bookLanguage = bookLanguage,
                            nativeLanguage = nativeLanguage,
                            onSpeak = onSpeak,
                        )
                    } else {
                        // Unknown symbol — just show it
                        PhonemeRow(
                            symbol = symbol,
                            name = "",
                            examples = emptyMap(),
                            bookLanguage = bookLanguage,
                            nativeLanguage = nativeLanguage,
                            onSpeak = onSpeak,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhonemeRow(
    symbol: String,
    name: String,
    examples: Map<String, String>,
    bookLanguage: String,
    nativeLanguage: String,
    onSpeak: (text: String, language: String) -> Unit,
) {
    val extColors = LocalExtendedColors.current
    val ipaFont = rememberIpaFontFamily()
    val bookLang = bookLanguage.lowercase().split("-", "_").first()
    val nativeLang = nativeLanguage.lowercase().split("-", "_").first()

    // Collect relevant examples: book language first, then native, then others
    val relevantExamples = buildList {
        examples[bookLang]?.let { add(bookLang to it) }
        if (nativeLang != bookLang) {
            examples[nativeLang]?.let { add(nativeLang to it) }
        }
        // Add one more example from any other language if we have fewer than 2
        if (size < 2) {
            examples.entries
                .filter { it.key != bookLang && it.key != nativeLang }
                .take(2 - size)
                .forEach { add(it.key to it.value) }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Play button — speak an example word, not the raw IPA symbol
            val speakExample = relevantExamples.firstOrNull()
            IconButton(
                onClick = {
                    if (speakExample != null) {
                        onSpeak(speakExample.second, speakExample.first)
                    }
                },
                enabled = speakExample != null,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Play $symbol",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.width(4.dp))

            // Symbol — use explicit font for IPA rendering
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = ipaFont),
                color = extColors.ipa,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
            )

            Spacer(Modifier.width(8.dp))

            // Description + examples
            Column(modifier = Modifier.weight(1f)) {
                if (name.isNotEmpty()) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (relevantExamples.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for ((lang, word) in relevantExamples) {
                            Row(
                                modifier = Modifier.clickable { onSpeak(word, lang) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = lang.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp,
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = word,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun languageDisplayName(code: String): String {
    val lang = code.lowercase().split("-", "_").first()
    return when (lang) {
        "en" -> "English"
        "de" -> "German"
        "fr" -> "French"
        "es" -> "Spanish"
        "it" -> "Italian"
        "pt" -> "Portuguese"
        "nl" -> "Dutch"
        "ru" -> "Russian"
        "ja" -> "Japanese"
        "zh" -> "Chinese"
        "ko" -> "Korean"
        "ar" -> "Arabic"
        "pl" -> "Polish"
        "sv" -> "Swedish"
        "da" -> "Danish"
        "no" -> "Norwegian"
        "fi" -> "Finnish"
        "cs" -> "Czech"
        "tr" -> "Turkish"
        "el" -> "Greek"
        "hu" -> "Hungarian"
        "ro" -> "Romanian"
        "uk" -> "Ukrainian"
        "hi" -> "Hindi"
        "th" -> "Thai"
        "vi" -> "Vietnamese"
        else -> code
    }
}
