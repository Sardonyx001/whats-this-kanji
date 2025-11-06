# What's This Kanji

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-31%2B-brightgreen.svg)](https://android-arsenal.com/api?level=31)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Android app for Japanese text processing and kanji lookup with offline KANJIDIC2 dictionary.

## Features

- System-wide text selection support via PROCESS_TEXT intent
- Offline kanji dictionary with 13,000+ characters
- Japanese text tokenization and word segmentation
- Readings (on-yomi and kun-yomi) and English meanings
- Save words for review
- Material 3 design with light/dark theme support

## Technical Stack

- Kotlin with Jetpack Compose
- Room database for offline storage
- Kuromoji for Japanese tokenization
- KANJIDIC2 dictionary data
- Material 3 components

## Requirements

- Android 12 (API 31) or higher
- ~3MB storage for dictionary

## Usage

1. Select Japanese text in any app
2. Tap "Kanji Reading" in the text selection menu
3. View readings, meanings, and kanji details
4. Save words for later review

## Building

```bash
./gradlew assembleDebug
```

## Dictionary Data

This app uses the [KANJIDIC2](http://www.edrdg.org/wiki/index.php/KANJIDIC_Project) dictionary, developed by the Electronic Dictionary Research and Development Group (EDRDG).

## License

MIT
