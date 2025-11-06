# Pre-built Database Setup Guide

This guide explains how to ship the `kanjidic2.db` pre-built database with your app to avoid slow initialization on first launch.

## Option 1: Ship Database in Assets (Recommended)

### Step 1: Build the Database
1. Run the initialization process once to create a complete database
2. The database will be created at: `/data/data/com.example.whatsthiskanji/databases/kanjidic2.db`
3. Use `adb pull` to extract it:
   ```bash
   adb pull /data/data/com.example.whatsthiskanji/databases/kanjidic2.db ./kanjidic2.db
   ```

### Step 2: Add Database to Assets
1. Create the assets folder if it doesn't exist:
   ```bash
   mkdir -p app/src/main/assets/databases
   ```

2. Copy the database file:
   ```bash
   cp kanjidic2.db app/src/main/assets/databases/kanjidic2.db
   ```

### Step 3: Update Database Initialization Code

Modify `KanjiDatabase.kt` to copy from assets if database doesn't exist:

```kotlin
companion object {
    private const val DATABASE_NAME = "kanjidic2.db"

    @Volatile
    private var INSTANCE: KanjiDatabase? = null

    fun getInstance(context: Context): KanjiDatabase {
        return INSTANCE ?: synchronized(this) {
            // Copy database from assets if it doesn't exist
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) {
                copyDatabaseFromAssets(context)
            }

            val instance = Room.databaseBuilder(
                context.applicationContext,
                KanjiDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }

    private fun copyDatabaseFromAssets(context: Context) {
        try {
            val dbPath = context.getDatabasePath(DATABASE_NAME)
            dbPath.parentFile?.mkdirs()

            context.assets.open("databases/$DATABASE_NAME").use { inputStream ->
                dbPath.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Database will be created normally if copy fails
        }
    }
}
```

### Step 4: Remove InitializationActivity Logic

Since the database is pre-bundled, you can simplify the initialization:

1. Remove or simplify `InitializationActivity.kt`
2. Remove download/parsing logic from `DictionaryRepository.kt`
3. The app will now load instantly on first launch!

## Option 2: Download on Demand (Current Implementation)

Keep the current implementation where:
- Database is downloaded/built on first launch
- User manually triggers initialization
- Better for reducing APK size but slower first launch

## Database Size Considerations

- Uncompressed DB: ~10-30 MB (depends on KANJIDIC2 data)
- Compressed in APK: ~3-8 MB (Android compresses assets automatically)
- Consider Option 2 if APK size is critical

## Testing Pre-built Database

1. Uninstall the app completely
2. Install the new version with bundled database
3. Launch app - it should work immediately
4. Verify kanji lookups work without initialization

## Updating the Database

When you need to update the dictionary:

1. Build new database with updated KANJIDIC2 data
2. Replace `app/src/main/assets/databases/kanjidic2.db`
3. Increment database version in `KanjiDatabase.kt`:
   ```kotlin
   @Database(..., version = 3)  // Increment this
   ```
4. Users will get the new database on app update

## Migration Strategy

If database schema changes:

```kotlin
.addMigrations(MIGRATION_2_3)  // Add migration if needed
```

For major changes, use `.fallbackToDestructiveMigration()` (already in place).
