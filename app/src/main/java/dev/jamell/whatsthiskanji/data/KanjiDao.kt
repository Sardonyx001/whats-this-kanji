package dev.jamell.whatsthiskanji.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for kanji database operations
 */
@Dao
interface KanjiDao {
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKanji(kanji: KanjiEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKanjiBatch(kanjis: List<KanjiEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: ReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<ReadingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeaning(meaning: MeaningEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeanings(meanings: List<MeaningEntity>)

    // Query operations
    @Query("SELECT * FROM kanji WHERE literal = :literal")
    suspend fun getKanji(literal: String): KanjiEntity?

    @Query("SELECT * FROM kanji WHERE literal IN (:literals)")
    suspend fun getKanjiBatch(literals: List<String>): List<KanjiEntity>

    @Query("SELECT * FROM readings WHERE literal = :literal")
    suspend fun getReadings(literal: String): List<ReadingEntity>

    @Query("SELECT * FROM meanings WHERE literal = :literal")
    suspend fun getMeanings(literal: String): List<MeaningEntity>

    @Query("""
        SELECT k.*,
               GROUP_CONCAT(DISTINCT CASE WHEN r.readingType = 'ja_on' THEN r.reading END) as onReadings,
               GROUP_CONCAT(DISTINCT CASE WHEN r.readingType = 'ja_kun' THEN r.reading END) as kunReadings,
               GROUP_CONCAT(DISTINCT m.meaning) as meanings
        FROM kanji k
        LEFT JOIN readings r ON k.literal = r.literal
        LEFT JOIN meanings m ON k.literal = m.literal
        WHERE k.literal = :literal
        GROUP BY k.literal
    """)
    suspend fun getKanjiWithDetails(literal: String): KanjiDetailsResult?

    @Query("""
        SELECT k.*,
               GROUP_CONCAT(DISTINCT CASE WHEN r.readingType = 'ja_on' THEN r.reading END) as onReadings,
               GROUP_CONCAT(DISTINCT CASE WHEN r.readingType = 'ja_kun' THEN r.reading END) as kunReadings,
               GROUP_CONCAT(DISTINCT m.meaning) as meanings
        FROM kanji k
        LEFT JOIN readings r ON k.literal = r.literal
        LEFT JOIN meanings m ON k.literal = m.literal
        WHERE k.literal IN (:literals)
        GROUP BY k.literal
    """)
    suspend fun getKanjiBatchWithDetails(literals: List<String>): List<KanjiDetailsResult>

    // Metadata operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: DictionaryMetadata)

    @Query("SELECT * FROM dictionary_metadata WHERE key = :key")
    suspend fun getMetadata(key: String): DictionaryMetadata?

    @Query("SELECT value FROM dictionary_metadata WHERE key = :key")
    suspend fun getMetadataValue(key: String): String?

    // Count operations for verification
    @Query("SELECT COUNT(*) FROM kanji")
    suspend fun getKanjiCount(): Int

    @Query("SELECT COUNT(*) FROM readings")
    suspend fun getReadingsCount(): Int

    @Query("SELECT COUNT(*) FROM meanings")
    suspend fun getMeaningsCount(): Int

    // Delete operations (for updates)
    @Query("DELETE FROM kanji")
    suspend fun deleteAllKanji()

    @Query("DELETE FROM readings")
    suspend fun deleteAllReadings()

    @Query("DELETE FROM meanings")
    suspend fun deleteAllMeanings()

    @Transaction
    suspend fun clearAllData() {
        deleteAllKanji()
        deleteAllReadings()
        deleteAllMeanings()
    }

    // Saved Words operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSavedWord(word: SavedWordEntity): Long

    @Query("SELECT * FROM saved_words ORDER BY timestamp DESC")
    suspend fun getAllSavedWords(): List<SavedWordEntity>

    @Query("SELECT * FROM saved_words WHERE word = :word LIMIT 1")
    suspend fun getSavedWord(word: String): SavedWordEntity?

    @Query("SELECT * FROM saved_words WHERE word = :word AND reading = :reading LIMIT 1")
    suspend fun getSavedWordByWordAndReading(word: String, reading: String): SavedWordEntity?

    @Delete
    suspend fun deleteSavedWord(word: SavedWordEntity)

    @Query("DELETE FROM saved_words WHERE id = :id")
    suspend fun deleteSavedWordById(id: Long)

    @Query("SELECT COUNT(*) FROM saved_words")
    suspend fun getSavedWordsCount(): Int
}

/**
 * Result class for combined query
 */
data class KanjiDetailsResult(
    val literal: String,
    val grade: Int?,
    val strokeCount: Int?,
    val freq: Int?,
    val jlpt: Int?,
    val onReadings: String?,
    val kunReadings: String?,
    val meanings: String?
) {
    fun toKanjiWithReadingsAndMeanings(): KanjiWithReadingsAndMeanings {
        return KanjiWithReadingsAndMeanings(
            kanji = KanjiEntity(literal, grade, strokeCount, freq, jlpt),
            onReadings = onReadings?.split(",")?.filterNot { it.isBlank() } ?: emptyList(),
            kunReadings = kunReadings?.split(",")?.filterNot { it.isBlank() } ?: emptyList(),
            meanings = meanings?.split(",")?.filterNot { it.isBlank() } ?: emptyList()
        )
    }
}