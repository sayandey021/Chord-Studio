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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gbros.tabslite.utilities.ChordShape
import com.gbros.tabslite.utilities.LearnInstrument
import com.gbros.tabslite.utilities.MusicTheoryAudioPlayer
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

    val coroutineScope = rememberCoroutineScope()

    fun previewActiveShape() {
        when (selectedInstrument) {
            InstrumentMode.GUITAR -> {
                val activeShape = guitarShapes.getOrElse(guitarShapeIndex.coerceIn(0, guitarShapes.size - 1)) {
                    guitarShapes.first()
                }
                MusicTheoryAudioPlayer.strumChord(activeShape, LearnInstrument.GUITAR, scope = coroutineScope)
            }
            InstrumentMode.UKULELE -> {
                val activeShape = ukuleleShapes.getOrElse(ukuleleShapeIndex.coerceIn(0, ukuleleShapes.size - 1)) {
                    ukuleleShapes.first()
                }
                MusicTheoryAudioPlayer.strumChord(activeShape, LearnInstrument.UKULELE, scope = coroutineScope)
            }
            InstrumentMode.PIANO -> {
                val root = TabTransposer.extractRootNote(activeChordName).ifEmpty { "C" }
                MusicTheoryAudioPlayer.playPianoChord(chordNotes, root, scope = coroutineScope)
            }
        }
    }

    LaunchedEffect(activeChordName, selectedInstrument, guitarShapeIndex, ukuleleShapeIndex) {
        previewActiveShape()
    }

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

                // Chord Title with interactive audio preview button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = activeChordName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF8FAFC),
                        fontSize = 32.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = { previewActiveShape() },
                        shape = CircleShape,
                        color = ColorAccentBlue.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, ColorAccentBlue),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Preview Chord Sound",
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (chordNotes.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        chordNotes.forEachIndexed { idx, note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorAccentBlue,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        val learnInst = when (selectedInstrument) {
                                            InstrumentMode.GUITAR -> LearnInstrument.GUITAR
                                            InstrumentMode.UKULELE -> LearnInstrument.UKULELE
                                            InstrumentMode.PIANO -> LearnInstrument.PIANO
                                        }
                                        MusicTheoryAudioPlayer.playInstrumentNote(note, learnInst)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            if (idx < chordNotes.size - 1) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Main Diagram View (Guitar Fretboard, Ukulele Fretboard, or Piano)
                when (selectedInstrument) {
                    InstrumentMode.GUITAR -> {
                        val activeShape = guitarShapes.getOrElse(guitarShapeIndex.coerceIn(0, guitarShapes.size - 1)) {
                            guitarShapes.first()
                        }
                        AuthenticFretboardDiagram(
                            shape = activeShape,
                            onNoteClick = { stringIdx, fret ->
                                MusicTheoryAudioPlayer.playFretNote(LearnInstrument.GUITAR, stringIdx, fret)
                            }
                        )

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
                            onSelectIndex = { guitarShapeIndex = it },
                            onPreviewSound = { previewActiveShape() }
                        )
                    }
                    InstrumentMode.UKULELE -> {
                        val activeShape = ukuleleShapes.getOrElse(ukuleleShapeIndex.coerceIn(0, ukuleleShapes.size - 1)) {
                            ukuleleShapes.first()
                        }
                        AuthenticFretboardDiagram(
                            shape = activeShape,
                            onNoteClick = { stringIdx, fret ->
                                MusicTheoryAudioPlayer.playFretNote(LearnInstrument.UKULELE, stringIdx, fret)
                            }
                        )

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
                            onSelectIndex = { ukuleleShapeIndex = it },
                            onPreviewSound = { previewActiveShape() }
                        )
                    }
                    InstrumentMode.PIANO -> {
                        PianoKeyboardVisualizer(
                            chord = activeChordName,
                            chordNotes = chordNotes,
                            onKeyClick = { noteName ->
                                MusicTheoryAudioPlayer.playSingleNote(noteName)
                            }
                        )
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
    modifier: Modifier = Modifier,
    onNoteClick: ((stringIdx: Int, fret: Int) -> Unit)? = null
) {
    val is4String = shape.instrument.lowercase() == "ukulele" || shape.instrument.lowercase() == "bass"
    val stringCount = if (is4String) 4 else 6
    val stringNames = when (shape.instrument.lowercase()) {
        "ukulele" -> listOf("G", "C", "E", "A")
        "bass" -> listOf("E", "A", "D", "G")
        else -> listOf("E", "A", "D", "G", "B", "e")
    }

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
    val stringSpacing = gridWidth / (stringCount - 1)
    val leftLabelWidth = 24.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top String Status Indicators: 'O' for Open, 'X' for Muted
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.width(leftLabelWidth))
            Box(
                modifier = Modifier
                    .width(gridWidth)
                    .height(26.dp)
            ) {
                frets.forEachIndexed { sIdx, f ->
                    val xPos = stringSpacing * sIdx
                    val isOpen = (f == 0)
                    val symbol = when (f) {
                        0 -> "O"
                        -1 -> "✕"
                        else -> ""
                    }
                    if (symbol.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .offset(x = xPos - 13.dp)
                                .width(26.dp)
                                .fillMaxHeight()
                                .then(
                                    if (isOpen && onNoteClick != null) {
                                        Modifier
                                            .clip(CircleShape)
                                            .clickable { onNoteClick(sIdx, 0) }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbol,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (f == 0) Color(0xFFF1F5F9) else Color(0xFFEF4444)
                            )
                        }
                    }
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
                    .width(leftLabelWidth)
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

                // Vertical Strings Lines (exact xPos = stringSpacing * s)
                for (s in 0 until stringCount) {
                    val xPos = stringSpacing * s
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
                            .offset(x = xPos - (stringThickness / 2))
                            .width(stringThickness)
                            .fillMaxHeight()
                            .padding(top = if (baseFret == 1) 6.dp else 2.dp)
                            .background(Color(0xFFE2E8F0))
                    )
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
                                .background(Color(0xFFEAB308))
                                .then(
                                    if (onNoteClick != null) {
                                        Modifier.clickable { onNoteClick(0, barreFret) }
                                    } else Modifier
                                ),
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
                        val actualFret = if (baseFret > 1) baseFret + (relFret - 1) else relFret
                        val fingerNum = fingers.getOrElse(stringIdx) { 0 }
                        val fingerText = when (fingerNum) {
                            1 -> "1"
                            2 -> "2"
                            3 -> "3"
                            4 -> "4"
                            5 -> "T"
                            else -> ""
                        }

                        val xPos = stringSpacing * stringIdx
                        val yOffset = ((relFret - 1) * 41.5 + 10).dp

                        Box(
                            modifier = Modifier
                                .offset(x = xPos - 12.dp, y = yOffset)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEAB308))
                                .border(BorderStroke(1.dp, Color(0xFFCA8A04)), CircleShape)
                                .then(
                                    if (onNoteClick != null) {
                                        Modifier.clickable { onNoteClick(stringIdx, actualFret) }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fingerText,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                style = TextStyle(lineHeight = 13.sp, textAlign = TextAlign.Center),
                                modifier = Modifier.offset(y = (-1.2).dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Bottom Tuning Strings Labels: 'E A D G B e' (or 'G C E A', 'E A D G')
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.width(leftLabelWidth))
            Box(
                modifier = Modifier
                    .width(gridWidth)
                    .height(26.dp)
            ) {
                stringNames.forEachIndexed { sIdx, name ->
                    val xPos = stringSpacing * sIdx
                    Box(
                        modifier = Modifier
                            .offset(x = xPos - 13.dp)
                            .width(26.dp)
                            .fillMaxHeight()
                            .then(
                                if (onNoteClick != null) {
                                    Modifier
                                        .clip(CircleShape)
                                        .clickable { onNoteClick(sIdx, 0) }
                                } else Modifier
                            ),
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
    onPreviewSound: (() -> Unit)? = null,
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onPreviewSound?.invoke() }
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Preview Shape Sound",
                tint = ColorAccentBlue,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Voicing ${currentIndex + 1} of $totalCount: $voicingName",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PianoKeyboardVisualizer(
    chord: String,
    chordNotes: List<String>,
    highlightNote: String? = null,
    onKeyClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val whiteKeys = listOf("C", "D", "E", "F", "G", "A", "B", "C", "D", "E", "F", "G", "A", "B")
    val whiteKeyWidth = 24.dp
    val whiteKeyHeight = 114.dp
    val blackKeyWidth = 15.dp
    val blackKeyHeight = 70.dp

    val normalizedChordNotes = remember(chordNotes) {
        chordNotes.map { normalizeNoteName(it) }.toSet()
    }
    val normalizedHighlight = highlightNote?.let { normalizeNoteName(it) }

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
                    val isHighlight = normalizedHighlight == note
                    val isPressed = normalizedChordNotes.contains(note)
                    val keyColor = when {
                        isHighlight -> Color(0xFFF43F5E)
                        isPressed -> Color(0xFF3B82F6)
                        else -> Color(0xFFE2E8F0)
                    }
                    Box(
                        modifier = Modifier
                            .width(whiteKeyWidth)
                            .height(whiteKeyHeight)
                            .padding(horizontal = 0.5.dp)
                            .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(keyColor)
                            .clickable(enabled = onKeyClick != null) { onKeyClick?.invoke(note) },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (isPressed || isHighlight) {
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
                val isHighlight = normalizedHighlight == noteName
                val isPressed = normalizedChordNotes.contains(noteName)
                val leftOffset = (whiteKeyWidth * whiteIndex) + (whiteKeyWidth * 0.65f)
                val keyColor = when {
                    isHighlight -> Color(0xFFF43F5E)
                    isPressed -> Color(0xFF10B981)
                    else -> Color(0xFF0F1118)
                }

                Box(
                    modifier = Modifier
                        .offset(x = leftOffset)
                        .width(blackKeyWidth)
                        .height(blackKeyHeight)
                        .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(keyColor)
                        .clickable(enabled = onKeyClick != null) { onKeyClick?.invoke(noteName) },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (isPressed || isHighlight) {
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
            Text("White key", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
            Spacer(Modifier.width(4.dp))
            Text("Black key", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
            if (highlightNote != null) {
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF43F5E)))
                Spacer(Modifier.width(4.dp))
                Text("Current: $highlightNote", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF43F5E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
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

/**
 * Ultra-compact floating chord hover card for instant chord visualization on hover.
 */
@Composable
fun CompactChordHoverCard(
    chord: String,
    defaultInstrument: String = "Guitar",
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isUkulele = defaultInstrument.contains("Ukulele", ignoreCase = true)
    val shapes = remember(chord, isUkulele) {
        if (isUkulele) TabTransposer.getUkuleleChordShapes(chord)
        else TabTransposer.getGuitarChordShapes(chord)
    }
    val activeShape = shapes.firstOrNull() ?: ChordShape(chord, if (isUkulele) "Ukulele" else "Guitar", emptyList())
    val chordNotes = remember(chord) { TabTransposer.getChordNotes(chord) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, ColorAccentBlue.copy(alpha = 0.65f)),
        shadowElevation = 10.dp,
        modifier = modifier
            .width(176.dp)
            .wrapContentHeight()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Chord Name, Instrument Tag & Round Speaker Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chord,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Color(0xFF93C5FD)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isUkulele) "Ukulele" else "Guitar",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                    Surface(
                        onClick = {
                            val learnInst = if (isUkulele) LearnInstrument.UKULELE else LearnInstrument.GUITAR
                            MusicTheoryAudioPlayer.strumChord(activeShape, learnInst, scope = coroutineScope)
                        },
                        shape = CircleShape,
                        color = ColorAccentBlue.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, ColorAccentBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Play $chord chord sound",
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            if (chordNotes.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = chordNotes.joinToString(" • "),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))

            // Compact Fretboard Diagram
            CompactFretboardDiagram(shape = activeShape)

            Spacer(Modifier.height(6.dp))

            Text(
                text = activeShape.voicingName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CompactFretboardDiagram(
    shape: ChordShape,
    modifier: Modifier = Modifier
) {
    val is4String = shape.instrument.lowercase() == "ukulele" || shape.instrument.lowercase() == "bass"
    val stringCount = if (is4String) 4 else 6
    val stringNames = if (is4String) listOf("G", "C", "E", "A") else listOf("E", "A", "D", "G", "B", "e")

    val frets = shape.frets.take(stringCount)
    val baseFret = shape.baseFret.coerceAtLeast(1)

    val relativeFrets = frets.map { f ->
        when {
            f <= 0 -> f
            baseFret > 1 -> (f - baseFret + 1)
            else -> f
        }
    }

    val gridWidth = if (is4String) 96.dp else 120.dp
    val fretCount = 4
    val fretHeight = 19.dp
    val gridHeight = fretHeight * fretCount
    val stringSpacing = gridWidth / (stringCount - 1)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top open (O) / muted (X) indicators - locked to stringSpacing
        Box(
            modifier = Modifier
                .width(gridWidth)
                .height(16.dp)
        ) {
            frets.forEachIndexed { sIdx, f ->
                val xPos = stringSpacing * sIdx
                val symbol = when (f) {
                    0 -> "O"
                    -1 -> "✕"
                    else -> ""
                }
                if (symbol.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .offset(x = xPos - 10.dp)
                            .width(20.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = symbol,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (f == 0) Color(0xFFF1F5F9) else Color(0xFFEF4444)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(3.dp))

        // Grid with base fret label
        Box(
            modifier = Modifier
                .width(gridWidth)
                .height(gridHeight)
        ) {
            // Base fret indicator on left margin without shifting the grid
            if (baseFret > 1) {
                Text(
                    text = "${baseFret}fr",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE047),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-22).dp, y = 3.dp)
                )
            }

            // Nut line (if baseFret == 1)
            if (baseFret == 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFFE2E8F0))
                        .align(Alignment.TopCenter)
                )
            }

            // Horizontal Frets
            for (f in 0..fretCount) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .offset(y = fretHeight * f)
                        .background(Color(0xFF334155))
                )
            }

            // Vertical Strings (exact xPos = stringSpacing * s)
            for (s in 0 until stringCount) {
                val xPos = stringSpacing * s
                val stringThickness = when (s) {
                    0 -> 2.dp
                    1 -> 1.7.dp
                    2 -> 1.4.dp
                    3 -> 1.2.dp
                    4 -> 1.0.dp
                    else -> 0.8.dp
                }

                Box(
                    modifier = Modifier
                        .offset(x = xPos - (stringThickness / 2))
                        .width(stringThickness)
                        .fillMaxHeight()
                        .background(Color(0xFF94A3B8))
                )
            }

            // Fingered dots (centered exactly at stringSpacing * sIdx)
            frets.forEachIndexed { sIdx, _ ->
                val relFret = relativeFrets.getOrElse(sIdx) { -1 }
                if (relFret in 1..fretCount) {
                    val xPos = stringSpacing * sIdx
                    val yCenter = fretHeight * (relFret - 0.5f) - 6.dp

                    Box(
                        modifier = Modifier
                            .offset(x = xPos - 6.dp, y = yCenter)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFDE047)),
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Bottom String Names: 'E A D G B e' (or 'G C E A') - locked to stringSpacing
        Box(
            modifier = Modifier
                .width(gridWidth)
                .height(18.dp)
        ) {
            stringNames.forEachIndexed { sIdx, name ->
                val xPos = stringSpacing * sIdx
                Box(
                    modifier = Modifier
                        .offset(x = xPos - 10.dp)
                        .width(20.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        style = TextStyle(lineHeight = 11.sp, textAlign = TextAlign.Center)
                    )
                }
            }
        }
    }
}


