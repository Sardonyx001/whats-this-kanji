package dev.jamell.whatsthiskanji

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.jamell.whatsthiskanji.settings.DisplayMode
import dev.jamell.whatsthiskanji.settings.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Router activity that determines which display mode to use
 * based on text length and user preferences
 */
class TextProcessRouter : ComponentActivity() {

    private lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesRepository = UserPreferencesRepository(this)

        // Get the selected text from the intent
        val selectedText = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
            }
            else -> ""
        }

        if (selectedText.isEmpty()) {
            finish()
            return
        }

        // Determine display mode and route to appropriate activity
        lifecycleScope.launch {
            val preferences = preferencesRepository.userPreferencesFlow.first()
            val displayMode = preferencesRepository.determineDisplayMode(
                selectedText.length,
                preferences
            )

            val targetActivity = when (displayMode) {
                DisplayMode.POPUP -> PopupTextActivity::class.java
                DisplayMode.BOTTOM_SHEET -> BottomSheetTextActivity::class.java
                DisplayMode.FULL_SCREEN -> ProcessTextActivity::class.java
            }

            val routerIntent = Intent(this@TextProcessRouter, targetActivity).apply {
                action = Intent.ACTION_PROCESS_TEXT
                putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText)
            }

            startActivity(routerIntent)
            finish()
        }
    }
}
