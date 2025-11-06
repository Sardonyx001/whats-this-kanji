package dev.jamell.whatsthiskanji.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.jamell.whatsthiskanji.data.*
import dev.jamell.whatsthiskanji.tokenizer.JapaneseTokenizer
import dev.jamell.whatsthiskanji.tokenizer.TokenizedWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for dictionary initialization and management
 */
class DictionaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DictionaryRepository(application.applicationContext)
    private val tokenizer = JapaneseTokenizer()

    private val _initializationStatus = MutableStateFlow<InitializationStatus?>(null)
    val initializationStatus: StateFlow<InitializationStatus?> = _initializationStatus.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _databaseStats = MutableStateFlow<DatabaseStats?>(null)
    val databaseStats: StateFlow<DatabaseStats?> = _databaseStats.asStateFlow()

    // Removed automatic initialization check from init block
    // User must manually trigger initialization

    /**
     * Check if dictionary is initialized
     */
    fun checkInitialization() {
        viewModelScope.launch {
            _isInitialized.value = repository.isDictionaryInitialized()
            if (_isInitialized.value) {
                _databaseStats.value = repository.getDatabaseStats()
            }
        }
    }

    /**
     * Initialize the dictionary (download and parse)
     */
    fun initializeDictionary() {
        viewModelScope.launch {
            repository.initializeDictionary().collect { status ->
                _initializationStatus.value = status

                if (status is InitializationStatus.Completed) {
                    _isInitialized.value = true
                    _databaseStats.value = repository.getDatabaseStats()
                }
            }
        }
    }

    /**
     * Process selected text and extract kanji readings
     *
     * @param text Selected text to process
     * @return List of word readings with kanji details
     */
    suspend fun processText(text: String): ProcessedTextResult {
        // Tokenize the text into words
        val tokens = tokenizer.tokenize(text)

        // Extract unique kanji from all tokens
        val allKanji = tokens.flatMap { token ->
            tokenizer.extractKanji(token.surface)
        }.toSet()

        // Query database for kanji details
        val kanjiDetails = repository.getKanjiBatch(allKanji.toList())

        // Create word readings by combining tokenizer and database info
        val wordReadings = tokens.filter { token ->
            token.surface.any { tokenizer.isKanji(it) }
        }.map { token ->
            WordReading(
                word = token.surface,
                reading = token.reading,
                baseForm = token.baseForm,
                partOfSpeech = token.partOfSpeech
            )
        }

        return ProcessedTextResult(
            originalText = text,
            wordReadings = wordReadings,
            kanjiDetails = kanjiDetails
        )
    }

    /**
     * Get database statistics
     */
    fun refreshStats() {
        viewModelScope.launch {
            _databaseStats.value = repository.getDatabaseStats()
        }
    }
}

/**
 * Result of processing text
 */
data class ProcessedTextResult(
    val originalText: String,
    val wordReadings: List<WordReading>,
    val kanjiDetails: List<KanjiWithReadingsAndMeanings>
)

/**
 * Word reading information
 */
data class WordReading(
    val word: String,
    val reading: String,
    val baseForm: String,
    val partOfSpeech: String
)