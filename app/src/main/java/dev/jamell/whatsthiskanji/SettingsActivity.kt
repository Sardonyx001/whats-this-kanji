package dev.jamell.whatsthiskanji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jamell.whatsthiskanji.settings.UserPreferencesRepository
import dev.jamell.whatsthiskanji.ui.theme.WhatsThisKanjiTheme
import kotlinx.coroutines.launch

/**
 * Settings screen for configuring display preferences
 */
class SettingsActivity : ComponentActivity() {

    private lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesRepository = UserPreferencesRepository(this)

        setContent {
            WhatsThisKanjiTheme {
                SettingsScreen(
                    preferencesRepository = preferencesRepository,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesRepository: UserPreferencesRepository,
    onBack: () -> Unit
) {
    val preferences by preferencesRepository.userPreferencesFlow.collectAsState(
        initial = dev.jamell.whatsthiskanji.settings.UserPreferences()
    )
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Light
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, fontWeight = FontWeight.Light)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display Mode Section
            Text(
                text = "Display Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Popup toggle
                    SettingToggleRow(
                        title = "Enable Popup",
                        description = "Show small popup for short text",
                        checked = preferences.enablePopup,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                preferencesRepository.updateEnablePopup(enabled)
                            }
                        }
                    )

                    if (preferences.enablePopup) {
                        SliderSetting(
                            title = "Popup max characters",
                            value = preferences.popupMaxLength,
                            valueRange = 5f..30f,
                            onValueChange = { value ->
                                scope.launch {
                                    preferencesRepository.updatePopupMaxLength(value.toInt())
                                }
                            }
                        )
                    }

                    HorizontalDivider()

                    // Bottom sheet toggle
                    SettingToggleRow(
                        title = "Enable Bottom Sheet",
                        description = "Show bottom sheet for medium text",
                        checked = preferences.enableBottomSheet,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                preferencesRepository.updateEnableBottomSheet(enabled)
                            }
                        }
                    )

                    if (preferences.enableBottomSheet) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SettingToggleRow(
                                title = "Always use bottom sheet",
                                description = "Use bottom sheet for all text lengths",
                                checked = preferences.bottomSheetInfinity,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        preferencesRepository.updateBottomSheetInfinity(enabled)
                                    }
                                }
                            )

                            if (!preferences.bottomSheetInfinity) {
                                SliderSetting(
                                    title = "Bottom sheet max characters",
                                    value = preferences.bottomSheetMaxLength,
                                    valueRange = 20f..100f,
                                    onValueChange = { value ->
                                        scope.launch {
                                            preferencesRepository.updateBottomSheetMaxLength(value.toInt())
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Display Options Section
            Text(
                text = "Display Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingToggleRow(
                        title = "Show Romaji",
                        description = "Display romanized readings",
                        checked = preferences.showRomaji,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                preferencesRepository.updateShowRomaji(enabled)
                            }
                        }
                    )

                    HorizontalDivider()

                    SettingToggleRow(
                        title = "Auto-save Words",
                        description = "Automatically save looked-up words",
                        checked = preferences.autoSaveWords,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                preferencesRepository.updateAutoSaveWords(enabled)
                            }
                        }
                    )
                }
            }

            // Info Section
            Text(
                text = "About Display Modes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(
                        title = "Popup",
                        description = "Quick glance at short text. Dismissible with a tap."
                    )
                    InfoRow(
                        title = "Bottom Sheet",
                        description = "Medium-sized view with word details. Swipe down to dismiss."
                    )
                    InfoRow(
                        title = "Full Screen",
                        description = "Complete kanji dictionary lookup with all details."
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SliderSetting(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = (valueRange.endInclusive - valueRange.start).toInt() - 1
        )
    }
}

@Composable
fun InfoRow(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
