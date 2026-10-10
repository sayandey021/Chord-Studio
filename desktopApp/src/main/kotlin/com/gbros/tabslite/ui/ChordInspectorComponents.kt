package com.gbros.tabslite.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gbros.tabslite.utilities.ChordShape
import com.gbros.tabslite.utilities.TabTransposer

enum class InstrumentMode {
    GUITAR,
    UKULELE,
    PIANO
}

@Composable
fun ChordInspectorDialog(
    chord: String,
    onDismiss: () -> Unit
) {
    val settings by com.gbros.tabslite.data.DesktopDatabase.instance.settingsFlow.collectAsState()
    val initialInstrument = if (settings.defaultInstrument.contains("Ukulele", ignoreCase = true)) {
        InstrumentMode.UKULELE
    } else {
        InstrumentMode.GUITAR
    }
    var selectedInstrument by remember(settings.defaultInstrument) { mutableStateOf(initialInstrument) }
    var useFlats by remember { mutableStateOf(chord.contains("b")) }
    var activeChordName by remember(chord, useFlats) {
        val root = TabTransposer.extractRootNote(chord)
        val suffix = chord.substring(root.length)
        val transposedRoot = if (useFlats) {
            val idx = TabTransposer.NOTES_SHARP.indexOf(root)
            if (idx != -1) TabTransposer.NOTES_FLAT[idx] else root
        } else {
            val idx = TabTransposer.NOTES_FLAT.indexOf(root)
            if (idx != -1) TabTransposer.NOTES_SHARP[idx] else root
        }
        mutableStateOf("$transposedRoot$suffix")
    }

    val guitarShapes = remember(activeChordName) { TabTransposer.getGuitarChordShapes(activeChordName) }
    val ukuleleShapes = remember(activeChordName) { TabTransposer.getUkuleleChordShapes(activeChordName) }
    val chordNotes = remember(activeChordName) { TabTransposer.getChordNotes(activeChordName) }

    var guitarShapeIndex by remember(activeChordName) { mutableStateOf(0) }
    var ukuleleShapeIndex by remember(activeChordName) { mutableStateOf(0) }

    val colors = AppColors

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardElevated),
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .padding(16.dp)
                .width(390.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar: Instrument Toggle Pills & Enharmonic Flat/Sharp Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Guitar Pill
                        Surface(
                            onClick = { selectedInstrument = InstrumentMode.GUITAR },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedInstrument == InstrumentMode.GUITAR) ColorAccentBlue.copy(alpha = 0.2f) else ColorSurfaceCard,
                            border = if (selectedInstrument == InstrumentMode.GUITAR) BorderStroke(1.dp, ColorAccentBlue) else BorderStroke(1.dp, ColorCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (selectedInstrument == InstrumentMode.GUITAR) ColorAccentBlue else ColorTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Guitar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedInstrument == InstrumentMode.GUITAR) Color(0xFF93C5FD) else ColorTextPrimary
                                )
                            }
                        }

                        // Ukulele Button
                        Surface(
                            onClick = { selectedInstrument = InstrumentMode.UKULELE },
                            shape = CircleShape,
                            color = if (selectedInstrument == InstrumentMode.UKULELE) ColorAccentBlue.copy(alpha = 0.2f) else ColorSurfaceCard,
                            border = if (selectedInstrument == InstrumentMode.UKULELE) BorderStroke(1.dp, ColorAccentBlue) else BorderStroke(1.dp, ColorCardBorder),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Ukulele",
                                    tint = if (selectedInstrument == InstrumentMode.UKULELE) ColorAccentBlue else ColorTextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Piano Button
                        Surface(
                            onClick = { selectedInstrument = InstrumentMode.PIANO },
                            shape = CircleShape,
                            color = if (selectedInstrument == InstrumentMode.PIANO) ColorAccentBlue.copy(alpha = 0.2f) else ColorSurfaceCard,
                            border = if (selectedInstrument == InstrumentMode.PIANO) BorderStroke(1.dp, ColorAccentBlue) else BorderStroke(1.dp, ColorCardBorder),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Piano",
                                    tint = if (selectedInstrument == InstrumentMode.PIANO) ColorAccentBlue else ColorTextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Flat / Sharp Enharmonic Toggle
                    Surface(
                        onClick = { useFlats = !useFlats },
                        shape = CircleShape,
                        color = Color(0xFF1C1E2A),
                        border = BorderStroke(1.dp, Color(0xFF33374C)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (useFlats) "♭" else "♯",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Chord Title (Large centered name matching screenshot)
                Text(
                    text = activeChordName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF8FAFC),
                    fontSize = 32.sp
                )

                if (chordNotes.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = chordNotes.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Main Diagram View (Guitar Fretboard, Ukulele Fretboard, or Piano)
                when (selectedInstrument) {
                    InstrumentMode.GUITAR -> {
                        val activeShape = guitarShapes.getOrElse(guitarShapeIndex.coerceIn(0, guitarShapes.size - 1)) {
                            guitarShapes.first()
                        }
                        AuthenticFretboardDiagram(shape = activeShape)

                        Spacer(Modifier.height(16.dp))

                        // Multi-voicing Pager Controls
                        VoicingPaginationControls(
                            currentIndex = guitarShapeIndex,
                            totalCount = guitarShapes.size,
                            voicingName = activeShape.voicingName,
                            onPrevious = {
                                if (guitarShapeIndex > 0) guitarShapeIndex--
                                else guitarShapeIndex = guitarShapes.size - 1
                            },
                            onNext = {
                                if (guitarShapeIndex < guitarShapes.size - 1) guitarShapeIndex++
                                else guitarShapeIndex = 0
                            },
                            onSelectIndex = { guitarShapeIndex = it }
                        )
                    }
                    InstrumentMode.UKULELE -> {
                        val activeShape = ukuleleShapes.getOrElse(ukuleleShapeIndex.coerceIn(0, ukuleleShapes.size - 1)) {
                            ukuleleShapes.first()
                        }
                        AuthenticFretboardDiagram(shape = activeShape)

                        Spacer(Modifier.height(16.dp))

                        VoicingPaginationControls(
                            currentIndex = ukuleleShapeIndex,
                            totalCount = ukuleleShapes.size,
                            voicingName = activeShape.voicingName,
                            onPrevious = {
                                if (ukuleleShapeIndex > 0) ukuleleShapeIndex--
                                else ukuleleShapeIndex = ukuleleShapes.size - 1
                            },
                            onNext = {
                                if (ukuleleShapeIndex < ukuleleShapes.size - 1) ukuleleShapeIndex++
                                else ukuleleShapeIndex = 0
                            },
                            onSelectIndex = { ukuleleShapeIndex = it }
                        )
                    }
                    InstrumentMode.PIANO -> {
                        PianoKeyboardVisualizer(chord = activeChordName, chordNotes = chordNotes)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Authentic Fretboard Diagram matching the official guitar/ukulele tablature design.
 */
@Composable
fun AuthenticFretboardDiagram(
    shape: ChordShape,
    modifier: Modifier = Modifier
) {
    val stringCount = if (shape.instrument.lowercase() == "ukulele") 4 else 6
    val stringNames = if (stringCount == 4) listOf("G", "C", "E", "A") else listOf("E", "A", "D", "G", "B", "e")

    val frets = shape.frets.take(stringCount)
    val fingers = shape.fingers.take(stringCount)
    val baseFret = shape.baseFret.coerceAtLeast(1)

    // Calculate relative frets (1 to 5) for rendering in the 5-fret grid
    val relativeFrets = frets.map { f ->
        when {
            f <= 0 -> f
            baseFret > 1 -> (f - baseFret + 1)
            else -> f
        }
    }

    val gridWidth = if (stringCount == 4) 160.dp else 220.dp
    val gridHeight = 220.dp
    val fretHeight = 40.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top String Status Indicators: 'O' for Open, 'X' for Muted
        Row(
            modifier = Modifier.width(gridWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            frets.forEach { f ->
                Box(
                    modifier = Modifier.width(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (f) {
                            0 -> "O"
                            -1 -> "✕"
                            else -> ""
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (f) {
                            0 -> Color(0xFFF1F5F9)
                            -1 -> Color(0xFFEF4444)
                            else -> Color.Transparent
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Fretboard Grid Container (with left fret number labels)
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left Fret Numbers Column
            Column(
                modifier = Modifier
                    .height(gridHeight)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (f in 1..5) {
                    val actualFretNumber = if (baseFret > 1) baseFret + (f - 1) else f
                    Box(
                        modifier = Modifier.height(fretHeight),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = actualFretNumber.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (baseFret > 1 && f == 1) Color(0xFFFDE047) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // The Fretboard Canvas Box
            Box(
                modifier = Modifier
                    .width(gridWidth)
                    .height(gridHeight)
            ) {
                // Background & Border Grid
                Column(modifier = Modifier.fillMaxSize()) {
                    // Nut Bar (Thick cream line if baseFret == 1, else regular fret wire)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (baseFret == 1) 6.dp else 2.dp)
                            .background(
                                if (baseFret == 1) Color(0xFFE6DCBE) else Color(0xFF64748B),
                                shape = if (baseFret == 1) RoundedCornerShape(2.dp) else RoundedCornerShape(0.dp)
                            )
                    )

                    // 5 Fret Horizontal Spaces
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(fretHeight)
                        )
                        HorizontalDivider(
                            color = Color(0xFF64748B),
                            thickness = 1.5.dp
                        )
                    }
                }

                // Vertical Strings Lines
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (baseFret == 1) 6.dp else 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (s in 0 until stringCount) {
                        // String thickness varies from thicker bass strings to thin treble strings
                        val stringThickness = when (s) {
                            0 -> 2.5.dp
                            1 -> 2.0.dp
                            2 -> 1.7.dp
                            3 -> 1.4.dp
                            4 -> 1.1.dp
                            else -> 0.9.dp
                        }

                        Box(
                            modifier = Modifier
                                .width(stringThickness)
                                .fillMaxHeight()
                                .background(Color(0xFFE2E8F0))
                        )
                    }
                }

                // Barre Chord Pill (if shape has barres)
                shape.barres.forEach { barreFret ->
                    val relBarreFret = if (baseFret > 1) (barreFret - baseFret + 1) else barreFret
                    if (relBarreFret in 1..5) {
                        val yOffset = ((relBarreFret - 1) * 41.5 + 12).dp
                        Box(
                            modifier = Modifier
                                .offset(y = yOffset)
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFFEAB308)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "1",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }

                // Finger Marker Dots (Golden circles matching the screenshot)
                relativeFrets.forEachIndexed { stringIdx, relFret ->
                    if (relFret in 1..5) {
                        val fingerNum = fingers.getOrElse(stringIdx) { 0 }
                        val fingerText = when (fingerNum) {
                            1 -> "1"
                            2 -> "2"
                            3 -> "3"
                            4 -> "4"
                            5 -> "T"
                            else -> ""
                        }

                        val stringSpacing = (gridWidth.value / (stringCount - 1)).dp
                        val xOffset = (stringIdx * stringSpacing.value - 12).dp
                        val yOffset = ((relFret - 1) * 41.5 + 10).dp

                        Box(
                            modifier = Modifier
                                .offset(x = xOffset, y = yOffset)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEAB308))
                                .border(BorderStroke(1.dp, Color(0xFFCA8A04)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fingerText,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Bottom Tuning Strings Labels: 'E A D G B e' (or 'G C E A')
        Row(
            modifier = Modifier.width(gridWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stringNames.forEach { name ->
                Box(
                    modifier = Modifier.width(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9)
                    )
                }
            }
        }
    }
}

/**
 * Paging dots and navigation arrows for switching between different voicings/shapes.
 */
@Composable
fun VoicingPaginationControls(
    currentIndex: Int,
    totalCount: Int,
    voicingName: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Previous Voicing",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Carousel Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until totalCount) {
                    val isSelected = i == currentIndex
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFFFDE047) else Color(0xFF33374C))
                            .clickable { onSelectIndex(i) }
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Next Button
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Next Voicing",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Voicing ${currentIndex + 1} of $totalCount: $voicingName",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PianoKeyboardVisualizer(
    chord: String,
    chordNotes: List<String>,
    modifier: Modifier = Modifier
) {
    val whiteKeys = listOf("C", "D", "E", "F", "G", "A", "B", "C", "D", "E", "F", "G", "A", "B")
    val whiteKeyWidth = 22.dp
    val whiteKeyHeight = 110.dp
    val blackKeyWidth = 14.dp
    val blackKeyHeight = 68.dp

    val normalizedChordNotes = remember(chordNotes) {
        chordNotes.map { normalizeNoteName(it) }.toSet()
    }

    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Piano Keyboard Pattern",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .width(whiteKeyWidth * whiteKeys.size)
                .height(whiteKeyHeight)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E2230))
        ) {
            // Render White Keys
            Row(modifier = Modifier.fillMaxSize()) {
                whiteKeys.forEach { note ->
                    val isPressed = normalizedChordNotes.contains(note)
                    Box(
                        modifier = Modifier
                            .width(whiteKeyWidth)
                            .height(whiteKeyHeight)
                            .padding(horizontal = 0.5.dp)
                            .background(
                                if (isPressed) Color(0xFF3B82F6) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (isPressed) {
                            Text(
                                text = note,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            // Render Black Keys
            val blackKeyPlacements = listOf(
                0 to "C#", 1 to "D#",
                3 to "F#", 4 to "G#", 5 to "A#",
                7 to "C#", 8 to "D#",
                10 to "F#", 11 to "G#", 12 to "A#"
            )

            blackKeyPlacements.forEach { (whiteIndex, noteName) ->
                val isPressed = normalizedChordNotes.contains(noteName)
                val leftOffset = (whiteKeyWidth * whiteIndex) + (whiteKeyWidth * 0.65f)

                Box(
                    modifier = Modifier
                        .offset(x = leftOffset)
                        .width(blackKeyWidth)
                        .height(blackKeyHeight)
                        .background(
                            if (isPressed) Color(0xFF10B981) else Color(0xFF0F1118),
                            shape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (isPressed) {
                        Text(
                            text = noteName,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
            Spacer(Modifier.width(4.dp))
            Text("White key press", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
            Spacer(Modifier.width(4.dp))
            Text("Black key press", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}

private fun normalizeNoteName(note: String): String {
    return when (note.trim().uppercase()) {
        "DB" -> "C#"
        "EB" -> "D#"
        "GB" -> "F#"
        "AB" -> "G#"
        "BB" -> "A#"
        else -> note.trim().uppercase()
    }
}

