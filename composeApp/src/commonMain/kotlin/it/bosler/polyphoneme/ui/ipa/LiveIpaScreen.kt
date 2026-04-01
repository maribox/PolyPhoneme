package it.bosler.polyphoneme.ui.ipa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.bosler.polyphoneme.ui.theme.rememberIpaFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiveIpaScreen(
    onBack: () -> Unit,
    viewModel: LiveIpaViewModel = viewModel { LiveIpaViewModel() },
) {
    val inputText by viewModel.inputText.collectAsState()
    val results by viewModel.results.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val ipaFont = rememberIpaFontFamily()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live IPA") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Text input
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.updateText(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Type or paste text") },
                placeholder = { Text("Bonjour, comment allez-vous?") },
                minLines = 2,
                maxLines = 5,
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateText("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            // Progress indicator
            AnimatedVisibility(visible = isTranscribing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Results — one section per language, sorted by hit rate
            for (result in results) {
                val pct = (result.hitRate * 100).toInt()

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Language header with play button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = result.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = " $pct%",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.speak(inputText, result.code) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Play ${result.name}",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // IPA cards with original words shown inline for unmatched words
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (ipaWord in result.ipaWords) {
                        if (ipaWord.ipa != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.clickable {
                                    viewModel.speak(ipaWord.word, result.code)
                                },
                            ) {
                                Text(
                                    text = ipaWord.ipa,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = ipaFont),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        } else {
                            Text(
                                text = ipaWord.word,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
