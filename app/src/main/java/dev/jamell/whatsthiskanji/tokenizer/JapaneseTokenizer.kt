package dev.jamell.whatsthiskanji.tokenizer

import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer

/**
 * Wrapper for Kuromoji Japanese tokenizer
 *
 * Provides word-level tokenization and reading extraction for Japanese text
 */
class JapaneseTokenizer {

    private val tokenizer: Tokenizer by lazy {
        Tokenizer()
    }

    /**
     * Tokenize Japanese text into words
     *
     * @param text Input Japanese text
     * @return List of tokenized words with readings in hiragana
     */
    fun tokenize(text: String): List<TokenizedWord> {
        if (text.isBlank()) return emptyList()

        val tokens = tokenizer.tokenize(text)
        return tokens.mapNotNull { token ->
            val reading = token.reading ?: token.surface
            TokenizedWord(
                surface = token.surface,
                reading = katakanaToHiragana(reading),
                baseForm = token.baseForm ?: token.surface,
                partOfSpeech = token.partOfSpeechLevel1,
                isKnown = token.isKnown
            )
        }
    }

    /**
     * Convert katakana to hiragana
     *
     * @param text Input text (may contain katakana)
     * @return Text with katakana converted to hiragana
     */
    fun katakanaToHiragana(text: String): String {
        return text.map { char ->
            when (char) {
                in '\u30A1'..'\u30F6' -> (char.code - 0x60).toChar() // Katakana to Hiragana
                else -> char
            }
        }.joinToString("")
    }

    /**
     * Extract unique kanji characters from text
     *
     * @param text Input text
     * @return Set of unique kanji characters
     */
    fun extractKanji(text: String): Set<String> {
        val kanjiRange = '\u4E00'..'\u9FFF'
        return text.filter { it in kanjiRange }
            .map { it.toString() }
            .toSet()
    }

    /**
     * Check if a character is a kanji
     */
    fun isKanji(char: Char): Boolean {
        val kanjiRange = '\u4E00'..'\u9FFF'
        return char in kanjiRange
    }

    /**
     * Get reading for a specific word
     *
     * @param word The word to get reading for
     * @return Reading in hiragana, or the original word if not found
     */
    fun getReading(word: String): String {
        val tokens = tokenizer.tokenize(word)
        return if (tokens.isNotEmpty()) {
            val reading = tokens.first().reading ?: word
            katakanaToHiragana(reading)
        } else {
            word
        }
    }
}

/**
 * Data class representing a tokenized word
 */
data class TokenizedWord(
    val surface: String,           // The actual word as it appears in text
    val reading: String,           // The reading in kana
    val baseForm: String,          // Dictionary/base form of the word
    val partOfSpeech: String,      // Part of speech
    val isKnown: Boolean           // Whether word is in dictionary
)