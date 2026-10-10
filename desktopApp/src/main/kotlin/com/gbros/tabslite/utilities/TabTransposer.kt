package com.gbros.tabslite.utilities

data class ChordShape(
    val chordName: String,
    val instrument: String,
    val frets: List<Int>, // -1 = X (muted), 0 = O (open), 1..24 = fret number
    val fingers: List<Int> = emptyList(), // 0 = none/open/mute, 1=Index, 2=Middle, 3=Ring, 4=Pinky, 5=Thumb
    val baseFret: Int = 1,
    val barres: List<Int> = emptyList(),
    val voicingName: String = "Standard Voicing"
)

object TabTransposer {
    val NOTES_SHARP = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val NOTES_FLAT  = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    fun extractChords(content: String): List<String> {
        val regex = Regex("\\[ch\\](.*?)\\[/ch\\]")
        return regex.findAll(content).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.distinct().toList()
    }

    fun transposeContent(content: String, halfSteps: Int): String {
        if (halfSteps == 0) return content
        val regex = Regex("\\[ch\\](.*?)\\[/ch\\]")
        return regex.replace(content) { matchResult ->
            val chord = matchResult.groupValues[1]
            val transposed = transposeChord(chord, halfSteps)
            "[ch]$transposed[/ch]"
        }
    }

    fun transposeChord(chord: String, halfSteps: Int): String {
        val parts = chord.split("/")
        return parts.joinToString("/") { part ->
            transposeSingleChord(part.trim(), halfSteps)
        }
    }

    private fun transposeSingleChord(chord: String, halfSteps: Int): String {
        if (chord.isEmpty()) return chord

        val rootNote = extractRootNote(chord)
        val suffix = chord.substring(rootNote.length)

        var index = NOTES_SHARP.indexOf(rootNote)
        if (index == -1) {
            index = NOTES_FLAT.indexOf(rootNote)
        }
        if (index == -1) return chord

        var newIndex = (index + halfSteps) % 12
        if (newIndex < 0) newIndex += 12

        val useFlats = rootNote.contains("b") || (halfSteps < 0 && !rootNote.contains("#"))
        val newNote = if (useFlats) NOTES_FLAT[newIndex] else NOTES_SHARP[newIndex]
        return "$newNote$suffix"
    }

    fun extractRootNote(chord: String): String {
        val clean = chord.trim()
        return when {
            clean.length >= 2 && (clean[1] == '#' || clean[1] == 'b') -> clean.substring(0, 2)
            clean.isNotEmpty() -> clean.substring(0, 1)
            else -> ""
        }
    }

    fun getChordNotes(chord: String): List<String> {
        val clean = chord.split("/")[0].trim()
        val root = extractRootNote(clean)
        if (root.isEmpty()) return emptyList()

        var rootIndex = NOTES_SHARP.indexOf(root)
        val useFlats = root.contains("b")
        if (rootIndex == -1) {
            rootIndex = NOTES_FLAT.indexOf(root)
        }
        if (rootIndex == -1) return listOf(root)

        val suffix = clean.substring(root.length).lowercase()

        val intervals = when {
            suffix.startsWith("maj7") || suffix.startsWith("m7+") -> listOf(0, 4, 7, 11)
            suffix.startsWith("m7b5") || suffix.startsWith("ø") -> listOf(0, 3, 6, 10)
            suffix.startsWith("m7") || suffix.startsWith("min7") || suffix.startsWith("-7") -> listOf(0, 3, 7, 10)
            suffix.startsWith("7b5") -> listOf(0, 4, 6, 10)
            suffix.startsWith("7#9") -> listOf(0, 4, 7, 10, 3)
            suffix.startsWith("7") || suffix.startsWith("dom7") -> listOf(0, 4, 7, 10)
            suffix.startsWith("m6") || suffix.startsWith("min6") -> listOf(0, 3, 7, 9)
            suffix.startsWith("6") || suffix.startsWith("add6") -> listOf(0, 4, 7, 9)
            suffix.startsWith("m9") || suffix.startsWith("min9") -> listOf(0, 3, 7, 10, 2)
            suffix.startsWith("maj9") -> listOf(0, 4, 7, 11, 2)
            suffix.startsWith("9") -> listOf(0, 4, 7, 10, 2)
            suffix.startsWith("m11") -> listOf(0, 3, 7, 10, 2, 5)
            suffix.startsWith("11") -> listOf(0, 4, 7, 10, 2, 5)
            suffix.startsWith("m13") -> listOf(0, 3, 7, 10, 2, 9)
            suffix.startsWith("13") -> listOf(0, 4, 7, 10, 2, 9)
            suffix.startsWith("add9") || suffix.startsWith("add2") -> listOf(0, 4, 7, 2)
            suffix.startsWith("madd9") -> listOf(0, 3, 7, 2)
            suffix.startsWith("sus4") || suffix.startsWith("sus") -> listOf(0, 5, 7)
            suffix.startsWith("sus2") -> listOf(0, 2, 7)
            suffix.startsWith("7sus4") -> listOf(0, 5, 7, 10)
            suffix.startsWith("dim7") || suffix.startsWith("o7") -> listOf(0, 3, 6, 9)
            suffix.startsWith("dim") || suffix.startsWith("o") -> listOf(0, 3, 6)
            suffix.startsWith("aug") || suffix.startsWith("+") -> listOf(0, 4, 8)
            suffix.startsWith("5") -> listOf(0, 7)
            suffix.startsWith("m") || suffix.startsWith("min") || suffix.startsWith("-") -> listOf(0, 3, 7)
            else -> listOf(0, 4, 7)
        }

        val noteList = if (useFlats) NOTES_FLAT else NOTES_SHARP
        return intervals.map { interval ->
            val noteIdx = (rootIndex + interval) % 12
            noteList[noteIdx]
        }.distinct()
    }

    /**
     * Returns multiple Guitar shapes / voicings across the neck for any given chord.
     */
    fun getGuitarChordShapes(chord: String): List<ChordShape> {
        val clean = chord.split("/")[0].trim()
        val root = extractRootNote(clean)
        if (root.isEmpty()) return listOf(ChordShape(chord, "Guitar", listOf(-1, 0, 2, 2, 1, 0), listOf(0, 0, 2, 3, 1, 0), 1))

        var rootIdx = NOTES_SHARP.indexOf(root)
        if (rootIdx == -1) rootIdx = NOTES_FLAT.indexOf(root)
        if (rootIdx == -1) rootIdx = 0

        val suffix = clean.substring(root.length).trim()

        val shapes = mutableListOf<ChordShape>()

        // Check specific hand-crafted popular chord voicings
        val specific = getSpecificGuitarShapes(clean)
        if (specific.isNotEmpty()) {
            return specific
        }

        // Generic CAGED Chord Shape Generator
        val isMinor = suffix.startsWith("m") && !suffix.startsWith("maj")
        val is7 = suffix == "7" || suffix == "dom7"
        val isMaj7 = suffix.startsWith("maj7")
        val isM7 = suffix.startsWith("m7")
        val isSus4 = suffix.startsWith("sus4")
        val isSus2 = suffix.startsWith("sus2")

        // 1. E-shape movable barre
        val eShift = (rootIdx - 4 + 12) % 12
        val eFret = if (eShift == 0) 1 else eShift
        if (isMinor) {
            val f = if (eShift == 0) listOf(0, 2, 2, 0, 0, 0) else listOf(eFret, eFret + 2, eFret + 2, eFret, eFret, eFret)
            val fg = if (eShift == 0) listOf(0, 2, 3, 0, 0, 0) else listOf(1, 3, 4, 1, 1, 1)
            shapes.add(ChordShape(clean, "Guitar", f, fg, baseFret = eFret.coerceAtLeast(1), barres = if (eShift > 0) listOf(eFret) else emptyList(), voicingName = "E-Shape (Fret $eFret)"))
        } else if (isMaj7) {
            val f = if (eShift == 0) listOf(0, 2, 1, 1, 0, 0) else listOf(eFret, -1, eFret + 1, eFret + 1, eFret - 1, -1)
            shapes.add(ChordShape(clean, "Guitar", f, listOf(1, 0, 3, 4, 2, 0), baseFret = eFret.coerceAtLeast(1), voicingName = "E-Shape Maj7"))
        } else if (is7) {
            val f = if (eShift == 0) listOf(0, 2, 0, 1, 0, 0) else listOf(eFret, eFret + 2, eFret, eFret + 1, eFret, eFret)
            shapes.add(ChordShape(clean, "Guitar", f, listOf(1, 3, 1, 2, 1, 1), baseFret = eFret.coerceAtLeast(1), barres = if (eShift > 0) listOf(eFret) else emptyList(), voicingName = "E7-Shape"))
        } else {
            val f = if (eShift == 0) listOf(0, 2, 2, 1, 0, 0) else listOf(eFret, eFret + 2, eFret + 2, eFret + 1, eFret, eFret)
            val fg = if (eShift == 0) listOf(0, 2, 3, 1, 0, 0) else listOf(1, 3, 4, 2, 1, 1)
            shapes.add(ChordShape(clean, "Guitar", f, fg, baseFret = eFret.coerceAtLeast(1), barres = if (eShift > 0) listOf(eFret) else emptyList(), voicingName = "E-Shape Barre (Fret $eFret)"))
        }

        // 2. A-shape movable barre
        val aShift = (rootIdx - 9 + 12) % 12
        val aFret = if (aShift == 0) 1 else aShift
        if (isMinor) {
            val f = if (aShift == 0) listOf(-1, 0, 2, 2, 1, 0) else listOf(-1, aFret, aFret + 2, aFret + 2, aFret + 1, aFret)
            val fg = if (aShift == 0) listOf(0, 0, 2, 3, 1, 0) else listOf(0, 1, 3, 4, 2, 1)
            shapes.add(ChordShape(clean, "Guitar", f, fg, baseFret = aFret.coerceAtLeast(1), barres = if (aShift > 0) listOf(aFret) else emptyList(), voicingName = "A-Shape Minor (Fret $aFret)"))
        } else if (is7) {
            val f = if (aShift == 0) listOf(-1, 0, 2, 0, 2, 0) else listOf(-1, aFret, aFret + 2, aFret, aFret + 2, aFret)
            shapes.add(ChordShape(clean, "Guitar", f, listOf(0, 1, 3, 1, 4, 1), baseFret = aFret.coerceAtLeast(1), barres = if (aShift > 0) listOf(aFret) else emptyList(), voicingName = "A7-Shape"))
        } else {
            val f = if (aShift == 0) listOf(-1, 0, 2, 2, 2, 0) else listOf(-1, aFret, aFret + 2, aFret + 2, aFret + 2, aFret)
            val fg = if (aShift == 0) listOf(0, 0, 1, 2, 3, 0) else listOf(0, 1, 2, 3, 4, 1)
            shapes.add(ChordShape(clean, "Guitar", f, fg, baseFret = aFret.coerceAtLeast(1), barres = if (aShift > 0) listOf(aFret) else emptyList(), voicingName = "A-Shape Barre (Fret $aFret)"))
        }

        // 3. D-shape movable
        val dShift = (rootIdx - 2 + 12) % 12
        if (dShift > 0) {
            val f = listOf(-1, -1, 0 + dShift, 2 + dShift, 3 + dShift, 2 + dShift)
            shapes.add(ChordShape(clean, "Guitar", f, listOf(0, 0, 0, 1, 3, 2), baseFret = dShift.coerceAtLeast(1), voicingName = "D-Shape (Fret $dShift)"))
        }

        return if (shapes.isNotEmpty()) shapes else listOf(ChordShape(clean, "Guitar", listOf(-1, 0, 2, 2, 1, 0), listOf(0, 0, 2, 3, 1, 0), 1))
    }

    private fun getSpecificGuitarShapes(chord: String): List<ChordShape> {
        return when (chord) {
            "A#", "Bb" -> listOf(
                ChordShape(chord, "Guitar", listOf(6, 8, 8, 7, 6, 6), listOf(1, 3, 4, 2, 1, 1), baseFret = 6, barres = listOf(6), voicingName = "6th Fret Barre (E-Shape)"),
                ChordShape(chord, "Guitar", listOf(-1, 1, 3, 3, 3, 1), listOf(0, 1, 2, 3, 4, 1), baseFret = 1, barres = listOf(1), voicingName = "1st Fret Barre (A-Shape)")
            )
            "D#", "Eb" -> listOf(
                ChordShape(chord, "Guitar", listOf(11, 13, 13, 12, 11, 11), listOf(1, 3, 4, 2, 1, 1), baseFret = 11, barres = listOf(11), voicingName = "11th Fret Barre (E-Shape)"),
                ChordShape(chord, "Guitar", listOf(-1, 6, 8, 8, 8, 6), listOf(0, 1, 2, 3, 4, 1), baseFret = 6, barres = listOf(6), voicingName = "6th Fret Barre (A-Shape)"),
                ChordShape(chord, "Guitar", listOf(-1, -1, 1, 3, 4, 3), listOf(0, 0, 1, 3, 4, 2), baseFret = 1, voicingName = "1st Fret Shape")
            )
            "Dm" -> listOf(
                ChordShape("Dm", "Guitar", listOf(-1, -1, 0, 2, 3, 1), listOf(0, 0, 0, 2, 3, 1), baseFret = 1, voicingName = "Open Position"),
                ChordShape("Dm", "Guitar", listOf(-1, 5, 7, 7, 6, 5), listOf(0, 1, 3, 4, 2, 1), baseFret = 5, barres = listOf(5), voicingName = "5th Fret Barre")
            )
            "Ab", "G#" -> listOf(
                ChordShape(chord, "Guitar", listOf(4, 6, 6, 5, 4, 4), listOf(1, 3, 4, 2, 1, 1), baseFret = 4, barres = listOf(4), voicingName = "4th Fret Barre (E-Shape)"),
                ChordShape(chord, "Guitar", listOf(-1, 11, 13, 13, 13, 11), listOf(0, 1, 2, 3, 4, 1), baseFret = 11, barres = listOf(11), voicingName = "11th Fret Barre")
            )
            "Bbm", "A#m" -> listOf(
                ChordShape(chord, "Guitar", listOf(-1, 1, 3, 3, 2, 1), listOf(0, 1, 3, 4, 2, 1), baseFret = 1, barres = listOf(1), voicingName = "1st Fret Barre (A-Shape)"),
                ChordShape(chord, "Guitar", listOf(6, 8, 8, 6, 6, 6), listOf(1, 3, 4, 1, 1, 1), baseFret = 6, barres = listOf(6), voicingName = "6th Fret Barre")
            )
            "Db", "C#" -> listOf(
                ChordShape(chord, "Guitar", listOf(-1, 4, 6, 6, 6, 4), listOf(0, 1, 2, 3, 4, 1), baseFret = 4, barres = listOf(4), voicingName = "4th Fret Barre (A-Shape)"),
                ChordShape(chord, "Guitar", listOf(9, 11, 11, 10, 9, 9), listOf(1, 3, 4, 2, 1, 1), baseFret = 9, barres = listOf(9), voicingName = "9th Fret Barre")
            )
            "Ebm7", "D#m7" -> listOf(
                ChordShape(chord, "Guitar", listOf(-1, 6, 8, 6, 7, 6), listOf(0, 1, 3, 1, 2, 1), baseFret = 6, barres = listOf(6), voicingName = "6th Fret Barre"),
                ChordShape(chord, "Guitar", listOf(11, 13, 11, 11, 11, 11), listOf(1, 3, 1, 1, 1, 1), baseFret = 11, barres = listOf(11), voicingName = "11th Fret Barre")
            )
            "A" -> listOf(
                ChordShape("A", "Guitar", listOf(-1, 0, 2, 2, 2, 0), listOf(0, 0, 1, 2, 3, 0), baseFret = 1, voicingName = "Open Position"),
                ChordShape("A", "Guitar", listOf(5, 7, 7, 6, 5, 5), listOf(1, 3, 4, 2, 1, 1), baseFret = 5, barres = listOf(5), voicingName = "5th Fret Barre")
            )
            "E" -> listOf(
                ChordShape("E", "Guitar", listOf(0, 2, 2, 1, 0, 0), listOf(0, 2, 3, 1, 0, 0), baseFret = 1, voicingName = "Open Position"),
                ChordShape("E", "Guitar", listOf(-1, 7, 9, 9, 9, 7), listOf(0, 1, 2, 3, 4, 1), baseFret = 7, barres = listOf(7), voicingName = "7th Fret Barre")
            )
            "Gmaj7" -> listOf(
                ChordShape("Gmaj7", "Guitar", listOf(3, 2, 0, 0, 0, 2), listOf(3, 1, 0, 0, 0, 2), baseFret = 1, voicingName = "Open Position"),
                ChordShape("Gmaj7", "Guitar", listOf(3, -1, 4, 4, 3, -1), listOf(1, 0, 3, 4, 2, 0), baseFret = 3, voicingName = "3rd Fret Drop-2"),
                ChordShape("Gmaj7", "Guitar", listOf(-1, 10, 12, 11, 12, 10), listOf(0, 1, 3, 2, 4, 1), baseFret = 10, barres = listOf(10), voicingName = "10th Fret Barre"),
                ChordShape("Gmaj7", "Guitar", listOf(-1, -1, 5, 7, 7, 7), listOf(0, 0, 1, 3, 3, 3), baseFret = 5, barres = listOf(7), voicingName = "7th Fret Barre")
            )
            "C" -> listOf(
                ChordShape("C", "Guitar", listOf(-1, 3, 2, 0, 1, 0), listOf(0, 3, 2, 0, 1, 0), baseFret = 1, voicingName = "Open Position"),
                ChordShape("C", "Guitar", listOf(-1, 3, 5, 5, 5, 3), listOf(0, 1, 2, 3, 4, 1), baseFret = 3, barres = listOf(3), voicingName = "3rd Fret Barre"),
                ChordShape("C", "Guitar", listOf(8, 10, 10, 9, 8, 8), listOf(1, 3, 4, 2, 1, 1), baseFret = 8, barres = listOf(8), voicingName = "8th Fret Barre")
            )
            "G" -> listOf(
                ChordShape("G", "Guitar", listOf(3, 2, 0, 0, 0, 3), listOf(2, 1, 0, 0, 0, 3), baseFret = 1, voicingName = "Open Position"),
                ChordShape("G", "Guitar", listOf(3, 2, 0, 0, 3, 3), listOf(2, 1, 0, 0, 3, 4), baseFret = 1, voicingName = "Open Folk Shape"),
                ChordShape("G", "Guitar", listOf(3, 5, 5, 4, 3, 3), listOf(1, 3, 4, 2, 1, 1), baseFret = 3, barres = listOf(3), voicingName = "3rd Fret Barre"),
                ChordShape("G", "Guitar", listOf(-1, 10, 12, 12, 12, 10), listOf(0, 1, 2, 3, 4, 1), baseFret = 10, barres = listOf(10), voicingName = "10th Fret Barre")
            )
            "D" -> listOf(
                ChordShape("D", "Guitar", listOf(-1, -1, 0, 2, 3, 2), listOf(0, 0, 0, 1, 3, 2), baseFret = 1, voicingName = "Open Position"),
                ChordShape("D", "Guitar", listOf(-1, 5, 7, 7, 7, 5), listOf(0, 1, 2, 3, 4, 1), baseFret = 5, barres = listOf(5), voicingName = "5th Fret Barre"),
                ChordShape("D", "Guitar", listOf(10, 12, 12, 11, 10, 10), listOf(1, 3, 4, 2, 1, 1), baseFret = 10, barres = listOf(10), voicingName = "10th Fret Barre")
            )
            "Em" -> listOf(
                ChordShape("Em", "Guitar", listOf(0, 2, 2, 0, 0, 0), listOf(0, 2, 3, 0, 0, 0), baseFret = 1, voicingName = "Open Position"),
                ChordShape("Em", "Guitar", listOf(-1, 7, 9, 9, 8, 7), listOf(0, 1, 3, 4, 2, 1), baseFret = 7, barres = listOf(7), voicingName = "7th Fret Barre"),
                ChordShape("Em", "Guitar", listOf(12, 14, 14, 12, 12, 12), listOf(1, 3, 4, 1, 1, 1), baseFret = 12, barres = listOf(12), voicingName = "12th Fret Barre")
            )
            "Am" -> listOf(
                ChordShape("Am", "Guitar", listOf(-1, 0, 2, 2, 1, 0), listOf(0, 0, 2, 3, 1, 0), baseFret = 1, voicingName = "Open Position"),
                ChordShape("Am", "Guitar", listOf(5, 7, 7, 5, 5, 5), listOf(1, 3, 4, 1, 1, 1), baseFret = 5, barres = listOf(5), voicingName = "5th Fret Barre"),
                ChordShape("Am", "Guitar", listOf(-1, 12, 14, 14, 13, 12), listOf(0, 1, 3, 4, 2, 1), baseFret = 12, barres = listOf(12), voicingName = "12th Fret Barre")
            )
            "F" -> listOf(
                ChordShape("F", "Guitar", listOf(1, 3, 3, 2, 1, 1), listOf(1, 3, 4, 2, 1, 1), baseFret = 1, barres = listOf(1), voicingName = "1st Fret Barre"),
                ChordShape("F", "Guitar", listOf(-1, -1, 3, 2, 1, 1), listOf(0, 0, 3, 2, 1, 1), baseFret = 1, barres = listOf(1), voicingName = "Easy 4-String Shape"),
                ChordShape("F", "Guitar", listOf(-1, 8, 10, 10, 10, 8), listOf(0, 1, 2, 3, 4, 1), baseFret = 8, barres = listOf(8), voicingName = "8th Fret Barre")
            )
            "Bm" -> listOf(
                ChordShape("Bm", "Guitar", listOf(-1, 2, 4, 4, 3, 2), listOf(0, 1, 3, 4, 2, 1), baseFret = 2, barres = listOf(2), voicingName = "2nd Fret Barre"),
                ChordShape("Bm", "Guitar", listOf(7, 9, 9, 7, 7, 7), listOf(1, 3, 4, 1, 1, 1), baseFret = 7, barres = listOf(7), voicingName = "7th Fret Barre")
            )
            "F#m" -> listOf(
                ChordShape("F#m", "Guitar", listOf(2, 4, 4, 2, 2, 2), listOf(1, 3, 4, 1, 1, 1), baseFret = 2, barres = listOf(2), voicingName = "2nd Fret Barre"),
                ChordShape("F#m", "Guitar", listOf(-1, 9, 11, 11, 10, 9), listOf(0, 1, 3, 4, 2, 1), baseFret = 9, barres = listOf(9), voicingName = "9th Fret Barre")
            )
            "Cmaj7" -> listOf(
                ChordShape("Cmaj7", "Guitar", listOf(-1, 3, 2, 0, 0, 0), listOf(0, 3, 2, 0, 0, 0), baseFret = 1, voicingName = "Open Position"),
                ChordShape("Cmaj7", "Guitar", listOf(-1, 3, 5, 4, 5, 3), listOf(0, 1, 3, 2, 4, 1), baseFret = 3, barres = listOf(3), voicingName = "3rd Fret Barre"),
                ChordShape("Cmaj7", "Guitar", listOf(8, 10, 9, 9, 8, 8), listOf(1, 4, 2, 3, 1, 1), baseFret = 8, barres = listOf(8), voicingName = "8th Fret Barre")
            )
            "Fmaj7" -> listOf(
                ChordShape("Fmaj7", "Guitar", listOf(-1, -1, 3, 2, 1, 0), listOf(0, 0, 3, 2, 1, 0), baseFret = 1, voicingName = "Open Position"),
                ChordShape("Fmaj7", "Guitar", listOf(1, 3, 2, 2, 1, 1), listOf(1, 4, 2, 3, 1, 1), baseFret = 1, barres = listOf(1), voicingName = "1st Fret Barre")
            )
            else -> emptyList()
        }
    }

    /**
     * Calculates the exact note sounded on each guitar string for a given chord shape.
     */
    fun getGuitarStringNotes(shape: ChordShape, useFlats: Boolean = false): List<String> {
        val openMidi = listOf(40, 45, 50, 55, 59, 64) // E2, A2, D3, G3, B3, E4
        val noteList = if (useFlats) NOTES_FLAT else NOTES_SHARP
        return shape.frets.take(6).mapIndexed { sIdx, fret ->
            if (fret < 0) "✕"
            else {
                val open = openMidi.getOrElse(sIdx) { 40 }
                val pitch = (open + fret) % 12
                noteList[pitch]
            }
        }
    }

    /**
     * Returns multiple Ukulele shapes / voicings for any given chord.
     */
    fun getUkuleleChordShapes(chord: String): List<ChordShape> {
        val clean = chord.split("/")[0].trim()
        val specific = getSpecificUkuleleShapes(clean)
        if (specific.isNotEmpty()) return specific

        val root = extractRootNote(clean)
        var rootIdx = NOTES_SHARP.indexOf(root)
        if (rootIdx == -1) rootIdx = NOTES_FLAT.indexOf(root)
        if (rootIdx == -1) rootIdx = 0

        val shift = rootIdx
        val f1 = listOf((0 + shift) % 12, (0 + shift) % 12, (0 + shift) % 12, (3 + shift) % 12)
        val f2 = listOf((2 + shift) % 12, (0 + shift) % 12, (1 + shift) % 12, (0 + shift) % 12)

        return listOf(
            ChordShape(clean, "Ukulele", f1, listOf(1, 1, 1, 3), baseFret = 1, voicingName = "Standard Voicing"),
            ChordShape(clean, "Ukulele", f2, listOf(2, 0, 1, 0), baseFret = 1, voicingName = "Alternative Shape")
        )
    }

    private fun getSpecificUkuleleShapes(chord: String): List<ChordShape> {
        return when (chord) {
            "Gmaj7" -> listOf(
                ChordShape("Gmaj7", "Ukulele", listOf(0, 2, 2, 2), listOf(0, 1, 2, 3), baseFret = 1, voicingName = "Open Shape"),
                ChordShape("Gmaj7", "Ukulele", listOf(4, 2, 2, 2), listOf(3, 1, 1, 1), baseFret = 2, barres = listOf(2), voicingName = "Barre Shape")
            )
            "C" -> listOf(
                ChordShape("C", "Ukulele", listOf(0, 0, 0, 3), listOf(0, 0, 0, 3), baseFret = 1, voicingName = "Open Shape"),
                ChordShape("C", "Ukulele", listOf(5, 4, 3, 3), listOf(3, 2, 1, 1), baseFret = 3, barres = listOf(3), voicingName = "3rd Fret Barre")
            )
            "G" -> listOf(
                ChordShape("G", "Ukulele", listOf(0, 2, 3, 2), listOf(0, 1, 3, 2), baseFret = 1, voicingName = "Open Shape"),
                ChordShape("G", "Ukulele", listOf(4, 2, 3, 2), listOf(3, 1, 2, 1), baseFret = 2, barres = listOf(2), voicingName = "2nd Fret Barre")
            )
            "Am" -> listOf(
                ChordShape("Am", "Ukulele", listOf(2, 0, 0, 0), listOf(2, 0, 0, 0), baseFret = 1, voicingName = "Open Shape"),
                ChordShape("Am", "Ukulele", listOf(5, 4, 5, 3), listOf(3, 2, 4, 1), baseFret = 3, voicingName = "3rd Fret Shape")
            )
            "F" -> listOf(
                ChordShape("F", "Ukulele", listOf(2, 0, 1, 0), listOf(2, 0, 1, 0), baseFret = 1, voicingName = "Open Shape"),
                ChordShape("F", "Ukulele", listOf(5, 5, 5, 3), listOf(3, 3, 3, 1), baseFret = 3, barres = listOf(5), voicingName = "5th Fret Shape")
            )
            "Em" -> listOf(
                ChordShape("Em", "Ukulele", listOf(0, 4, 3, 2), listOf(0, 3, 2, 1), baseFret = 1, voicingName = "Open Shape"),
                ChordShape("Em", "Ukulele", listOf(4, 4, 3, 2), listOf(2, 3, 1, 1), baseFret = 2, barres = listOf(2), voicingName = "2nd Fret Barre")
            )
            "Dm" -> listOf(
                ChordShape("Dm", "Ukulele", listOf(2, 2, 1, 0), listOf(2, 3, 1, 0), baseFret = 1, voicingName = "Open Shape"),
                ChordShape("Dm", "Ukulele", listOf(7, 5, 5, 5), listOf(3, 1, 1, 1), baseFret = 5, barres = listOf(5), voicingName = "5th Fret Barre")
            )
            else -> emptyList()
        }
    }
}
