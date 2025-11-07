package dev.jamell.whatsthiskanji

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jamell.whatsthiskanji.data.KanjiDatabase
import dev.jamell.whatsthiskanji.data.KanjiEntity
import dev.jamell.whatsthiskanji.data.KanjiWithReadingsAndMeanings
import dev.jamell.whatsthiskanji.data.SavedWordEntity
import dev.jamell.whatsthiskanji.ui.theme.WhatsThisKanjiTheme
import dev.jamell.whatsthiskanji.viewmodel.DictionaryViewModel
import dev.jamell.whatsthiskanji.viewmodel.WordReading
import kotlinx.coroutines.launch

/**
 * Activity to process selected text and show kanji/word readings
 */
class ProcessTextActivity : ComponentActivity() {

    private val viewModel: DictionaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get the selected text from the intent
        val selectedText = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
            }
            else -> ""
        }

        val database = KanjiDatabase.getInstance(this)

        setContent {
            WhatsThisKanjiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProcessTextScreen(
                        selectedText = selectedText,
                        viewModel = viewModel,
                        database = database,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessTextScreen(
    selectedText: String,
    viewModel: DictionaryViewModel,
    database: KanjiDatabase,
    onClose: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var wordReadings by remember { mutableStateOf<List<WordReading>>(emptyList()) }
    var kanjiDetails by remember { mutableStateOf<List<KanjiWithReadingsAndMeanings>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFullText by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Truncate text if too long (max 100 characters for display)
    val displayText = if (!showFullText && selectedText.length > 100) {
        selectedText.take(100) + "..."
    } else {
        selectedText
    }

    LaunchedEffect(selectedText) {
        scope.launch {
            try {
                val result = viewModel.processText(selectedText)
                wordReadings = result.wordReadings
                kanjiDetails = result.kanjiDetails
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Reading",
                        fontWeight = FontWeight.Light
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Text("✕", fontSize = 20.sp, fontWeight = FontWeight.Light)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Display selected text - minimal with truncation
                    Column {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 20.sp,
                            lineHeight = 32.sp,
                            modifier = Modifier.padding(bottom = if (selectedText.length > 100 && !showFullText) 8.dp else 32.dp)
                        )

                        // Show "see more" button if text is truncated
                        if (selectedText.length > 100 && !showFullText) {
                            TextButton(
                                onClick = { showFullText = true },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                Text(
                                    "See more",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Word Readings Section
                    if (wordReadings.isNotEmpty()) {
                        Text(
                            text = "Words",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        wordReadings.forEach { wordReading ->
                            WordReadingCard(
                                wordReading = wordReading,
                                kanjiDetails = kanjiDetails,
                                selectedText = selectedText,
                                database = database
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // Kanji Details Section
                    if (kanjiDetails.isNotEmpty()) {
                        Text(
                            text = "Kanji",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        kanjiDetails.forEach { kanji ->
                            KanjiDetailCard(kanji)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    if (wordReadings.isEmpty() && kanjiDetails.isEmpty()) {
                        Text(
                            text = "No Japanese text found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WordReadingCard(
    wordReading: WordReading,
    kanjiDetails: List<KanjiWithReadingsAndMeanings>,
    selectedText: String,
    database: KanjiDatabase
) {
    var isSaved by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferencesRepository = remember { dev.jamell.whatsthiskanji.settings.UserPreferencesRepository(context) }
    val preferences by preferencesRepository.userPreferencesFlow.collectAsState(
        initial = dev.jamell.whatsthiskanji.settings.UserPreferences()
    )

    // Get meanings from kanji in this word
    val meanings = remember(wordReading, kanjiDetails) {
        val kanjiInWord = wordReading.word.filter { char ->
            char in '\u4E00'..'\u9FFF'
        }.map { it.toString() }

        kanjiDetails
            .filter { it.kanji.literal in kanjiInWord }
            .flatMap { it.meanings }
            .distinct()
            .take(3)
            .joinToString(", ")
    }

    // Check if already saved on first composition
    LaunchedEffect(wordReading) {
        scope.launch {
            val existingWord = database.kanjiDao().getSavedWordByWordAndReading(
                wordReading.word,
                wordReading.reading
            )
            if (existingWord != null) {
                isSaved = true
            }
        }
    }

    // Auto-save if enabled
    LaunchedEffect(wordReading, preferences.autoSaveWords) {
        if (preferences.autoSaveWords && !isSaved) {
            scope.launch {
                val savedWord = SavedWordEntity(
                    word = wordReading.word,
                    reading = wordReading.reading,
                    meaning = meanings.ifBlank { wordReading.partOfSpeech },
                    context = selectedText
                )
                val rowId = database.kanjiDao().insertSavedWord(savedWord)
                if (rowId > 0) {
                    isSaved = true
                    showSavedMessage = true
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = wordReading.word,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = wordReading.reading,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Meanings
                if (meanings.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = meanings,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Saved message
                if (showSavedMessage) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1500)
                        showSavedMessage = false
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            // Save button
            IconButton(
                onClick = {
                    scope.launch {
                        val savedWord = SavedWordEntity(
                            word = wordReading.word,
                            reading = wordReading.reading,
                            meaning = meanings.ifBlank { wordReading.partOfSpeech },
                            context = selectedText
                        )
                        val rowId = database.kanjiDao().insertSavedWord(savedWord)
                        if (rowId > 0) {
                            isSaved = true
                            showSavedMessage = true
                        }
                    }
                },
                enabled = !isSaved
            ) {
                Text(
                    text = if (isSaved) "✓" else "○",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = if (isSaved)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun KanjiDetailCard(kanjiDetail: KanjiWithReadingsAndMeanings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Kanji character
        Text(
            text = kanjiDetail.kanji.literal,
            fontSize = 40.sp,
            fontWeight = FontWeight.Normal
        )

        // Readings and meanings
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Readings
            val readings = buildList {
                if (kanjiDetail.onReadings.isNotEmpty()) {
                    addAll(kanjiDetail.onReadings)
                }
                if (kanjiDetail.kunReadings.isNotEmpty()) {
                    addAll(kanjiDetail.kunReadings)
                }
            }.take(4).joinToString(" · ")

            if (readings.isNotEmpty()) {
                Text(
                    text = readings,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Meanings
            if (kanjiDetail.meanings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = kanjiDetail.meanings.take(3).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Meta info
            val metaInfo = buildList {
                kanjiDetail.kanji.jlpt?.let { add("N$it") }
                kanjiDetail.kanji.strokeCount?.let { add("$it strokes") }
            }.joinToString(" · ")

            if (metaInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = metaInfo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// Previews
@Preview(showBackground = true)
@Composable
fun KanjiDetailCardPreview() {
    WhatsThisKanjiTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            KanjiDetailCard(
                kanjiDetail = KanjiWithReadingsAndMeanings(
                    kanji = KanjiEntity(
                        literal = "漢",
                        grade = 3,
                        strokeCount = 13,
                        freq = 1000,
                        jlpt = 4
                    ),
                    onReadings = listOf("カン"),
                    kunReadings = listOf("から"),
                    meanings = listOf("Sino-", "China")
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KanjiDetailCardComplexPreview() {
    WhatsThisKanjiTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            KanjiDetailCard(
                kanjiDetail = KanjiWithReadingsAndMeanings(
                    kanji = KanjiEntity(
                        literal = "日",
                        grade = 1,
                        strokeCount = 4,
                        freq = 1,
                        jlpt = 5
                    ),
                    onReadings = listOf("ニチ", "ジツ"),
                    kunReadings = listOf("ひ", "か"),
                    meanings = listOf("day", "sun", "Japan", "counter for days")
                )
            )
        }
    }
}
