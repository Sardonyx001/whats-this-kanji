package dev.jamell.whatsthiskanji.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

/**
 * Repository for managing dictionary data
 *
 * Handles downloading, parsing, and querying KANJIDIC2 dictionary
 */
class DictionaryRepository(private val context: Context) {

    private val database = KanjiDatabase.getInstance(context)
    private val dao = database.kanjiDao()
    private val parser = Kanjidic2Parser()

    companion object {
        private const val TAG = "DictionaryRepository"
        private const val KANJIDIC2_URL = "https://www.edrdg.org/kanjidic/kanjidic2.xml.gz"
        private const val VERSION_KEY = "kanjidic2_version"
        private const val INITIALIZED_KEY = "dictionary_initialized"
        private const val ASSET_DB_NAME = "kanjidic2.db"

        // Current expected version - update this when KANJIDIC2 is updated
        private const val CURRENT_VERSION = "2025-01"
    }

    /**
     * Check if dictionary is initialized and up to date
     */
    suspend fun isDictionaryInitialized(): Boolean {
        return withContext(Dispatchers.IO) {
            val initialized = dao.getMetadataValue(INITIALIZED_KEY) == "true"
            val version = dao.getMetadataValue(VERSION_KEY)
            val hasData = dao.getKanjiCount() > 0

            initialized && hasData && version == CURRENT_VERSION
        }
    }

    /**
     * Get current dictionary version
     */
    suspend fun getCurrentVersion(): String? {
        return withContext(Dispatchers.IO) {
            dao.getMetadataValue(VERSION_KEY)
        }
    }

    /**
     * Initialize dictionary from bundled asset (fast, offline)
     *
     * @return Flow emitting initialization status
     */
    fun initializeFromAsset(): Flow<InitializationStatus> = flow {
        try {
            emit(InitializationStatus.CopyingFromAsset(0))

            // Copy the pre-built database from assets
            copyDatabaseFromAsset()

            emit(InitializationStatus.CopyingFromAsset(100))

            // Verify it worked
            val stats = getDatabaseStats()

            emit(InitializationStatus.Completed(
                kanjiCount = stats.kanjiCount,
                readingsCount = stats.readingsCount,
                meaningsCount = stats.meaningsCount
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing from asset", e)
            emit(InitializationStatus.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Download and initialize the dictionary (slower, requires internet)
     *
     * @return Flow emitting initialization status
     */
    fun initializeFromDownload(): Flow<InitializationStatus> = flow {
        try {
            emit(InitializationStatus.Downloading(0))

            // Download the dictionary file
            val gzFile = downloadKanjidic2()
            emit(InitializationStatus.Downloading(100))

            // Decompress the file
            emit(InitializationStatus.Decompressing)
            val xmlFile = decompressGzFile(gzFile)

            // Parse the XML file
            emit(InitializationStatus.Parsing(0))
            val parsedData = parser.parse(FileInputStream(xmlFile)) { processed ->
                // Can't emit from callback, but we track progress
                Log.d(TAG, "Parsed $processed kanji")
            }

            // Store in database
            emit(InitializationStatus.StoringInDatabase(0))
            storeInDatabase(parsedData) { progress ->
                // Emit progress updates
            }

            // Update metadata
            dao.insertMetadata(DictionaryMetadata(VERSION_KEY, CURRENT_VERSION))
            dao.insertMetadata(DictionaryMetadata(INITIALIZED_KEY, "true"))

            // Clean up temporary files
            gzFile.delete()
            xmlFile.delete()

            emit(InitializationStatus.Completed(
                kanjiCount = dao.getKanjiCount(),
                readingsCount = dao.getReadingsCount(),
                meaningsCount = dao.getMeaningsCount()
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing from download", e)
            emit(InitializationStatus.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Initialize dictionary - tries asset first, falls back to download
     */
    fun initializeDictionary(): Flow<InitializationStatus> = flow {
        try {
            // Try asset first (fast and offline)
            emit(InitializationStatus.CopyingFromAsset(0))
            copyDatabaseFromAsset()
            emit(InitializationStatus.CopyingFromAsset(100))

            val stats = getDatabaseStats()
            emit(InitializationStatus.Completed(
                kanjiCount = stats.kanjiCount,
                readingsCount = stats.readingsCount,
                meaningsCount = stats.meaningsCount
            ))

        } catch (assetError: Exception) {
            Log.w(TAG, "Asset initialization failed, trying download", assetError)

            // Fall back to download
            try {
                emit(InitializationStatus.Downloading(0))
                val gzFile = downloadKanjidic2()
                emit(InitializationStatus.Downloading(100))

                emit(InitializationStatus.Decompressing)
                val xmlFile = decompressGzFile(gzFile)

                emit(InitializationStatus.Parsing(0))
                val parsedData = parser.parse(FileInputStream(xmlFile)) { processed ->
                    Log.d(TAG, "Parsed $processed kanji")
                }

                emit(InitializationStatus.StoringInDatabase(0))
                storeInDatabase(parsedData) { progress -> }

                dao.insertMetadata(DictionaryMetadata(VERSION_KEY, CURRENT_VERSION))
                dao.insertMetadata(DictionaryMetadata(INITIALIZED_KEY, "true"))

                gzFile.delete()
                xmlFile.delete()

                emit(InitializationStatus.Completed(
                    kanjiCount = dao.getKanjiCount(),
                    readingsCount = dao.getReadingsCount(),
                    meaningsCount = dao.getMeaningsCount()
                ))

            } catch (downloadError: Exception) {
                Log.e(TAG, "Download initialization also failed", downloadError)
                emit(InitializationStatus.Error("Failed to initialize: ${downloadError.message}"))
            }
        }
    }

    /**
     * Copy pre-built database from assets to app's database location
     */
    private suspend fun copyDatabaseFromAsset() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Copying database from assets...")

        // Close the current database connection
        database.close()

        // Get the destination path for the database
        val dbPath = context.getDatabasePath("kanjidic2.db")

        // Ensure parent directory exists
        dbPath.parentFile?.mkdirs()

        // Copy from assets
        context.assets.open(ASSET_DB_NAME).use { input ->
            FileOutputStream(dbPath).use { output ->
                input.copyTo(output)
            }
        }

        Log.d(TAG, "Database copied successfully to ${dbPath.absolutePath}")

        // Reopen the database to use the new file
        // Room will automatically reconnect
    }

    /**
     * Download KANJIDIC2 file from internet
     */
    private suspend fun downloadKanjidic2(): File = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(KANJIDIC2_URL)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to download: ${response.code}")
        }

        val outputFile = File(context.cacheDir, "kanjidic2.xml.gz")
        response.body?.byteStream()?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Empty response body")

        Log.d(TAG, "Downloaded KANJIDIC2 to ${outputFile.absolutePath}")
        outputFile
    }

    /**
     * Decompress GZIP file
     */
    private suspend fun decompressGzFile(gzFile: File): File = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "kanjidic2.xml")

        GZIPInputStream(FileInputStream(gzFile)).use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }

        Log.d(TAG, "Decompressed to ${outputFile.absolutePath}")
        outputFile
    }

    /**
     * Store parsed data in database
     */
    private suspend fun storeInDatabase(
        parsedData: List<Kanjidic2Parser.ParsedKanji>,
        progressCallback: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Storing ${parsedData.size} kanji in database")

        // Clear existing data
        dao.clearAllData()

        // Insert in batches for better performance
        val batchSize = 500
        parsedData.chunked(batchSize).forEachIndexed { index, batch ->
            // Insert kanji
            dao.insertKanjiBatch(batch.map { it.kanji })

            // Insert readings
            val allReadings = batch.flatMap { it.readings }
            dao.insertReadings(allReadings)

            // Insert meanings
            val allMeanings = batch.flatMap { it.meanings }
            dao.insertMeanings(allMeanings)

            val progress = ((index + 1) * batchSize * 100) / parsedData.size
            progressCallback(progress.coerceAtMost(100))

            Log.d(TAG, "Stored batch ${index + 1}, progress: $progress%")
        }

        Log.d(TAG, "Finished storing data in database")
    }

    /**
     * Query kanji by character
     */
    suspend fun getKanjiWithDetails(literal: String): KanjiWithReadingsAndMeanings? {
        return withContext(Dispatchers.IO) {
            dao.getKanjiWithDetails(literal)?.toKanjiWithReadingsAndMeanings()
        }
    }

    /**
     * Query multiple kanji characters
     */
    suspend fun getKanjiBatch(literals: List<String>): List<KanjiWithReadingsAndMeanings> {
        return withContext(Dispatchers.IO) {
            dao.getKanjiBatchWithDetails(literals)
                .map { it.toKanjiWithReadingsAndMeanings() }
        }
    }

    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(): DatabaseStats {
        return withContext(Dispatchers.IO) {
            DatabaseStats(
                kanjiCount = dao.getKanjiCount(),
                readingsCount = dao.getReadingsCount(),
                meaningsCount = dao.getMeaningsCount(),
                version = dao.getMetadataValue(VERSION_KEY)
            )
        }
    }
}

/**
 * Initialization status for dictionary setup
 */
sealed class InitializationStatus {
    data class CopyingFromAsset(val progress: Int) : InitializationStatus()
    data class Downloading(val progress: Int) : InitializationStatus()
    object Decompressing : InitializationStatus()
    data class Parsing(val processed: Int) : InitializationStatus()
    data class StoringInDatabase(val progress: Int) : InitializationStatus()
    data class Completed(
        val kanjiCount: Int,
        val readingsCount: Int,
        val meaningsCount: Int
    ) : InitializationStatus()
    data class Error(val message: String) : InitializationStatus()
}

/**
 * Database statistics
 */
data class DatabaseStats(
    val kanjiCount: Int,
    val readingsCount: Int,
    val meaningsCount: Int,
    val version: String?
)

// IOException helper
class IOException(message: String) : Exception(message)