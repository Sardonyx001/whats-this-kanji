package dev.jamell.whatsthiskanji

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jamell.whatsthiskanji.ui.theme.WhatsThisKanjiTheme

/**
 * Main launcher activity - simple UI with no automatic initialization
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WhatsThisKanjiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SimpleMainScreen(
                        onInitialize = {
                            startActivity(Intent(this, InitializationActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleMainScreen(onInitialize: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title - minimalist
        Text(
            text = "漢字",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "What's This Kanji",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 64.dp)
        )

        // Saved Words Button - minimal style
        OutlinedButton(
            onClick = {
                val intent = Intent(context, SavedWordsActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "Saved Words",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dictionary Setup Button
        OutlinedButton(
            onClick = onInitialize,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "Dictionary",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Usage hint - subtle
        Text(
            text = "Select Japanese text in any app\nto view readings and meanings",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            lineHeight = 20.sp
        )
    }
}

// Previews
@Preview(showBackground = true)
@Composable
fun SimpleMainScreenPreview() {
    WhatsThisKanjiTheme {
        SimpleMainScreen(onInitialize = {})
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SimpleMainScreenDarkPreview() {
    WhatsThisKanjiTheme {
        SimpleMainScreen(onInitialize = {})
    }
}