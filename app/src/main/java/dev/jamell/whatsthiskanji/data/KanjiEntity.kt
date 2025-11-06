package dev.jamell.whatsthiskanji.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Main kanji entity representing a single kanji character
 */
@Entity(
    tableName = "kanji",
    indices = [Index(value = ["freq"])]
)
data class KanjiEntity(
    @PrimaryKey
    val literal: String,          // The kanji character itself
    val grade: Int?,              // Grade level (1-10)
    val strokeCount: Int?,        // Number of strokes
    val freq: Int?,               // Frequency ranking
    val jlpt: Int?                // JLPT level (1-5)
)

/**
 * Reading entity for on-yomi and kun-yomi readings
 */
@Entity(
    tableName = "readings",
    indices = [Index(value = ["literal"])]
)
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val literal: String,          // Foreign key to kanji
    val readingType: String,      // "ja_on" or "ja_kun"
    val reading: String           // The actual reading in kana
)

/**
 * Meaning entity for English meanings
 */
@Entity(
    tableName = "meanings",
    indices = [Index(value = ["literal"])]
)
data class MeaningEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val literal: String,          // Foreign key to kanji
    val meaning: String           // English meaning
)

/**
 * Metadata entity to track dictionary version
 */
@Entity(tableName = "dictionary_metadata")
data class DictionaryMetadata(
    @PrimaryKey
    val key: String,
    val value: String
)

/**
 * Saved word entity for user's study/review list
 */
@Entity(
    tableName = "saved_words",
    indices = [Index(value = ["timestamp"])]
)
data class SavedWordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,              // The word/phrase
    val reading: String,           // Reading in hiragana
    val meaning: String,           // English meaning(s)
    val context: String,           // Original context/sentence
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Combined data class for query results
 */
data class KanjiWithReadingsAndMeanings(
    val kanji: KanjiEntity,
    val onReadings: List<String>,
    val kunReadings: List<String>,
    val meanings: List<String>
)