package dev.jamell.whatsthiskanji

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.jamell.whatsthiskanji.ui.theme.WhatsThisKanjiTheme
import dev.jamell.whatsthiskanji.viewmodel.DictionaryViewModel
import kotlinx.coroutines.launch

/**
 * Popup window activity for short text selections
 * Shows a small floating dialog with basic kanji readings
 */
class PopupTextActivity : ComponentActivity() {

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

        setContent {
            WhatsThisKanjiTheme {
                PopupDialog(
                    selectedText = selectedText,
                    viewModel = viewModel,
                    onDismiss = { finish() },
                    onOpenFull = {
                        // Open full activity
                        val fullIntent = Intent(this, ProcessTextActivity::class.java).apply {
                            action = Intent.ACTION_PROCESS_TEXT
                            putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText)
                        }
                        startActivity(fullIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun PopupDialog(
    selectedText: String,
    viewModel: DictionaryViewModel,
    onDismiss: () -> Unit,
    onOpenFull: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var readings by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFullText by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Truncate text if too long (max 50 characters for display)
    val displayText = if (!showFullText && selectedText.length > 50) {
        selectedText.take(50) + "..."
    } else {
        selectedText
    }

    LaunchedEffect(selectedText) {
        scope.launch {
            try {
                val result = viewModel.processText(selectedText)
                // Extract just the readings for compact display
                readings = result.wordReadings.map { "${it.word} (${it.reading})" }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal
                        )
                        // Show "see more" button if text is truncated
                        if (selectedText.length > 50 && !showFullText) {
                            TextButton(
                                onClick = { showFullText = true },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    "See more",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Light)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    readings.isNotEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            readings.take(3).forEach { reading ->
                                Text(
                                    text = reading,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Light
                                )
                            }

                            if (readings.size > 3) {
                                Text(
                                    text = "+${readings.size - 3} more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // View More button
                        TextButton(
                            onClick = onOpenFull,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                "View Details",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = "No Japanese text found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}