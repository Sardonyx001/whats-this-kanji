package dev.jamell.whatsthiskanji.data

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

/**
 * Parser for KANJIDIC2 XML format
 *
 * Parses the KANJIDIC2 XML file and extracts kanji, readings, and meanings
 */
class Kanjidic2Parser {

    data class ParsedKanji(
        val kanji: KanjiEntity,
        val readings: List<ReadingEntity>,
        val meanings: List<MeaningEntity>
    )

    /**
     * Parse KANJIDIC2 XML from an InputStream
     *
     * @param inputStream The input stream containing KANJIDIC2 XML data
     * @param progressCallback Callback to report progress (processed count)
     * @return List of parsed kanji with their readings and meanings
     */
    suspend fun parse(
        inputStream: InputStream,
        progressCallback: ((Int) -> Unit)? = null
    ): List<ParsedKanji> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ParsedKanji>()
        var processedCount = 0

        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "character") {
                    val parsedKanji = parseCharacter(parser)
                    if (parsedKanji != null) {
                        result.add(parsedKanji)
                        processedCount++

                        // Report progress every 100 kanji
                        if (processedCount % 100 == 0) {
                            progressCallback?.invoke(processedCount)
                        }
                    }
                }
                eventType = parser.next()
            }

            // Final progress update
            progressCallback?.invoke(processedCount)

        } catch (e: XmlPullParserException) {
            throw IOException("Error parsing KANJIDIC2 XML", e)
        } finally {
            inputStream.close()
        }

        result
    }

    /**
     * Parse a single <character> element
     */
    private fun parseCharacter(parser: XmlPullParser): ParsedKanji? {
        var literal: String? = null
        var grade: Int? = null
        var strokeCount: Int? = null
        var freq: Int? = null
        var jlpt: Int? = null
        val readings = mutableListOf<ReadingEntity>()
        val meanings = mutableListOf<MeaningEntity>()

        try {
            var eventType = parser.eventType
            var depth = 1 // We're already inside <character>

            while (depth > 0) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        depth++
                        when (parser.name) {
                            "literal" -> {
                                literal = readText(parser)
                            }
                            "misc" -> {
                                parseMisc(parser, onGrade = { grade = it },
                                         onStrokeCount = { strokeCount = it },
                                         onFreq = { freq = it },
                                         onJlpt = { jlpt = it })
                                depth-- // parseMisc consumes its closing tag
                            }
                            "reading_meaning" -> {
                                parseReadingMeaning(parser, literal, readings, meanings)
                                depth-- // parseReadingMeaning consumes its closing tag
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        depth--
                    }
                }
                eventType = parser.next()
            }

            return if (literal != null) {
                ParsedKanji(
                    kanji = KanjiEntity(literal, grade, strokeCount, freq, jlpt),
                    readings = readings,
                    meanings = meanings
                )
            } else {
                null
            }

        } catch (e: Exception) {
            // Skip malformed entries
            return null
        }
    }

    /**
     * Parse <misc> element
     */
    private fun parseMisc(
        parser: XmlPullParser,
        onGrade: (Int) -> Unit,
        onStrokeCount: (Int) -> Unit,
        onFreq: (Int) -> Unit,
        onJlpt: (Int) -> Unit
    ) {
        var depth = 1
        var eventType = parser.next()

        while (depth > 0) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "grade" -> readText(parser).toIntOrNull()?.let(onGrade)
                        "stroke_count" -> {
                            // Take first stroke count (most accepted)
                            val text = readText(parser)
                            if (text.isNotEmpty()) {
                                text.toIntOrNull()?.let(onStrokeCount)
                            }
                        }
                        "freq" -> readText(parser).toIntOrNull()?.let(onFreq)
                        "jlpt" -> readText(parser).toIntOrNull()?.let(onJlpt)
                    }
                }
                XmlPullParser.END_TAG -> {
                    depth--
                }
            }
            if (depth > 0) {
                eventType = parser.next()
            }
        }
    }

    /**
     * Parse <reading_meaning> element
     */
    private fun parseReadingMeaning(
        parser: XmlPullParser,
        literal: String?,
        readings: MutableList<ReadingEntity>,
        meanings: MutableList<MeaningEntity>
    ) {
        var depth = 1
        var eventType = parser.next()

        while (depth > 0) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "reading" -> {
                            val rType = parser.getAttributeValue(null, "r_type")
                            val reading = readText(parser)

                            if (literal != null && (rType == "ja_on" || rType == "ja_kun") && reading.isNotEmpty()) {
                                readings.add(ReadingEntity(
                                    literal = literal,
                                    readingType = rType,
                                    reading = reading
                                ))
                            }
                        }
                        "meaning" -> {
                            // Only process English meanings (no m_lang attribute)
                            val mLang = parser.getAttributeValue(null, "m_lang")
                            if (mLang == null) {
                                val meaning = readText(parser)
                                if (literal != null && meaning.isNotEmpty()) {
                                    meanings.add(MeaningEntity(
                                        literal = literal,
                                        meaning = meaning
                                    ))
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    depth--
                }
            }
            if (depth > 0) {
                eventType = parser.next()
            }
        }
    }

    /**
     * Read text content from current element
     */
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text ?: ""
            parser.nextTag()
        }
        return result
    }
}