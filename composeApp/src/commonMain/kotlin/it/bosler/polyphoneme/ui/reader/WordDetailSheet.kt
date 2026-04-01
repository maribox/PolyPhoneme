package it.bosler.polyphoneme.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.bosler.polyphoneme.data.ipa.PhonemeDatabase
import it.bosler.polyphoneme.data.ipa.PhonemeTerms
import it.bosler.polyphoneme.data.ipa.TermCategory
import it.bosler.polyphoneme.model.Token
import it.bosler.polyphoneme.ui.settings.IPA_VOWEL_SYMBOLS
import it.bosler.polyphoneme.ui.settings.IpaVowelChart
import it.bosler.polyphoneme.ui.theme.LocalExtendedColors
import it.bosler.polyphoneme.ui.theme.rememberIpaFontFamily
import kotlinx.coroutines.launch

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
    var expandedVowel by remember { mutableStateOf<String?>(null) }
    val langName = languageDisplayName(bookLanguage)
    val wiktionaryLang = bookLanguage.lowercase().split("-", "_").first()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        // Back always dismisses the sheet (not partially collapse)
        BackHandler { onDismiss() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
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

            if (token.translation != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = token.translation,
                    style = MaterialTheme.typography.titleMedium,
                    color = extColors.translation,
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
                    val isVowel = symbol in IPA_VOWEL_SYMBOLS
                    val isExpanded = expandedVowel == symbol

                    Column {
                        PhonemeRow(
                            symbol = symbol,
                            name = info?.name ?: "",
                            examples = info?.examples ?: emptyMap(),
                            bookLanguage = bookLanguage,
                            nativeLanguage = nativeLanguage,
                            onSpeak = onSpeak,
                            isVowel = isVowel,
                            isChartExpanded = isExpanded,
                            onToggleChart = {
                                expandedVowel = if (isExpanded) null else symbol
                                if (!isExpanded) {
                                    scope.launch {
                                        // Small delay for AnimatedVisibility to start expanding
                                        kotlinx.coroutines.delay(100)
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                }
                            },
                        )

                        AnimatedVisibility(visible = isExpanded) {
                            var chartSelectedVowel by remember { mutableStateOf(symbol) }
                            Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                                IpaVowelChart(
                                    highlightedVowels = setOf(symbol),
                                    onVowelSelected = { if (it != null) chartSelectedVowel = it },
                                )
                                // Show PhonemeRow for tapped chart vowel (fixed height to prevent layout shifts)
                                Box(modifier = Modifier.defaultMinSize(minHeight = 72.dp)) {
                                    val vowelInfo = PhonemeDatabase.lookup(chartSelectedVowel)
                                    PhonemeRow(
                                        symbol = chartSelectedVowel,
                                        name = vowelInfo?.name ?: "",
                                        examples = vowelInfo?.examples ?: emptyMap(),
                                        bookLanguage = bookLanguage,
                                        nativeLanguage = nativeLanguage,
                                        onSpeak = onSpeak,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(4.dp))
                            }
                        }
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
    isVowel: Boolean = false,
    isChartExpanded: Boolean = false,
    onToggleChart: (() -> Unit)? = null,
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
                    ClickablePhonemeDescription(name = name)
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

            if (isVowel && onToggleChart != null) {
                VowelChartIcon(
                    vowelSymbol = symbol,
                    active = isChartExpanded,
                    onClick = onToggleChart,
                )
            }
        }
    }
}

@Composable
private fun VowelChartIcon(
    vowelSymbol: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
    val ipaFont = rememberIpaFontFamily()
    val textMeasurer = rememberTextMeasurer()
    val style = TextStyle(
        fontSize = 12.sp,
        fontFamily = ipaFont,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = TextAlign.Center,
    )
    val measuredText = remember(vowelSymbol, style) { textMeasurer.measure(vowelSymbol, style) }

    Canvas(
        modifier = modifier
            .size(28.dp)
            .clickable(onClick = onClick),
    ) {
        val w = size.width
        val h = size.height
        val pad = w * 0.08f

        // IPA trapezoid: full width top, left edge narrows inward at bottom
        val path = Path().apply {
            moveTo(pad, pad)                          // top-left
            lineTo(w - pad, pad)                      // top-right
            lineTo(w - pad, h - pad)                  // bottom-right
            lineTo(w * 0.35f, h - pad)                // bottom-left (indented)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 1.6f))

        // Center vowel in the trapezoid centroid (slightly right of box center)
        val cx = (pad + (w - pad) + (w - pad) + w * 0.35f) / 4f
        val cy = h / 2f
        drawText(
            textLayoutResult = measuredText,
            topLeft = Offset(
                cx - measuredText.size.width / 2f,
                cy - measuredText.size.height / 2f,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClickablePhonemeDescription(name: String) {
    val terms = remember(name) { PhonemeTerms.parseDescription(name) }
    var expandedTerm by remember { mutableStateOf<String?>(null) }

    if (terms.isEmpty()) {
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
        return
    }

    Column {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (term in terms) {
                val isExpanded = expandedTerm == term.term
                val bgColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                val textColor = if (isExpanded) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier.clickable {
                        expandedTerm = if (isExpanded) null else term.term
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = bgColor,
                ) {
                    Text(
                        text = term.term,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }

        // Show explanation for expanded term
        AnimatedVisibility(visible = expandedTerm != null) {
            val info = expandedTerm?.let { PhonemeTerms.lookup(it) }
            if (info != null) {
                Text(
                    text = info.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
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
