#!/usr/bin/env python3

"""
Generate kanjidic2.db with correct schema

Usage: python3 generate_database.py kanjidic2.xml
"""

import sqlite3
import xml.etree.ElementTree as ET
import sys
import os

if len(sys.argv) < 2:
    print("Usage: python3 generate_database.py <kanjidic2.xml>")
    sys.exit(1)

xml_file = sys.argv[1]
if not os.path.exists(xml_file):
    print(f"Error: File not found: {xml_file}")
    sys.exit(1)

print(f"Generating kanjidic2.db from {xml_file}...")

# Create SQLite database
db_file = "kanjidic2.db"
if os.path.exists(db_file):
    os.remove(db_file)

conn = sqlite3.connect(db_file)
cursor = conn.cursor()

# Create tables with correct schema (matching KanjiEntity.kt)
cursor.execute("""
    CREATE TABLE kanji (
        literal TEXT NOT NULL PRIMARY KEY,
        grade INTEGER,
        strokeCount INTEGER,
        freq INTEGER,
        jlpt INTEGER
    )
""")

cursor.execute("CREATE INDEX index_kanji_freq ON kanji(freq ASC)")

cursor.execute("""
    CREATE TABLE readings (
        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
        literal TEXT NOT NULL,
        readingType TEXT NOT NULL,
        reading TEXT NOT NULL
    )
""")

cursor.execute("CREATE INDEX index_readings_literal ON readings(literal ASC)")

cursor.execute("""
    CREATE TABLE meanings (
        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
        literal TEXT NOT NULL,
        meaning TEXT NOT NULL
    )
""")

cursor.execute("CREATE INDEX index_meanings_literal ON meanings(literal ASC)")

cursor.execute("""
    CREATE TABLE dictionary_metadata (
        key TEXT NOT NULL PRIMARY KEY,
        value TEXT NOT NULL
    )
""")

# Create Room's internal tracking table
cursor.execute("""
    CREATE TABLE room_table_modification_log (
        table_id INTEGER PRIMARY KEY,
        invalidated INTEGER NOT NULL DEFAULT 0
    )
""")

# Insert entries for each table Room needs to track
cursor.execute("INSERT INTO room_table_modification_log VALUES (0, 0)")  # kanji
cursor.execute("INSERT INTO room_table_modification_log VALUES (1, 0)")  # readings
cursor.execute("INSERT INTO room_table_modification_log VALUES (2, 0)")  # meanings
cursor.execute("INSERT INTO room_table_modification_log VALUES (3, 0)")  # dictionary_metadata
cursor.execute("INSERT INTO room_table_modification_log VALUES (4, 0)")  # saved_words

# Create Room master table
cursor.execute("""
    CREATE TABLE room_master_table (
        id INTEGER PRIMARY KEY,
        identity_hash TEXT
    )
""")

# Insert Room identity hash (this should match your database version hash)
# For now, we'll leave it empty - Room will handle it
cursor.execute("INSERT INTO room_master_table VALUES (42, 'PLACEHOLDER_HASH')")

print("✓ Tables created")

# Parse XML and insert data
print("Parsing XML...")
tree = ET.parse(xml_file)
root = tree.getroot()

count = 0
for character in root.findall('character'):
    # Extract kanji data
    literal = character.find('literal')
    if literal is None:
        continue

    literal_text = literal.text

    # Extract misc data
    misc = character.find('misc')
    grade = None
    stroke_count = None
    freq = None
    jlpt = None

    if misc is not None:
        grade_elem = misc.find('grade')
        if grade_elem is not None:
            grade = int(grade_elem.text)

        stroke_elem = misc.find('stroke_count')
        if stroke_elem is not None:
            stroke_count = int(stroke_elem.text)

        freq_elem = misc.find('freq')
        if freq_elem is not None:
            freq = int(freq_elem.text)

        jlpt_elem = misc.find('jlpt')
        if jlpt_elem is not None:
            jlpt = int(jlpt_elem.text)

    # Insert kanji
    cursor.execute(
        "INSERT INTO kanji (literal, grade, strokeCount, freq, jlpt) VALUES (?, ?, ?, ?, ?)",
        (literal_text, grade, stroke_count, freq, jlpt)
    )

    # Extract readings and meanings
    reading_meaning = character.find('reading_meaning')
    if reading_meaning is not None:
        rmgroup = reading_meaning.find('rmgroup')
        if rmgroup is not None:
            # Extract readings
            for reading_elem in rmgroup.findall('reading'):
                r_type = reading_elem.get('r_type')
                if r_type in ('ja_on', 'ja_kun'):
                    cursor.execute(
                        "INSERT INTO readings (literal, readingType, reading) VALUES (?, ?, ?)",
                        (literal_text, r_type, reading_elem.text)
                    )

            # Extract meanings (only English, no m_lang attribute)
            for meaning_elem in rmgroup.findall('meaning'):
                if meaning_elem.get('m_lang') is None:
                    cursor.execute(
                        "INSERT INTO meanings (literal, meaning) VALUES (?, ?)",
                        (literal_text, meaning_elem.text)
                    )

    count += 1
    if count % 100 == 0:
        conn.commit()
        print(f"\rProcessed: {count} kanji", end='', flush=True)

conn.commit()
print(f"\n✓ Processed {count} kanji total")

# Add metadata
cursor.execute("INSERT INTO dictionary_metadata VALUES ('kanjidic2_version', '2025-01')")
cursor.execute("INSERT INTO dictionary_metadata VALUES ('dictionary_initialized', 'true')")
conn.commit()

print("✓ Metadata added")

# Verify schema
print("\n=== Verifying Schema ===")
cursor.execute("PRAGMA table_info(kanji)")
for row in cursor.fetchall():
    cid, name, type_, notnull, dflt_value, pk = row
    print(f"  {name}: {type_}, notNull={notnull}, pk={pk}")

# Get stats
cursor.execute("SELECT COUNT(*) FROM kanji")
kanji_count = cursor.fetchone()[0]
cursor.execute("SELECT COUNT(*) FROM readings")
readings_count = cursor.fetchone()[0]
cursor.execute("SELECT COUNT(*) FROM meanings")
meanings_count = cursor.fetchone()[0]

print(f"\n=== Statistics ===")
print(f"  Kanji: {kanji_count}")
print(f"  Readings: {readings_count}")
print(f"  Meanings: {meanings_count}")

conn.close()

db_size_mb = os.path.getsize(db_file) / (1024 * 1024)
print(f"\n✓ Database generated: {db_file}")
print(f"  Size: {db_size_mb:.2f} MB")
print(f"\n✓ Done! Copy this file to app/src/main/assets/kanjidic2.db")