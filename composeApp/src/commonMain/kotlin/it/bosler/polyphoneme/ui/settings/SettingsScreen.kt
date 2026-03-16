package it.bosler.polyphoneme.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.bosler.polyphoneme.model.IpaPosition
import it.bosler.polyphoneme.model.ReadingMode
import kotlin.math.roundToInt

private val LANGUAGES = listOf(
    "en" to "English",
    "de" to "German",
    "fr" to "French",
    "es" to "Spanish",
    "it" to "Italian",
    "pt" to "Portuguese",
    "nl" to "Dutch",
    "ru" to "Russian",
    "ja" to "Japanese",
    "zh" to "Chinese",
    "ko" to "Korean",
    "ar" to "Arabic",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAbout: (() -> Unit)? = null,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Language Section
            SectionHeader("Language")

            var expanded by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("I speak") },
                supportingContent = {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        OutlinedTextField(
                            value = LANGUAGES.find { it.first == settings.nativeLanguage }?.second
                                ?: settings.nativeLanguage,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            LANGUAGES.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        viewModel.updateNativeLanguage(code)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                },
                leadingContent = {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Reading Section
            SectionHeader("Reading")

            // Reading Mode
            ReadingModePicker(
                selected = settings.readingMode,
                onSelect = { viewModel.updateReadingMode(it) },
            )

            // Font Size
            SettingSlider(
                icon = Icons.Default.FormatSize,
                title = "Font size",
                value = settings.fontSize.toFloat(),
                valueRange = 12f..28f,
                steps = 15,
                valueLabel = "${settings.fontSize}sp",
                onValueChange = { viewModel.updateFontSize(it.roundToInt()) },
            )

            // Line Spacing
            SettingSlider(
                icon = Icons.Default.FormatLineSpacing,
                title = "Line spacing",
                value = settings.lineSpacing,
                valueRange = 1.0f..3.0f,
                steps = 19,
                valueLabel = "%.1f".format(settings.lineSpacing),
                onValueChange = { viewModel.updateLineSpacing(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // IPA Section
            SectionHeader("Pronunciation (IPA)")

            IpaPositionPicker(
                selected = settings.ipaPosition,
                onSelect = { viewModel.updateIpaPosition(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Translation Section
            SectionHeader("Translation")

            SettingSlider(
                icon = Icons.Default.Translate,
                title = "Translation frequency",
                value = settings.translationFrequency,
                valueRange = 0f..1f,
                steps = 0,
                valueLabel = when {
                    settings.translationFrequency <= 0.01f -> "Off"
                    settings.translationFrequency >= 0.99f -> "All words"
                    else -> "${(settings.translationFrequency * 100).toInt()}%"
                },
                onValueChange = { viewModel.updateTranslationFrequency(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // About Section
            if (onAbout != null) {
                ListItem(
                    headlineContent = { Text("About PolyPhoneme") },
                    supportingContent = { Text("Version, help & licenses") },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable(onClick = onAbout),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Visual IPA Position Picker ──────────────────────────────────────────────
// Spatial layout: Above at top, Below at bottom, Before on left, Behind on right, Replace in center

@Composable
private fun IpaPositionPicker(
    selected: IpaPosition,
    onSelect: (IpaPosition) -> Unit,
) {
    ListItem(
        headlineContent = { Text("IPA position") },
        supportingContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Above — top center
                IpaPositionChip(
                    label = "Above",
                    isSelected = selected == IpaPosition.ABOVE,
                    onClick = { onSelect(IpaPosition.ABOVE) },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                // Before — center left
                IpaPositionChip(
                    label = "Before",
                    isSelected = selected == IpaPosition.BEFORE,
                    onClick = { onSelect(IpaPosition.BEFORE) },
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                // Replace — center
                IpaPositionChip(
                    label = "Replace",
                    isSelected = selected == IpaPosition.REPLACE,
                    onClick = { onSelect(IpaPosition.REPLACE) },
                    modifier = Modifier.align(Alignment.Center),
                )
                // Behind — center right
                IpaPositionChip(
                    label = "Behind",
                    isSelected = selected == IpaPosition.BEHIND,
                    onClick = { onSelect(IpaPosition.BEHIND) },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
                // Below — bottom center
                IpaPositionChip(
                    label = "Below",
                    isSelected = selected == IpaPosition.BELOW,
                    onClick = { onSelect(IpaPosition.BELOW) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        },
        leadingContent = {
            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
    )
}

@Composable
private fun IpaPositionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.surfaceContainerLow

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

// ── Reading Mode Picker ─────────────────────────────────────────────────────

@Composable
private fun ReadingModePicker(
    selected: ReadingMode,
    onSelect: (ReadingMode) -> Unit,
) {
    ListItem(
        headlineContent = { Text("Reading mode") },
        supportingContent = {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReadingModeCard(
                    mode = ReadingMode.PAGE,
                    label = "Page",
                    isSelected = selected == ReadingMode.PAGE,
                    onClick = { onSelect(ReadingMode.PAGE) },
                    modifier = Modifier.weight(1f),
                ) {
                    // Mini page preview: a page outline with left/right arrows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            repeat(3) {
                                Surface(
                                    modifier = Modifier
                                        .padding(vertical = 1.dp)
                                        .width(if (it == 2) 20.dp else 28.dp)
                                        .height(2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(1.dp),
                                    content = {},
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
                ReadingModeCard(
                    mode = ReadingMode.SCROLL,
                    label = "Scroll",
                    isSelected = selected == ReadingMode.SCROLL,
                    onClick = { onSelect(ReadingMode.SCROLL) },
                    modifier = Modifier.weight(1f),
                ) {
                    // Mini scroll preview: lines with a scrollbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            repeat(4) {
                                Surface(
                                    modifier = Modifier
                                        .padding(vertical = 1.dp)
                                        .fillMaxWidth(if (it == 3) 0.6f else 1f)
                                        .height(2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(1.dp),
                                    content = {},
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        // Scrollbar indicator
                        Surface(
                            modifier = Modifier.width(3.dp).height(20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(1.5.dp),
                        ) {
                            Column {
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    modifier = Modifier.width(3.dp).height(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(1.5.dp),
                                    content = {},
                                )
                            }
                        }
                    }
                }
            }
        },
        leadingContent = {
            Icon(Icons.Default.SwapVert, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
    )
}

@Composable
private fun ReadingModeCard(
    mode: ReadingMode,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.surfaceContainerLow

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(modifier = Modifier.height(28.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                preview()
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Shared Components ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingSlider(
    icon: ImageVector,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f))
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        supportingContent = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
    )
}
