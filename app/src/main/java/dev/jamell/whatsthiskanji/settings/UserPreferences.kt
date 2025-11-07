package dev.jamell.whatsthiskanji.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Display mode for text processing
 */
enum class DisplayMode {
    POPUP,          // Floating popup window
    BOTTOM_SHEET,   // Bottom sheet dialog
    FULL_SCREEN     // Full activity (original behavior)
}

/**
 * User preferences repository
 */
class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ENABLE_POPUP = booleanPreferencesKey("enable_popup")
        val ENABLE_BOTTOM_SHEET = booleanPreferencesKey("enable_bottom_sheet")
        val POPUP_MAX_LENGTH = intPreferencesKey("popup_max_length")
        val BOTTOM_SHEET_MAX_LENGTH = intPreferencesKey("bottom_sheet_max_length")
        val BOTTOM_SHEET_INFINITY = booleanPreferencesKey("bottom_sheet_infinity")
        val AUTO_SAVE_WORDS = booleanPreferencesKey("auto_save_words")
        val SHOW_ROMAJI = booleanPreferencesKey("show_romaji")
    }

    // Flow of user preferences
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                enablePopup = preferences[PreferencesKeys.ENABLE_POPUP] ?: true,
                enableBottomSheet = preferences[PreferencesKeys.ENABLE_BOTTOM_SHEET] ?: true,
                popupMaxLength = preferences[PreferencesKeys.POPUP_MAX_LENGTH] ?: 10,
                bottomSheetMaxLength = preferences[PreferencesKeys.BOTTOM_SHEET_MAX_LENGTH] ?: 50,
                bottomSheetInfinity = preferences[PreferencesKeys.BOTTOM_SHEET_INFINITY] ?: false,
                autoSaveWords = preferences[PreferencesKeys.AUTO_SAVE_WORDS] ?: false,
                showRomaji = preferences[PreferencesKeys.SHOW_ROMAJI] ?: false
            )
        }

    suspend fun updateEnablePopup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_POPUP] = enabled
        }
    }

    suspend fun updateEnableBottomSheet(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_BOTTOM_SHEET] = enabled
        }
    }

    suspend fun updatePopupMaxLength(length: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POPUP_MAX_LENGTH] = length
        }
    }

    suspend fun updateBottomSheetMaxLength(length: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BOTTOM_SHEET_MAX_LENGTH] = length
        }
    }

    suspend fun updateBottomSheetInfinity(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BOTTOM_SHEET_INFINITY] = enabled
        }
    }

    suspend fun updateAutoSaveWords(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SAVE_WORDS] = enabled
        }
    }

    suspend fun updateShowRomaji(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ROMAJI] = enabled
        }
    }

    /**
     * Determine display mode based on text length and user preferences
     */
    fun determineDisplayMode(textLength: Int, preferences: UserPreferences): DisplayMode {
        return when {
            preferences.enablePopup && textLength <= preferences.popupMaxLength -> DisplayMode.POPUP
            preferences.enableBottomSheet && (preferences.bottomSheetInfinity || textLength <= preferences.bottomSheetMaxLength) -> DisplayMode.BOTTOM_SHEET
            else -> DisplayMode.FULL_SCREEN
        }
    }
}

/**
 * User preferences data class
 */
data class UserPreferences(
    val enablePopup: Boolean = true,
    val enableBottomSheet: Boolean = true,
    val popupMaxLength: Int = 10,
    val bottomSheetMaxLength: Int = 50,
    val bottomSheetInfinity: Boolean = false,
    val autoSaveWords: Boolean = false,
    val showRomaji: Boolean = false
)