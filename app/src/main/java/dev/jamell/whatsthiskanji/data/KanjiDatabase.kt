package dev.jamell.whatsthiskanji.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for storing KANJIDIC2 data
 */
@Database(
    entities = [
        KanjiEntity::class,
        ReadingEntity::class,
        MeaningEntity::class,
        DictionaryMetadata::class,
        SavedWordEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class KanjiDatabase : RoomDatabase() {
    abstract fun kanjiDao(): KanjiDao

    companion object {
        private const val DATABASE_NAME = "kanjidic2.db"

        @Volatile
        private var INSTANCE: KanjiDatabase? = null

        fun getInstance(context: Context): KanjiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KanjiDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Reset the database instance (useful after copying from assets)
         */
        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}