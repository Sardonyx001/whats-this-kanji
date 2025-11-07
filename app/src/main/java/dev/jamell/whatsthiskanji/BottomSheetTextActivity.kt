package dev.jamell.whatsthiskanji

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jamell.whatsthiskanji.data.KanjiDatabase
import dev.jamell.whatsthiskanji.ui.theme.WhatsThisKanjiTheme
import dev.jamell.whatsthiskanji.viewmodel.DictionaryViewModel
import dev.jamell.whatsthiskanji.viewmodel.WordReading
import kotlinx.coroutines.launch

/**
 * Bottom sheet activity for medium-length text selections
 * Shows a modal bottom sheet with word readings and basic kanji info
 */
class BottomSheetTextActivity : ComponentActivity() {

    private val viewModel: DictionaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the selected text from the intent
        val selectedText = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
            }
            else -> ""
        }

        val database = KanjiDatabase.getInstance(this)
        val activity = this

        setContent {
            WhatsThisKanjiTheme {
                // Transparent background to show the app underneath
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    BottomSheetScreen(
                        selectedText = selectedText,
                        viewModel = viewModel,
                        database = database,
                        onDismiss = { activity.finish() },
                        onOpenFull = {
                            // Open full activity
                            val fullIntent = Intent(activity, ProcessTextActivity::class.java).apply {
                                action = Intent.ACTION_PROCESS_TEXT
                                putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText)
                            }
                            activity.startActivity(fullIntent)
                            activity.finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetScreen(
    selectedText: String,
    viewModel: DictionaryViewModel,
    database: KanjiDatabase,
    onDismiss: () -> Unit,
    onOpenFull: () -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var wordReadings by remember { mutableStateOf<List<WordReading>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    LaunchedEffect(selectedText) {
        scope.launch {
            try {
                val result = viewModel.processText(selectedText)
                wordReadings = result.wordReadings
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
                isLoading = false
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                onDismiss()
            },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            scrimColor = Color.Black.copy(alpha = 0.32f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        showBottomSheet = false
                        onDismiss()
                    }) {
                        Text("✕", fontSize = 20.sp, fontWeight = FontWeight.Light)
                    }
                }

                // Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    errorMessage != null -> {
                        Text(
                            text = "Error: $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 32.dp)
                        )
                    }

                    wordReadings.isNotEmpty() -> {
                        // Two separate scrollable panes
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Top pane: Selected text
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp)
                                    .verticalScroll(rememberScrollState())
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Selected Text",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = selectedText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            // Bottom pane: Word readings
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "Readings",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    wordReadings.forEach { wordReading ->
                                        BottomSheetWordCard(wordReading)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // View Full Details button
                        Button(
                            onClick = {
                                showBottomSheet = false
                                onOpenFull()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Full Details")
                        }
                    }

                    else -> {
                        Text(
                            text = "No Japanese text found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomSheetWordCard(wordReading: WordReading) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = wordReading.word,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = wordReading.reading,
                fontSize = 15.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        if (wordReading.partOfSpeech.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = wordReading.partOfSpeech,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
