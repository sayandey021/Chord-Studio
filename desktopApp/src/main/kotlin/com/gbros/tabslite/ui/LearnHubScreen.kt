package com.gbros.tabslite.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.gbros.tabslite.utilities.*

enum class LearnTab(val title: String, val subtitle: String) {
    CHORDS("Chord Encyclopedia", "All shapes & voicings across instruments"),
    SCALES("Scales & Modes Lab", "Fretboard patterns, degrees & theory"),
    STRUMMING("Strumming Patterns", "Master rhythms across different time signatures")
}

@Composable
fun LearnHubScreen(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LearnTab.CHORDS) }
    val colors = AppColors

    DisposableEffect(Unit) {
        onDispose {
            MusicTheoryAudioPlayer.stop()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        // Studio Header
        Surface(
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.accent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Learn & Theory Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Master chord shapes, scale fretboard layouts, and time-signature strumming patterns",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Primary Mode Tabs (Chords / Scales / Strumming)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LearnTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Surface(
                            onClick = {
                                MusicTheoryAudioPlayer.stop()
                                selectedTab = tab
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) colors.cardElevated else colors.card,
                            border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = when (tab) {
                                    LearnTab.CHORDS -> Icons.Default.MusicNote
                                    LearnTab.SCALES -> Icons.Default.GraphicEq
                                    LearnTab.STRUMMING -> Icons.Default.Speed
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.accent else colors.textMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tab.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Section Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                LearnTab.CHORDS -> ChordsEncyclopediaSection()
                LearnTab.SCALES -> ScalesExplorerSection()
                LearnTab.STRUMMING -> StrummingPatternsSection()
            }
        }
    }
}

// =========================================================================================
// 1. CHORDS ENCYCLOPEDIA & SHAPES SECTION
// =========================================================================================

@Composable
fun ChordsEncyclopediaSection() {
    val colors = AppColors
    val coroutineScope = rememberCoroutineScope()

    var useFlats by remember { mutableStateOf(false) }
    var selectedRoot by remember { mutableStateOf("C") }
    var selectedQuality by remember { mutableStateOf(MusicTheoryData.CHORD_QUALITIES.first()) }
    var selectedInstrument by remember { mutableStateOf(LearnInstrument.GUITAR) }
    var selectedPianoTone by remember { mutableStateOf(PianoTone.CONCERT_GRAND) }
    var searchQuery by remember { mutableStateOf("") }

    val activeChordName = remember(selectedRoot, selectedQuality) {
        "${selectedRoot}${selectedQuality.suffix}"
    }

    // Voicings
    val chordShapes = remember(activeChordName, selectedInstrument) {
        MusicTheoryData.getAllChordVoicings(activeChordName, selectedInstrument)
    }
    var currentShapeIndex by remember(activeChordName, selectedInstrument) { mutableStateOf(0) }

    val activeShape = chordShapes.getOrElse(currentShapeIndex.coerceIn(0, (chordShapes.size - 1).coerceAtLeast(0))) {
        chordShapes.firstOrNull() ?: ChordShape(activeChordName, selectedInstrument.displayName, listOf(-1, 0, 2, 2, 1, 0), listOf(0, 0, 2, 3, 1, 0), 1)
    }

    val chordNotes = remember(activeChordName) {
        TabTransposer.getChordNotes(activeChordName)
    }

    val listState = rememberLazyListState()
    val rightScrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isNarrow = maxWidth < 920.dp
        val leftColWidth = if (isNarrow) 260.dp else if (maxWidth < 1150.dp) 285.dp else 315.dp
        val padH = if (isNarrow) 16.dp else 24.dp
        val padV = 16.dp

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padH, vertical = padV),
            horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 14.dp else 20.dp)
        ) {
            // Left Column: Root Selector, Quality Selector, Search Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.width(leftColWidth).fillMaxHeight()
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Search Bar
                item {
                    Text("Search Chord", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.textMuted)
                    Spacer(Modifier.height(6.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { input ->
                            searchQuery = input
                            if (input.isNotBlank()) {
                                val root = TabTransposer.extractRootNote(input)
                                if (root.isNotEmpty()) {
                                    val matchedRoot = if (MusicTheoryData.NOTES_SHARP.contains(root)) root else if (MusicTheoryData.NOTES_FLAT.contains(root)) root else null
                                    if (matchedRoot != null) {
                                        selectedRoot = matchedRoot
                                        useFlats = root.contains("b")
                                        val suffix = input.substring(root.length).trim()
                                        val q = MusicTheoryData.CHORD_QUALITIES.find { it.suffix.equals(suffix, ignoreCase = true) }
                                        if (q != null) selectedQuality = q
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(colors.accent),
                        decorationBox = { inner ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .background(colors.card, RoundedCornerShape(8.dp))
                                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text("Type e.g. G, Bm, F#m7, Cadd9...", color = colors.textMuted, fontSize = 12.sp)
                                    }
                                    inner()
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = colors.textMuted, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    )
                }

                // Root Notes Picker with Enharmonic Flat/Sharp Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Root Note", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.textMuted)
                        Surface(
                            onClick = {
                                useFlats = !useFlats
                                val curIdx = if (!useFlats) MusicTheoryData.NOTES_FLAT.indexOf(selectedRoot) else MusicTheoryData.NOTES_SHARP.indexOf(selectedRoot)
                                if (curIdx != -1) {
                                    selectedRoot = if (useFlats) MusicTheoryData.NOTES_FLAT[curIdx] else MusicTheoryData.NOTES_SHARP[curIdx]
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = colors.card,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (useFlats) "♭ Flats" else "♯ Sharps",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        lineHeight = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accent
                                    ),
                                    modifier = Modifier.offset(y = (-0.8).dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val noteList = if (useFlats) MusicTheoryData.NOTES_FLAT else MusicTheoryData.NOTES_SHARP

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        noteList.forEach { note ->
                            val isSelected = selectedRoot == note
                            Surface(
                                onClick = { selectedRoot = note },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) colors.accent else colors.card,
                                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                modifier = Modifier.size(width = 46.dp, height = 34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = note,
                                        style = TextStyle(
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            lineHeight = 13.sp,
                                            color = if (isSelected) Color.White else colors.textPrimary,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.offset(y = (-0.8).dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Chord Qualities Grouped
                item {
                    Text("Chord Type & Quality", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.textMuted)
                    Spacer(Modifier.height(8.dp))

                    val categories = MusicTheoryData.CHORD_QUALITIES.map { it.category }.distinct()
                    categories.forEach { cat ->
                        Text(
                            text = cat.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        val qualitiesInCat = MusicTheoryData.CHORD_QUALITIES.filter { it.category == cat }
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            qualitiesInCat.forEach { quality ->
                                val isSelected = selectedQuality.id == quality.id
                                Surface(
                                    onClick = { selectedQuality = quality },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.card,
                                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 9.dp)
                                    ) {
                                        Text(
                                            text = quality.name,
                                            style = TextStyle(
                                                fontSize = 11.5.sp,
                                                lineHeight = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) colors.accent else colors.textSecondary
                                            ),
                                            modifier = Modifier.offset(y = (-0.6).dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Instrument Switcher, Authentic Fretboard / Keyboard Visualizer, Theory Card, Audio
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rightScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar: Active Chord, Instrument Switcher, Audio Action Buttons
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    // Row 1: Active Chord Title & Quality on Left + Strum / Arpeggiate Audio Buttons on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeChordName,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = selectedQuality.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Formula: ${selectedQuality.formula} (${chordNotes.joinToString(" - ")})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        // Play Audio Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isChordPlaying = MusicTheoryAudioPlayer.isPlaying
                            if (isChordPlaying) {
                                Button(
                                    onClick = { MusicTheoryAudioPlayer.stop() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Stop Audio",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (selectedInstrument == LearnInstrument.PIANO) {
                                            MusicTheoryAudioPlayer.playPianoChord(chordNotes, selectedRoot, selectedPianoTone, isArpeggio = false, scope = coroutineScope)
                                        } else {
                                            MusicTheoryAudioPlayer.strumChord(activeShape, selectedInstrument, isArpeggio = false, scope = coroutineScope)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (selectedInstrument == LearnInstrument.PIANO) "Play Chord" else "Strum",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (selectedInstrument == LearnInstrument.PIANO) {
                                            MusicTheoryAudioPlayer.playPianoChord(chordNotes, selectedRoot, selectedPianoTone, isArpeggio = true, scope = coroutineScope)
                                        } else {
                                            MusicTheoryAudioPlayer.strumChord(activeShape, selectedInstrument, isArpeggio = true, scope = coroutineScope)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, colors.borderHover),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Arpeggiate",
                                        fontSize = 12.sp,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                    Spacer(Modifier.height(10.dp))

                    // Row 2: Instrument Selector Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Instrument:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted
                            )
                            Spacer(Modifier.width(4.dp))
                            LearnInstrument.entries.forEach { inst ->
                                val isSelected = selectedInstrument == inst
                                Surface(
                                    onClick = {
                                        selectedInstrument = inst
                                        currentShapeIndex = 0
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.card,
                                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Text(
                                            text = inst.displayName,
                                            style = TextStyle(
                                                fontSize = 11.5.sp,
                                                lineHeight = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) colors.accent else colors.textSecondary
                                            ),
                                            modifier = Modifier.offset(y = (-0.8).dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Piano Tone Ribbon (visible when Piano is selected)
                    AnimatedVisibility(
                        visible = selectedInstrument == LearnInstrument.PIANO,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = colors.border)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🎹 Piano Tone:",
                                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accent
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        PianoTone.entries.forEach { tone ->
                                            val isSelected = selectedPianoTone == tone
                                            Surface(
                                                onClick = {
                                                    selectedPianoTone = tone
                                                    MusicTheoryAudioPlayer.setInstrument(LearnInstrument.PIANO, tone)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) colors.accent else colors.card,
                                                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 10.dp)
                                                ) {
                                                    Text(
                                                        text = tone.displayName,
                                                        style = TextStyle(
                                                            fontSize = 11.sp,
                                                            lineHeight = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) Color.White else colors.textSecondary,
                                                            textAlign = TextAlign.Center
                                                        ),
                                                        modifier = Modifier.offset(y = (-0.8).dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = selectedPianoTone.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = colors.border)
                    Spacer(Modifier.height(8.dp))

                    // Horizontal Scrollable Chord Qualities Bar
                    HorizontalScrollableFilterBar(
                        items = MusicTheoryData.CHORD_QUALITIES,
                        selectedItem = selectedQuality,
                        onItemSelected = { selectedQuality = it },
                        labelProvider = { it.name },
                        badgeProvider = { it.category }
                    )
                }
            }

            // Main Diagram Display Area (Fretboard or Piano)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedInstrument) {
                        LearnInstrument.GUITAR, LearnInstrument.UKULELE, LearnInstrument.BASS -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                AuthenticFretboardDiagram(
                                    shape = activeShape,
                                    onNoteClick = { stringIdx, fret ->
                                        MusicTheoryAudioPlayer.playFretNote(
                                            instrument = selectedInstrument,
                                            stringIdx = stringIdx,
                                            fret = fret,
                                            pianoTone = selectedPianoTone,
                                            scope = coroutineScope
                                        )
                                    }
                                )

                                Spacer(Modifier.height(18.dp))

                                if (chordShapes.size > 1) {
                                    VoicingPaginationControls(
                                        currentIndex = currentShapeIndex,
                                        totalCount = chordShapes.size,
                                        voicingName = activeShape.voicingName,
                                        onPrevious = {
                                            currentShapeIndex = if (currentShapeIndex > 0) currentShapeIndex - 1 else chordShapes.size - 1
                                            chordShapes.getOrNull(currentShapeIndex)?.let { s ->
                                                MusicTheoryAudioPlayer.strumChord(s, selectedInstrument, scope = coroutineScope)
                                            }
                                        },
                                        onNext = {
                                            currentShapeIndex = if (currentShapeIndex < chordShapes.size - 1) currentShapeIndex + 1 else 0
                                            chordShapes.getOrNull(currentShapeIndex)?.let { s ->
                                                MusicTheoryAudioPlayer.strumChord(s, selectedInstrument, scope = coroutineScope)
                                            }
                                        },
                                        onSelectIndex = {
                                            currentShapeIndex = it
                                            chordShapes.getOrNull(currentShapeIndex)?.let { s ->
                                                MusicTheoryAudioPlayer.strumChord(s, selectedInstrument, scope = coroutineScope)
                                            }
                                        },
                                        onPreviewSound = {
                                            MusicTheoryAudioPlayer.strumChord(activeShape, selectedInstrument, scope = coroutineScope)
                                        }
                                    )
                                } else {
                                    Text(
                                        text = "Shape: ${activeShape.voicingName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                        LearnInstrument.PIANO -> {
                            PianoKeyboardVisualizer(
                                chord = activeChordName,
                                chordNotes = chordNotes,
                                onKeyClick = { noteName ->
                                    MusicTheoryAudioPlayer.playSingleNote(noteName, 4, selectedPianoTone, coroutineScope)
                                }
                            )
                        }
                    }
                }
            }

            // Theory Details Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Harmonic Formula: ${selectedQuality.formula}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = selectedQuality.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chordNotes.forEach { n ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.card,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text(
                                    text = n,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (n == selectedRoot) Color(0xFFFDE047) else colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

// =========================================================================================
// 2. SCALES & MODES EXPLORER SECTION
// =========================================================================================

@Composable
fun ScalesExplorerSection() {
    val colors = AppColors
    val coroutineScope = rememberCoroutineScope()

    var useFlats by remember { mutableStateOf(false) }
    var selectedRoot by remember { mutableStateOf("C") }
    var selectedScale by remember { mutableStateOf(MusicTheoryData.SCALES.first()) }
    var selectedInstrument by remember { mutableStateOf(LearnInstrument.GUITAR) }
    var selectedPianoTone by remember { mutableStateOf(PianoTone.CONCERT_GRAND) }
    var showDegreesInsteadOfNotes by remember { mutableStateOf(false) }
    var playingNoteStep by remember { mutableStateOf(-1) }

    val scaleNotes = remember(selectedRoot, selectedScale, useFlats) {
        MusicTheoryData.getScaleNotes(selectedRoot, selectedScale, useFlats)
    }

    val fretboardNotes = remember(selectedRoot, selectedScale, selectedInstrument, useFlats) {
        if (selectedInstrument != LearnInstrument.PIANO) {
            MusicTheoryData.getFretboardScaleNotes(selectedRoot, selectedScale, selectedInstrument, maxFrets = 15, useFlats = useFlats)
        } else emptyList()
    }

    val sectionScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(sectionScrollState)
            .padding(horizontal = 32.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Control Ribbon: Root Selector, Scale Selector, Instrument Selector, Play Scale
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Row 1: Key Selector on Left + Play Scale Audio Button on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Root Notes
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Key:",
                            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        )
                        Spacer(Modifier.width(8.dp))
                        val noteList = if (useFlats) MusicTheoryData.NOTES_FLAT else MusicTheoryData.NOTES_SHARP
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            noteList.forEach { note ->
                                val isSelected = selectedRoot == note
                                Surface(
                                    onClick = { selectedRoot = note },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) colors.accent else colors.card,
                                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                    modifier = Modifier.size(width = 30.dp, height = 28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = note,
                                            style = TextStyle(
                                                fontSize = 11.sp,
                                                lineHeight = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else colors.textPrimary,
                                                textAlign = TextAlign.Center
                                            ),
                                            modifier = Modifier.offset(y = (-0.8).dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            onClick = {
                                useFlats = !useFlats
                                val curIdx = if (!useFlats) MusicTheoryData.NOTES_FLAT.indexOf(selectedRoot) else MusicTheoryData.NOTES_SHARP.indexOf(selectedRoot)
                                if (curIdx != -1) {
                                    selectedRoot = if (useFlats) MusicTheoryData.NOTES_FLAT[curIdx] else MusicTheoryData.NOTES_SHARP[curIdx]
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = colors.card,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.size(width = 28.dp, height = 28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (useFlats) "♭" else "♯",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accent,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.offset(y = (-0.8).dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Play Scale Audio Button / Stop Button
                    val isScalePlaying = playingNoteStep != -1 || MusicTheoryAudioPlayer.isPlaying
                    Button(
                        onClick = {
                            if (isScalePlaying) {
                                MusicTheoryAudioPlayer.stop()
                                playingNoteStep = -1
                            } else {
                                MusicTheoryAudioPlayer.playScaleNotes(
                                    notes = scaleNotes,
                                    bpm = 130,
                                    instrument = selectedInstrument,
                                    pianoTone = selectedPianoTone,
                                    scope = coroutineScope
                                ) { step ->
                                    playingNoteStep = step
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScalePlaying) Color(0xFFEF4444) else colors.accent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isScalePlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isScalePlaying) "Stop Scale Run" else "Play Scale Run",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.offset(y = (-0.8).dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                Spacer(Modifier.height(10.dp))

                // Row 2: Instrument Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Instrument:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        )
                        Spacer(Modifier.width(4.dp))
                        LearnInstrument.entries.forEach { inst ->
                            val isSelected = selectedInstrument == inst
                            Surface(
                                onClick = { selectedInstrument = inst },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.card,
                                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = inst.displayName,
                                        style = TextStyle(
                                            fontSize = 11.5.sp,
                                            lineHeight = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.accent else colors.textSecondary,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.offset(y = (-0.8).dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Piano Tone Ribbon (visible when Piano is selected)
                AnimatedVisibility(
                    visible = selectedInstrument == LearnInstrument.PIANO,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = colors.border)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🎹 Piano Tone:",
                                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                                Spacer(Modifier.width(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    PianoTone.entries.forEach { tone ->
                                        val isSelected = selectedPianoTone == tone
                                        Surface(
                                            onClick = {
                                                selectedPianoTone = tone
                                                MusicTheoryAudioPlayer.setInstrument(LearnInstrument.PIANO, tone)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) colors.accent else colors.card,
                                            border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = tone.displayName,
                                                    style = TextStyle(
                                                        fontSize = 11.sp,
                                                        lineHeight = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color.White else colors.textSecondary,
                                                        textAlign = TextAlign.Center
                                                    ),
                                                    modifier = Modifier.offset(y = (-0.8).dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = selectedPianoTone.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Row 2: Scale Types Horizontal Scroll / Filter Bar
                HorizontalScrollableFilterBar(
                    items = MusicTheoryData.SCALES,
                    selectedItem = selectedScale,
                    onItemSelected = { selectedScale = it },
                    labelProvider = { it.name },
                    badgeProvider = { it.category }
                )
            }
        }

        // Fretboard or Keyboard Visualizer Container
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                // Top Legend & View Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "$selectedRoot ${selectedScale.name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        // Legend badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEAB308))
                            )
                            Text(
                                text = "Root Note ($selectedRoot)",
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    lineHeight = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textMuted
                                ),
                                modifier = Modifier.offset(y = (-1.2).dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            )
                            Text(
                                text = "Scale Notes",
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    lineHeight = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textMuted
                                ),
                                modifier = Modifier.offset(y = (-1.2).dp)
                            )
                        }
                    }

                    // Toggle: Note Names vs Scale Degrees
                    Surface(
                        onClick = { showDegreesInsteadOfNotes = !showDegreesInsteadOfNotes },
                        shape = RoundedCornerShape(6.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = if (showDegreesInsteadOfNotes) "Showing: Scale Degrees (1, 3, 5...)" else "Showing: Note Names (C, E, G...)",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    lineHeight = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accent
                                ),
                                modifier = Modifier.offset(y = (-0.8).dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Fretboard or Piano Keyboard
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (selectedInstrument == LearnInstrument.PIANO) {
                        PianoKeyboardVisualizer(
                            chord = "$selectedRoot ${selectedScale.name}",
                            chordNotes = scaleNotes,
                            highlightNote = if (playingNoteStep != -1) scaleNotes.getOrNull(playingNoteStep) else null,
                            onKeyClick = { noteName ->
                                MusicTheoryAudioPlayer.playSingleNote(noteName, 4, selectedPianoTone, coroutineScope)
                            }
                        )
                    } else {
                        // Interactive Full Fretboard Display (frets 0 to 15)
                        InteractiveScaleFretboard(
                            instrument = selectedInstrument,
                            scaleNotes = fretboardNotes,
                            showDegrees = showDegreesInsteadOfNotes,
                            highlightNote = if (playingNoteStep != -1) scaleNotes.getOrNull(playingNoteStep) else null,
                            onNoteClick = { stringIdx, fret ->
                                MusicTheoryAudioPlayer.playFretNote(
                                    instrument = selectedInstrument,
                                    stringIdx = stringIdx,
                                    fret = fret,
                                    pianoTone = selectedPianoTone,
                                    scope = coroutineScope
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Scale Notes Row with audio pulse highlight (cleanly below fretboard with dedicated spacing)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scale Notes: ",
                        style = TextStyle(
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        ),
                        modifier = Modifier.offset(y = (-0.8).dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    scaleNotes.forEachIndexed { idx, note ->
                        val isPlayingThis = (idx == playingNoteStep)
                        Surface(
                            onClick = {
                                MusicTheoryAudioPlayer.playInstrumentNote(
                                    note = note,
                                    instrument = selectedInstrument,
                                    pianoTone = selectedPianoTone,
                                    scope = coroutineScope
                                )
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                isPlayingThis -> colors.accent
                                note == selectedRoot -> Color(0xFFEAB308).copy(alpha = 0.2f)
                                else -> colors.surface
                            },
                            border = BorderStroke(1.dp, if (isPlayingThis || note == selectedRoot) Color(0xFFEAB308) else colors.border),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = note,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPlayingThis) Color.White else if (note == selectedRoot) Color(0xFFFDE047) else colors.textPrimary,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.offset(y = (-0.8).dp)
                                )
                            }
                        }
                        if (idx < scaleNotes.size - 1) {
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                }
            }
        }

        // Scale Theory & Harmonization Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Formula: ${selectedScale.formulaText}  •  Mood: ${selectedScale.mood}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = selectedScale.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    if (selectedScale.famousSongs.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Famous examples: ${selectedScale.famousSongs.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }

                if (selectedScale.harmonizedTriads.isNotEmpty()) {
                    Spacer(Modifier.width(20.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Harmonized Chords:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.textMuted)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            selectedScale.harmonizedTriads.forEach { tri ->
                                Surface(
                                    onClick = {
                                        val triNotes = TabTransposer.getChordNotes(tri)
                                        val triRoot = TabTransposer.extractRootNote(tri).ifEmpty { "C" }
                                        if (selectedInstrument == LearnInstrument.PIANO) {
                                            MusicTheoryAudioPlayer.playPianoChord(triNotes, triRoot, selectedPianoTone, scope = coroutineScope)
                                        } else {
                                            val shapes = MusicTheoryData.getAllChordVoicings(tri, selectedInstrument)
                                            shapes.firstOrNull()?.let { s ->
                                                MusicTheoryAudioPlayer.strumChord(s, selectedInstrument, scope = coroutineScope)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    color = colors.card,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    ) {
                                        Text(
                                            text = tri,
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                lineHeight = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textSecondary
                                            ),
                                            modifier = Modifier.offset(y = (-0.6).dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Horizontal Fretboard Diagram showing frets 0 to 15
 */
@Composable
fun ScaleFretNoteBadge(
    text: String,
    isRoot: Boolean,
    isHighlight: Boolean,
    size: androidx.compose.ui.unit.Dp = 25.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val bgColor = when {
        isHighlight -> Color(0xFFF43F5E)
        isRoot -> Color(0xFFEAB308)
        else -> Color(0xFF0284C7)
    }
    val textColor = when {
        isRoot && !isHighlight -> Color.Black
        else -> Color.White
    }
    val borderColor = when {
        isHighlight -> Color.White
        isRoot -> Color(0xFFFDE047)
        else -> Color(0xFF38BDF8)
    }

    val fontSize = if (text.length >= 3) 9.sp else if (text.length == 2) 10.5.sp else 12.sp

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, borderColor, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = textColor,
            textAlign = TextAlign.Center,
            style = TextStyle(
                lineHeight = fontSize,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.offset(y = (-1.2).dp)
        )
    }
}

/**
 * Interactive Horizontal Fretboard Diagram showing frets 0 to 15 with pixel-perfect note centering
 */
@Composable
fun InteractiveScaleFretboard(
    instrument: LearnInstrument,
    scaleNotes: List<MusicTheoryData.FretNoteInfo>,
    showDegrees: Boolean = false,
    highlightNote: String? = null,
    onNoteClick: ((stringIdx: Int, fret: Int) -> Unit)? = null
) {
    val stringCount = instrument.stringCount
    val stringNames = instrument.stringNames
    val maxFrets = 15

    val fretboardScrollState = rememberScrollState()

    val stringRowHeight = 32.dp
    val fretboardGridHeight = stringRowHeight * stringCount
    val fretWidth = 54.dp
    val nutWidth = 8.dp
    val noteCircleSize = 25.dp

    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(fretboardScrollState)
            .horizontalMouseWheelScroll(fretboardScrollState, scope)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: String Names (Open Headstock)
        Column(
            modifier = Modifier.height(fretboardGridHeight)
        ) {
            for (row in 0 until stringCount) {
                val stringIdx = (stringCount - 1) - row
                val sName = stringNames.getOrElse(stringIdx) { "" }
                Box(
                    modifier = Modifier
                        .height(stringRowHeight)
                        .width(28.dp)
                        .then(
                            if (onNoteClick != null) {
                                Modifier.clip(CircleShape).clickable { onNoteClick(stringIdx, 0) }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        style = TextStyle(lineHeight = 12.sp),
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(6.dp))

        // Fret 0 (Open Strings Note Badges)
        Column(
            modifier = Modifier.height(fretboardGridHeight)
        ) {
            for (row in 0 until stringCount) {
                val stringIdx = (stringCount - 1) - row
                val openNote = scaleNotes.find { it.stringIdx == stringIdx && it.fret == 0 }

                Box(
                    modifier = Modifier
                        .height(stringRowHeight)
                        .width(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (openNote != null) {
                        ScaleFretNoteBadge(
                            text = if (showDegrees) openNote.scaleDegree else openNote.noteName,
                            isRoot = openNote.isRoot,
                            isHighlight = openNote.noteName == highlightNote,
                            size = noteCircleSize,
                            onClick = if (onNoteClick != null) { { onNoteClick(openNote.stringIdx, 0) } } else null
                        )
                    } else {
                        Text(
                            text = "✕",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset(y = (-1).dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(6.dp))

        // Nut Bar (Bone/Brass Cream Line)
        Box(
            modifier = Modifier
                .width(nutWidth)
                .height(fretboardGridHeight)
                .background(Color(0xFFE6DCBE), RoundedCornerShape(2.dp))
        )

        // Frets 1 to 15 Grid
        Box(
            modifier = Modifier
                .width(fretWidth * maxFrets)
                .height(fretboardGridHeight + 26.dp)
        ) {
            // Horizontal Strings Lines (Running exactly through the vertical center of each row)
            for (row in 0 until stringCount) {
                val thickness = when (row) {
                    stringCount - 1 -> 2.5.dp
                    stringCount - 2 -> 2.0.dp
                    stringCount - 3 -> 1.6.dp
                    stringCount - 4 -> 1.3.dp
                    stringCount - 5 -> 1.0.dp
                    else -> 0.8.dp
                }
                val wireY = (row * stringRowHeight.value) + (stringRowHeight.value / 2f) - (thickness.value / 2f)

                Box(
                    modifier = Modifier
                        .offset(y = wireY.dp)
                        .fillMaxWidth()
                        .height(thickness)
                        .background(Color(0xFF64748B))
                )
            }

            // Vertical Fret Wires
            Row(modifier = Modifier.fillMaxWidth().height(fretboardGridHeight)) {
                for (f in 1..maxFrets) {
                    Box(
                        modifier = Modifier
                            .width(fretWidth)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        // Fret Wire Line
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF94A3B8))
                        )

                        // Inlay Dots on frets 3, 5, 7, 9, 12 (double dot), 15
                        if (f in listOf(3, 5, 7, 9, 15)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF334155))
                                )
                            }
                        } else if (f == 12) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF334155)))
                                Spacer(Modifier.height(20.dp))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF334155)))
                            }
                        }
                    }
                }
            }

            // Scale Note Markers Placed on Frets
            scaleNotes.filter { it.fret in 1..maxFrets }.forEach { noteInfo ->
                val stringInvert = (stringCount - 1) - noteInfo.stringIdx
                val xPos = ((noteInfo.fret - 1) * fretWidth.value) + ((fretWidth.value - noteCircleSize.value) / 2f)
                val yPos = (stringInvert * stringRowHeight.value) + ((stringRowHeight.value - noteCircleSize.value) / 2f)

                ScaleFretNoteBadge(
                    text = if (showDegrees) noteInfo.scaleDegree else noteInfo.noteName,
                    isRoot = noteInfo.isRoot,
                    isHighlight = noteInfo.noteName == highlightNote,
                    size = noteCircleSize,
                    modifier = Modifier.offset(x = xPos.dp, y = yPos.dp),
                    onClick = if (onNoteClick != null) { { onNoteClick(noteInfo.stringIdx, noteInfo.fret) } } else null
                )
            }

            // Bottom Fret Numbers (1 to 15)
            Row(
                modifier = Modifier
                    .offset(y = fretboardGridHeight + 6.dp)
                    .width(fretWidth * maxFrets)
            ) {
                for (f in 1..maxFrets) {
                    Box(
                        modifier = Modifier.width(fretWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = f.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (f in listOf(3, 5, 7, 9, 12, 15)) Color(0xFF94A3B8) else Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================================
// 3. STRUMMING PATTERNS FOR DIFFERENT TIME SIGNATURES SECTION
// =========================================================================================

@Composable
fun StrummingPatternsSection() {
    val colors = AppColors
    val coroutineScope = rememberCoroutineScope()

    var selectedTimeSignature by remember { mutableStateOf("All") }
    var selectedPattern by remember { mutableStateOf(MusicTheoryData.STRUMMING_PATTERNS.first()) }
    var bpm by remember(selectedPattern) { mutableStateOf(selectedPattern.defaultBpm) }
    var playbackChord by remember { mutableStateOf("G") }
    var selectedGuitarSound by remember { mutableStateOf(StrumGuitarSound.ACOUSTIC_STEEL) }
    var activeBeatIndex by remember { mutableStateOf(-1) }

    val filteredPatterns = remember(selectedTimeSignature) {
        if (selectedTimeSignature == "All") MusicTheoryData.STRUMMING_PATTERNS
        else MusicTheoryData.STRUMMING_PATTERNS.filter { it.timeSignature == selectedTimeSignature }
    }

    val timeSignatures = listOf("All", "4/4", "3/4", "6/8", "2/4", "12/8", "5/4")
    val playbackChords = listOf("G", "C", "D", "Em", "Am", "F", "A", "E")

    val strumScrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isNarrow = maxWidth < 920.dp
        val leftColWidth = if (isNarrow) 260.dp else if (maxWidth < 1150.dp) 285.dp else 315.dp
        val padH = if (isNarrow) 16.dp else 24.dp
        val padV = 16.dp

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padH, vertical = padV),
            horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 14.dp else 20.dp)
        ) {
            // Left Column: Filter Chips & Patterns Catalog
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.width(leftColWidth).fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Time Signature Filters",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(8.dp))

                    // Time Signature Filter Chips
                    val tsListState = rememberLazyListState()
                    LazyRow(
                        state = tsListState,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().horizontalMouseWheelScroll(tsListState, coroutineScope)
                    ) {
                        items(timeSignatures) { ts ->
                            val isSelected = selectedTimeSignature == ts
                            Surface(
                                onClick = { selectedTimeSignature = ts },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) colors.accent else colors.card,
                                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = ts,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            lineHeight = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else colors.textSecondary
                                        ),
                                        modifier = Modifier.offset(y = (-0.8).dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colors.border)
                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Available Rhythms (${filteredPatterns.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(8.dp))

                    // Patterns List with dedicated non-overlapping scrollbar
                    val patternsListState = rememberLazyListState()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            state = patternsListState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredPatterns) { pattern ->
                                val isSelected = selectedPattern.id == pattern.id
                                Surface(
                                    onClick = {
                                        MusicTheoryAudioPlayer.stop()
                                        selectedPattern = pattern
                                        bpm = pattern.defaultBpm
                                        activeBeatIndex = -1
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) colors.cardElevated else colors.card,
                                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = pattern.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) colors.accent else colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = colors.surface,
                                                modifier = Modifier.height(20.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                ) {
                                                    Text(
                                                        text = pattern.timeSignature,
                                                        style = TextStyle(
                                                            fontSize = 10.sp,
                                                            lineHeight = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.accent
                                                        ),
                                                        modifier = Modifier.offset(y = (-0.6).dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = pattern.genre,
                                                fontSize = 11.sp,
                                                color = colors.textMuted
                                            )
                                            Text(
                                                text = "${pattern.defaultBpm} bpm",
                                                fontSize = 11.sp,
                                                color = colors.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(6.dp))

                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(patternsListState),
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 2.dp),
                            style = defaultScrollbarStyle().copy(
                                unhoverColor = Color.White.copy(alpha = 0.18f),
                                hoverColor = colors.accent,
                                thickness = 6.dp,
                                shape = RoundedCornerShape(3.dp)
                            )
                        )
                    }
                }
            }

            // Right Column: Interactive Strumming Stage, Visualizer, Metronome, Controls with dedicated scrollbar
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(strumScrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Header Bar: Pattern Name, Time Signature, Genre, Difficulty
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.accent.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, colors.accent),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        ) {
                                            Text(
                                                text = selectedPattern.timeSignature,
                                                style = TextStyle(
                                                    fontSize = 14.sp,
                                                    lineHeight = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = colors.accent
                                                ),
                                                modifier = Modifier.offset(y = (-0.8).dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = selectedPattern.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Feel: ${selectedPattern.genre}  •  Difficulty: ${selectedPattern.difficulty}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            // Interactive Strum Audio Play/Stop Button (Protected with min width, height & single-line text)
                            val isPatternPlaying = MusicTheoryAudioPlayer.isPlaying
                            Button(
                                onClick = {
                                    if (isPatternPlaying) {
                                        MusicTheoryAudioPlayer.stop()
                                        activeBeatIndex = -1
                                    } else {
                                        MusicTheoryAudioPlayer.playStrummingLoop(
                                            pattern = selectedPattern,
                                            bpm = bpm,
                                            chordName = playbackChord,
                                            sound = selectedGuitarSound,
                                            scope = coroutineScope
                                        ) { beatIdx ->
                                            activeBeatIndex = beatIdx
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPatternPlaying) Color(0xFFEF4444) else colors.accent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 140.dp).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPatternPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isPatternPlaying) "Stop Loop" else "Play Rhythm Loop",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = colors.border)
                        Spacer(Modifier.height(10.dp))

                        // Horizontal Scrollable Rhythms Bar
                        HorizontalScrollableFilterBar(
                            items = filteredPatterns,
                            selectedItem = selectedPattern,
                            onItemSelected = {
                                MusicTheoryAudioPlayer.stop()
                                selectedPattern = it
                                bpm = it.defaultBpm
                                activeBeatIndex = -1
                            },
                            labelProvider = { it.name },
                            badgeProvider = { it.timeSignature }
                        )
                    }
                }

                // Central Strum Visualizer Stage
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 230.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "COUNTING SEQUENCE: ${selectedPattern.countText}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accent,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(Modifier.height(14.dp))

                        // Dynamic Interactive Strumming Bars with Animated Cursor
                        Box(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            InteractiveStrummingDiagramView(
                                pattern = selectedPattern,
                                activeBeatIndex = activeBeatIndex
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Strumming Legend Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("↓", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Down", fontSize = 11.sp, color = colors.textMuted)
                            }
                            Spacer(Modifier.width(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("↑", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Up", fontSize = 11.sp, color = colors.textMuted)
                            }
                            Spacer(Modifier.width(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("▼", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 13.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Accent", fontSize = 11.sp, color = colors.textMuted)
                            }
                            Spacer(Modifier.width(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("B", fontWeight = FontWeight.Bold, color = Color(0xFFA855F7), fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Bass", fontSize = 11.sp, color = colors.textMuted)
                            }
                            Spacer(Modifier.width(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✕", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Mute", fontSize = 11.sp, color = colors.textMuted)
                            }
                            Spacer(Modifier.width(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .offset(y = 1.dp)
                                        .background(Color(0xFF64748B), CircleShape)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Rest", fontSize = 11.sp, color = colors.textMuted)
                            }
                        }
                    }
                }

                // Controls Ribbon: Tempo Slider, Chords Selector & Practice Tips
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tempo BPM Control
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tempo: ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.textMuted)
                                Text("$bpm BPM", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.accent)
                                Spacer(Modifier.width(10.dp))
                                IconButton(
                                    onClick = { bpm = (bpm - 5).coerceAtLeast(40) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("-5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                                }
                                Slider(
                                    value = bpm.toFloat(),
                                    onValueChange = {
                                        bpm = it.toInt()
                                        if (MusicTheoryAudioPlayer.isPlaying) {
                                            MusicTheoryAudioPlayer.playStrummingLoop(selectedPattern, bpm, playbackChord, selectedGuitarSound, coroutineScope) { b -> activeBeatIndex = b }
                                        }
                                    },
                                    valueRange = 40f..220f,
                                    modifier = Modifier.width(130.dp)
                                )
                                IconButton(
                                    onClick = { bpm = (bpm + 5).coerceAtMost(220) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("+5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // Playback Sound Chord with horizontal scrolling to prevent clipping
                            val chordScrollState = rememberScrollState()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false).horizontalScroll(chordScrollState)
                            ) {
                                Text(
                                    text = "Sound Chord: ",
                                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMuted
                                )
                                Spacer(Modifier.width(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    playbackChords.forEach { c ->
                                        val isSelected = playbackChord == c
                                        Surface(
                                            onClick = {
                                                playbackChord = c
                                                if (MusicTheoryAudioPlayer.isPlaying) {
                                                    MusicTheoryAudioPlayer.playStrummingLoop(selectedPattern, bpm, playbackChord, selectedGuitarSound, coroutineScope) { b -> activeBeatIndex = b }
                                                }
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) colors.accent else colors.card,
                                            border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                            modifier = Modifier.size(width = 30.dp, height = 26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = c,
                                                    style = TextStyle(
                                                        fontSize = 11.sp,
                                                        lineHeight = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color.White else colors.textPrimary,
                                                        textAlign = TextAlign.Center
                                                    ),
                                                    modifier = Modifier.offset(y = (-0.8).dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = colors.border)
                        Spacer(Modifier.height(8.dp))

                        // Guitar Sound / Tone Selector (horizontal scrollable to fit any width)
                        val toneScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(toneScrollState),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tone:",
                                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                            Spacer(Modifier.width(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StrumGuitarSound.entries.forEach { tone ->
                                    val isSelected = selectedGuitarSound == tone
                                    Surface(
                                        onClick = {
                                            selectedGuitarSound = tone
                                            MusicTheoryAudioPlayer.setStrumGuitarSound(tone)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) colors.accent else colors.card,
                                        border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 9.dp)
                                        ) {
                                            Text(
                                                text = tone.displayName,
                                                style = TextStyle(
                                                    fontSize = 11.sp,
                                                    lineHeight = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else colors.textSecondary,
                                                    textAlign = TextAlign.Center
                                                ),
                                                modifier = Modifier.offset(y = (-0.8).dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = colors.border)
                        Spacer(Modifier.height(10.dp))

                        // Tips & Example Songs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Technique Tip: ${selectedPattern.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            if (selectedPattern.exampleSongs.isNotEmpty()) {
                                Spacer(Modifier.width(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Songs: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
                                    Text(
                                        text = selectedPattern.exampleSongs.take(2).joinToString(" • "),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.accent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(strumScrollState),
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    style = defaultScrollbarStyle().copy(
                        unhoverColor = Color.White.copy(alpha = 0.18f),
                        hoverColor = colors.accent,
                        thickness = 6.dp,
                        shape = RoundedCornerShape(3.dp)
                    )
                )
            }
        }
    }
}

/**
 * Beautiful Animated Strumming Beats Sequence with bouncing glowing indicator
 */
@Composable
fun InteractiveStrummingDiagramView(
    pattern: StrumPattern,
    activeBeatIndex: Int
) {
    val beats = pattern.beats
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .horizontalMouseWheelScroll(scrollState, scope)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        beats.forEachIndexed { idx, beat ->
            val isActive = idx == activeBeatIndex

            val strokeColor = when (beat.stroke) {
                StrumStrokeType.DOWN -> Color(0xFF38BDF8)
                StrumStrokeType.UP -> Color(0xFF10B981)
                StrumStrokeType.ACCENT_DOWN, StrumStrokeType.ACCENT_UP -> Color(0xFFF59E0B)
                StrumStrokeType.BASS -> Color(0xFFA855F7)
                StrumStrokeType.MUTE -> Color(0xFFEF4444)
                StrumStrokeType.REST -> Color(0xFF475569)
            }

            Column(
                modifier = Modifier
                    .width(58.dp)
                    .padding(horizontal = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Playhead / Accent Indicator (fixed 20.dp container)
                Box(
                    modifier = Modifier.height(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        Surface(
                            shape = CircleShape,
                            color = strokeColor,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    } else if (beat.stroke == StrumStrokeType.ACCENT_DOWN || beat.stroke == StrumStrokeType.ACCENT_UP) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                            modifier = Modifier.height(14.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text(
                                    text = "ACC",
                                    fontSize = 8.sp,
                                    lineHeight = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // The Strum Column Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isActive -> strokeColor.copy(alpha = 0.25f)
                        beat.isPrimaryBeat -> Color(0xFF1E2433)
                        else -> Color(0xFF141822)
                    },
                    border = BorderStroke(
                        width = if (isActive) 2.dp else 1.dp,
                        color = when {
                            isActive -> strokeColor
                            beat.isPrimaryBeat -> Color(0xFF334155)
                            else -> Color(0xFF1E293B)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp, bottom = 8.dp, start = 2.dp, end = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Strum Symbol in vertically centered zone
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (beat.stroke == StrumStrokeType.REST) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(strokeColor, CircleShape)
                                )
                            } else {
                                Text(
                                    text = beat.stroke.symbol,
                                    fontSize = when (beat.stroke) {
                                        StrumStrokeType.DOWN, StrumStrokeType.UP -> 24.sp
                                        StrumStrokeType.ACCENT_DOWN, StrumStrokeType.ACCENT_UP -> 20.sp
                                        else -> 18.sp
                                    },
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = strokeColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Strum Stroke Label with dedicated height container to prevent clipping & misalignment
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = beat.stroke.shortLabel,
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color.White else Color(0xFF94A3B8),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Beat Count Label (e.g. "1", "&", "2", "&")
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isActive) strokeColor else Color.Transparent,
                    modifier = Modifier.height(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                        Text(
                            text = beat.countLabel,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            fontWeight = if (beat.isPrimaryBeat || isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) Color.Black else if (beat.isPrimaryBeat) Color(0xFFF1F5F9) else Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
