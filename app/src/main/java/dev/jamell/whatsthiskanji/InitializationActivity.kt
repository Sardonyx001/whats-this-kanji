package dev.jamell.whatsthiskanji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jamell.whatsthiskanji.data.InitializationStatus
import dev.jamell.whatsthiskanji.ui.theme.WhatsThisKanjiTheme
import dev.jamell.whatsthiskanji.viewmodel.DictionaryViewModel

/**
 * Activity for initializing the dictionary database
 */
class InitializationActivity : ComponentActivity() {

    private val viewModel: DictionaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WhatsThisKanjiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InitializationScreen(
                        viewModel = viewModel,
                        onComplete = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun InitializationScreen(
    viewModel: DictionaryViewModel,
    onComplete: () -> Unit
) {
    val initStatus by viewModel.initializationStatus.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState()
    val databaseStats by viewModel.databaseStats.collectAsState()

    // Check initialization status when screen opens
    LaunchedEffect(Unit) {
        viewModel.checkInitialization()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Dictionary",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        when {
            isInitialized && initStatus == null -> {
                // Already initialized, show stats
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status indicator
                    Text(
                        text = "Ready",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Dictionary info
                    databaseStats?.let { stats ->
                        DictionaryInfoRow(label = "Kanji", value = "${stats.kanjiCount}")
                        Spacer(modifier = Modifier.height(8.dp))
                        DictionaryInfoRow(label = "Readings", value = "${stats.readingsCount}")
                        Spacer(modifier = Modifier.height(8.dp))
                        DictionaryInfoRow(label = "Meanings", value = "${stats.meaningsCount}")

                        if (stats.version != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            DictionaryInfoRow(label = "Version", value = stats.version)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Source info
                    Text(
                        text = "KANJIDIC2 Dictionary\nwww.edrdg.org/kanjidic",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    OutlinedButton(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Done")
                    }
                }
            }

            initStatus == null && !isInitialized -> {
                // Not initialized yet
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Not initialized",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Install the KANJIDIC2 dictionary\nfor offline kanji lookups",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 48.dp)
                    )

                    OutlinedButton(
                        onClick = { viewModel.initializeDictionary() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Install Dictionary")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Source: www.edrdg.org/kanjidic",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            else -> {
                // Show initialization progress
                InitializationProgress(status = initStatus!!, onComplete = onComplete)
            }
        }
    }
}

@Composable
fun DictionaryInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun InitializationProgress(
    status: InitializationStatus,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (status) {
            is InitializationStatus.CopyingFromAsset -> {
                Text(
                    text = "Installing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                LinearProgressIndicator(
                    progress = { status.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Copying from assets...",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            is InitializationStatus.Downloading -> {
                Text(
                    text = "Downloading",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                LinearProgressIndicator(
                    progress = { status.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${status.progress}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            is InitializationStatus.Decompressing -> {
                Text(
                    text = "Processing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            is InitializationStatus.Parsing -> {
                Text(
                    text = "Parsing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (status.processed > 0) {
                    Text(
                        text = "${status.processed} kanji",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            is InitializationStatus.StoringInDatabase -> {
                Text(
                    text = "Storing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                LinearProgressIndicator(
                    progress = { status.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${status.progress}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            is InitializationStatus.Completed -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Complete",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    DictionaryInfoRow(label = "Kanji", value = "${status.kanjiCount}")
                    Spacer(modifier = Modifier.height(8.dp))
                    DictionaryInfoRow(label = "Readings", value = "${status.readingsCount}")
                    Spacer(modifier = Modifier.height(8.dp))
                    DictionaryInfoRow(label = "Meanings", value = "${status.meaningsCount}")

                    Spacer(modifier = Modifier.height(48.dp))

                    OutlinedButton(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Done")
                    }
                }
            }

            is InitializationStatus.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// Previews
@Preview(showBackground = true)
@Composable
fun DictionaryInfoRowPreview() {
    WhatsThisKanjiTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DictionaryInfoRow(label = "Kanji", value = "13,108")
            Spacer(modifier = Modifier.height(8.dp))
            DictionaryInfoRow(label = "Readings", value = "37,043")
            Spacer(modifier = Modifier.height(8.dp))
            DictionaryInfoRow(label = "Meanings", value = "24,821")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InitializationProgressDownloadingPreview() {
    WhatsThisKanjiTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            InitializationProgress(
                status = InitializationStatus.Downloading(45),
                onComplete = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InitializationProgressParsingPreview() {
    WhatsThisKanjiTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            InitializationProgress(
                status = InitializationStatus.Parsing(5000),
                onComplete = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InitializationProgressCompletedPreview() {
    WhatsThisKanjiTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            InitializationProgress(
                status = InitializationStatus.Completed(
                    kanjiCount = 13108,
                    readingsCount = 37043,
                    meaningsCount = 24821
                ),
                onComplete = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InitializationProgressErrorPreview() {
    WhatsThisKanjiTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            InitializationProgress(
                status = InitializationStatus.Error("Failed to download dictionary file. Please check your internet connection."),
                onComplete = {}
            )
        }
    }
}