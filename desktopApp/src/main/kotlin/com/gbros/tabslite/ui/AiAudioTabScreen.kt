package com.gbros.tabslite.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import com.gbros.tabslite.data.DesktopDatabase
import com.gbros.tabslite.data.Tab
import com.gbros.tabslite.utilities.AudioAnalysisEngine
import com.gbros.tabslite.utilities.AudioAnalysisResult
import com.gbros.tabslite.utilities.ChordEvent
import com.gbros.tabslite.utilities.ChordShape
import com.gbros.tabslite.utilities.DemoAudioTrack
import com.gbros.tabslite.utilities.OnnxChordModel
import com.gbros.tabslite.utilities.StudioAudioPlayer
import com.gbros.tabslite.utilities.TabTransposer
import com.gbros.tabslite.utilities.LyricsExtractor
import com.gbros.tabslite.utilities.SyncedLyricLine
import com.gbros.tabslite.utilities.ExtractedLyricsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.sound.sampled.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAudioTabScreen(
    onOpenInTabViewer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Studio View Mode: STUDIO_TIMELINE vs CLASSIC_TAB
    var activeViewMode by remember { mutableStateOf("STUDIO_TIMELINE") }
    var showImportDialog by remember { mutableStateOf(false) }

    // Audio Analysis State
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var currentAudioSamples by remember { mutableStateOf<FloatArray?>(null) }
    var selectedDemo by remember { mutableStateOf<DemoAudioTrack?>(null) }
    var songTitle by remember { mutableStateOf("") }
    var artistName by remember { mutableStateOf("") }
    var customLyrics by remember { mutableStateOf("") }
    var syncedLyrics by remember { mutableStateOf<List<SyncedLyricLine>>(emptyList()) }
    var showLyricsDrawer by remember { mutableStateOf(false) }
    var isFetchingLyricsOnline by remember { mutableStateOf(false) }
    var isTranscribingLyricsAi by remember { mutableStateOf(false) }
    var aiLyricsProgressText by remember { mutableStateOf("") }
    var aiTranscribeErrorText by remember { mutableStateOf<String?>(null) }

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    var currentStepMessage by remember { mutableStateOf("Ready to analyze audio") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var analysisResult by remember { mutableStateOf<AudioAnalysisResult?>(null) }
    var saveStatusMessage by remember { mutableStateOf<String?>(null) }

    // Studio Controls State (as circled by user)
    var chordFilterMode by remember { mutableStateOf("All") } // "Basic" or "All"
    var showPositions by remember { mutableStateOf(true) } // Positions toggle switch
    var capoPosition by remember { mutableIntStateOf(0) }
    var pitchShiftSteps by remember { mutableIntStateOf(0) } // Transpose in half steps
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var isLooping by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableFloatStateOf(0.85f) }
    var isMuted by remember { mutableStateOf(false) }

    // Playback Timeline Clock & Audio Player State
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayTimeSeconds by remember { mutableDoubleStateOf(0.0) }
    var smoothPlayTimeSeconds by remember { mutableDoubleStateOf(0.0) }
    var lastAudioClockSeconds by remember { mutableDoubleStateOf(0.0) }
    var lastAudioClockNanos by remember { mutableLongStateOf(0L) }
    val totalDurationSeconds = remember(analysisResult) { analysisResult?.durationSeconds ?: 0.0 }

    // Connect StudioAudioPlayer callbacks & volume/mute settings
    DisposableEffect(Unit) {
        StudioAudioPlayer.onProgressUpdate = { progressSec ->
            currentPlayTimeSeconds = progressSec
            lastAudioClockSeconds = progressSec
            lastAudioClockNanos = System.nanoTime()
        }
        StudioAudioPlayer.onPlaybackFinished = {
            isPlaying = false
        }
        onDispose {
            StudioAudioPlayer.stop()
        }
    }

    // 60+ FPS frame-driven clock interpolation for butter-smooth visual scrolling (timeline strictly >= 0.0s)
    LaunchedEffect(isPlaying, speedMultiplier, totalDurationSeconds) {
        if (isPlaying) {
            lastAudioClockSeconds = currentPlayTimeSeconds
            lastAudioClockNanos = System.nanoTime()
            while (isActive && isPlaying) {
                withFrameNanos { frameNanos ->
                    val elapsedSec = (frameNanos - lastAudioClockNanos) / 1_000_000_000.0
                    val interpolated = (lastAudioClockSeconds + elapsedSec * speedMultiplier).coerceIn(0.0, totalDurationSeconds)
                    smoothPlayTimeSeconds = interpolated
                    currentPlayTimeSeconds = interpolated
                }
            }
        } else {
            smoothPlayTimeSeconds = currentPlayTimeSeconds
        }
    }

    LaunchedEffect(volumeLevel) {
        StudioAudioPlayer.volume = volumeLevel
    }
    LaunchedEffect(isMuted) {
        StudioAudioPlayer.isMuted = isMuted
    }
    LaunchedEffect(isLooping) {
        StudioAudioPlayer.isLooping = isLooping
    }
    LaunchedEffect(speedMultiplier) {
        StudioAudioPlayer.speed = speedMultiplier
    }
    LaunchedEffect(pitchShiftSteps) {
        StudioAudioPlayer.pitchShift = pitchShiftSteps
    }

    // Microphone Recording State
    var isRecordingMic by remember { mutableStateOf(false) }
    var micRecordingTimeSeconds by remember { mutableIntStateOf(0) }
    var micTargetLine by remember { mutableStateOf<TargetDataLine?>(null) }
    var micRecordedSamples by remember { mutableStateOf<FloatArray?>(null) }

    fun resolveTitleAndArtist(file: File): Pair<String, String> {
        val meta = LyricsExtractor.readAudioMetadata(file)
        val (extractedArtist, extractedTitle) = LyricsExtractor.parseArtistAndTitleFromFilename(file.nameWithoutExtension)

        val title = meta.title?.takeIf { it.isNotBlank() && !it.contains("unknown", ignoreCase = true) }
            ?.let { LyricsExtractor.cleanTitle(it) }
            ?.takeIf { it.isNotBlank() }
            ?: (if (extractedTitle.isNotBlank()) extractedTitle else LyricsExtractor.cleanTitle(file.nameWithoutExtension))

        val artist = meta.artist?.takeIf { it.isNotBlank() && !it.contains("unknown", ignoreCase = true) }
            ?.let { LyricsExtractor.cleanTitle(it) }
            ?.takeIf { it.isNotBlank() }
            ?: (if (extractedArtist.isNotBlank()) extractedArtist else "Local Audio File")

        return Pair(title, artist)
    }

    fun handleStartAnalysis(fileOverride: File? = null) {
        val file = fileOverride ?: selectedFile
        val demo = if (fileOverride != null) null else selectedDemo
        val mic = if (fileOverride != null || demo != null) null else micRecordedSamples

        if (file == null && demo == null && mic == null) {
            errorMessage = "Please choose an audio file, select a demo, or record audio first."
            return
        }

        // Stop any current playback immediately
        StudioAudioPlayer.stop()
        isPlaying = false
        currentPlayTimeSeconds = 0.0
        smoothPlayTimeSeconds = 0.0
        lastAudioClockSeconds = 0.0
        capoPosition = 0
        pitchShiftSteps = 0
        activeViewMode = "STUDIO_TIMELINE"

        // Synchronize title and artist based on the selected audio source
        if (file != null) {
            selectedFile = file
            selectedDemo = null
            micRecordedSamples = null
            val (resolvedTitle, resolvedArtist) = resolveTitleAndArtist(file)
            songTitle = resolvedTitle
            artistName = resolvedArtist
        } else if (demo != null) {
            selectedDemo = demo
            selectedFile = null
            micRecordedSamples = null
            songTitle = demo.name
            artistName = demo.artist
        } else if (mic != null) {
            selectedFile = null
            selectedDemo = null
            songTitle = "Microphone Recording"
            artistName = "Live Input"
        }

        isAnalyzing = true
        analysisProgress = 0.05f
        currentStepMessage = "Loading audio into on-device DSP engine..."
        errorMessage = null
        saveStatusMessage = null
        analysisResult = null

        coroutineScope.launch {
            try {
                val samples = withContext(Dispatchers.IO) {
                    if (file != null) {
                        AudioAnalysisEngine.loadAudioFile(file) { step, pct ->
                            currentStepMessage = step
                            analysisProgress = pct
                        }
                    } else if (demo != null) {
                        AudioAnalysisEngine.generateDemoAudioSamples(demo)
                    } else if (mic != null) {
                        mic
                    } else {
                        throw IllegalStateException("No audio source available.")
                    }
                }
                currentAudioSamples = samples

                // Fast metadata lyrics check (ID3 tags, sidecar file, or fast LRCLIB online lookup).
                // Never run heavy AI Whisper automatically on import - Whisper is strictly on-demand in Lyrics Studio.
                currentStepMessage = "Checking lyrics (tags & online)..."
                analysisProgress = 0.55f
                val durationSec = samples.size.toDouble() / 16000.0
                val extractedLyricsRes = withContext(Dispatchers.IO) {
                    if (customLyrics.isNotBlank()) {
                        val parsed = LyricsExtractor.parseLrc(customLyrics)
                        ExtractedLyricsResult(plainLyrics = customLyrics, syncedLyrics = parsed, source = "manual")
                    } else if (file != null) {
                        // Fast metadata & online search only (~0.2s). Never runs Whisper.
                        LyricsExtractor.extractLyrics(file, durationSec, songTitle, artistName)
                    } else if (demo != null && demo.lyrics.isNotBlank()) {
                        ExtractedLyricsResult(plainLyrics = demo.lyrics, syncedLyrics = LyricsExtractor.parseLrc(demo.lyrics), source = "demo")
                    } else {
                        null
                    }
                }

                if (extractedLyricsRes != null) {
                    customLyrics = extractedLyricsRes.toLrcOrPlainText()
                    syncedLyrics = extractedLyricsRes.syncedLyrics
                    if (songTitle.isBlank() && !extractedLyricsRes.detectedTitle.isNullOrBlank()) {
                        songTitle = extractedLyricsRes.detectedTitle
                    }
                    if (artistName.isBlank() && !extractedLyricsRes.detectedArtist.isNullOrBlank()) {
                        artistName = extractedLyricsRes.detectedArtist
                    }
                }

                val titleToUse = songTitle
                val artistToUse = artistName

                val result = withContext(Dispatchers.Default) {
                    AudioAnalysisEngine.analyzeAudio(
                        samples = samples,
                        sampleRate = 16000f,
                        songTitle = titleToUse,
                        artistName = artistToUse,
                        providedLyrics = if (customLyrics.isNotBlank()) customLyrics else extractedLyricsRes?.plainLyrics,
                        syncedLyrics = syncedLyrics,
                        onProgress = { step, pct ->
                            currentStepMessage = step
                            analysisProgress = pct
                        }
                    )
                }
                analysisResult = result
                if (result.audioSamples.isNotEmpty()) {
                    StudioAudioPlayer.loadAudio(result.audioSamples, result.sampleRate)
                }
                showImportDialog = false
                currentPlayTimeSeconds = 0.0
                smoothPlayTimeSeconds = 0.0
                lastAudioClockSeconds = 0.0
                isPlaying = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "An unexpected error occurred during audio analysis."
            } finally {
                isAnalyzing = false
            }
        }
    }

    // Modern Native File Picker Launcher (Windows 11 Dark Common Item Dialog / macOS / Linux)
    val audioPickerLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("mp3", "wav", "flac", "m4a", "aac", "ogg", "aif", "aiff", "au")),
        title = "Select Audio File (MP3 / WAV / FLAC / M4A)"
    ) { platformFile ->
        platformFile?.let { pFile ->
            val pickedFile = pFile.file
            selectedFile = pickedFile
            selectedDemo = null
            micRecordedSamples = null
            val (resolvedTitle, resolvedArtist) = resolveTitleAndArtist(pickedFile)
            songTitle = resolvedTitle
            artistName = resolvedArtist
            customLyrics = ""
            syncedLyrics = emptyList()
            capoPosition = 0
            pitchShiftSteps = 0
            handleStartAnalysis(pickedFile)
        }
    }

    fun openFilePickerAndAnalyze() {
        try {
            StudioAudioPlayer.stop()
            isPlaying = false
            currentPlayTimeSeconds = 0.0
            smoothPlayTimeSeconds = 0.0
            lastAudioClockSeconds = 0.0

            audioPickerLauncher.launch()
        } catch (e: Exception) {
            errorMessage = "Could not open file picker: ${e.message}"
        }
    }

    fun simplifyChordsForBasicMode(
        rawList: List<ChordEvent>,
        pitchShiftSteps: Int,
        bpm: Int
    ): List<ChordEvent> {
        if (rawList.isEmpty()) return emptyList()

        val secondsPerBeat = 60.0 / bpm.coerceIn(60, 200)

        // Step 1: Transpose and simplify chord names to basic triads
        val simplifiedRaw = rawList.map { ev ->
            val transposed = if (pitchShiftSteps != 0) {
                TabTransposer.transposeChord(ev.chordName, pitchShiftSteps)
            } else ev.chordName

            val root = TabTransposer.extractRootNote(transposed)
            val isMinor = transposed.contains("m") && !transposed.contains("maj")
            val basicName = if (root.isNotEmpty()) {
                if (isMinor) "${root}m" else root
            } else {
                transposed.replace("maj7", "").replace("m7", "m").replace("7", "").replace("sus4", "").replace("sus2", "")
            }
            ev.copy(chordName = basicName)
        }

        // Step 2: Merge adjacent identical chords initially
        val mergedInitial = mutableListOf<ChordEvent>()
        for (ev in simplifiedRaw) {
            val last = mergedInitial.lastOrNull()
            if (last != null && last.chordName == ev.chordName) {
                val combinedDur = (ev.timestampSeconds + ev.durationSeconds) - last.timestampSeconds
                mergedInitial[mergedInitial.lastIndex] = last.copy(durationSeconds = combinedDur)
            } else {
                mergedInitial.add(ev)
            }
        }

        // Step 3: Remove genuine micro-glitches/flickers (shorter than ~0.65s or 0.85 beats)
        // that appear as brief transient glitches sandwiched between two identical chords.
        // Genuine chord changes (whether lasting 1 beat, 2 beats, or 4 beats) are NEVER swallowed.
        val flickerThreshold = (secondsPerBeat * 0.85).coerceIn(0.35, 0.70)
        val filtered = mutableListOf<ChordEvent>()
        var i = 0
        while (i < mergedInitial.size) {
            val curr = mergedInitial[i]
            if (curr.durationSeconds < flickerThreshold && filtered.isNotEmpty() && i + 1 < mergedInitial.size) {
                val prev = filtered.last()
                val next = mergedInitial[i + 1]
                if (prev.chordName == next.chordName) {
                    val combinedDur = (next.timestampSeconds + next.durationSeconds) - prev.timestampSeconds
                    filtered[filtered.lastIndex] = prev.copy(durationSeconds = combinedDur)
                    i += 2
                    continue
                }
            }
            filtered.add(curr)
            i++
        }

        // Step 4: Final run-length merge & beat alignment
        val finalChords = mutableListOf<ChordEvent>()
        for (ev in filtered) {
            val last = finalChords.lastOrNull()
            if (last != null && last.chordName == ev.chordName) {
                val combinedDur = (ev.timestampSeconds + ev.durationSeconds) - last.timestampSeconds
                finalChords[finalChords.lastIndex] = last.copy(durationSeconds = combinedDur)
            } else {
                finalChords.add(ev)
            }
        }

        var beatCounter = 1
        return finalChords.map { ev ->
            val beats = max(1, (ev.durationSeconds / secondsPerBeat).roundToInt())
            val eventWithBeat = ev.copy(beatNumber = beatCounter)
            beatCounter += beats
            eventWithBeat
        }
    }

    // Compute effective timeline and chords considering Pitch Shift, Capo and Filter
    val effectiveTimeline = remember(analysisResult, pitchShiftSteps, chordFilterMode, capoPosition) {
        val list = analysisResult?.chordTimeline ?: emptyList()
        val bpm = analysisResult?.detectedBpm ?: 120
        val totalShift = pitchShiftSteps - capoPosition
        if (list.isEmpty()) {
            listOf(
                ChordEvent(1.0, "A#", 0.95, 2.0, 1),
                ChordEvent(3.0, "D#", 0.95, 2.0, 3),
                ChordEvent(5.0, "F", 0.95, 2.0, 5),
                ChordEvent(7.0, "Dm", 0.95, 2.0, 7)
            ).map { ev ->
                if (totalShift != 0) ev.copy(chordName = TabTransposer.transposeChord(ev.chordName, totalShift)) else ev
            }
        } else if (chordFilterMode == "Basic") {
            simplifyChordsForBasicMode(list, totalShift, bpm)
        } else {
            // "All" mode: Transpose and merge consecutive identical chords
            val transposedList = list.map { ev ->
                val transposed = if (totalShift != 0) {
                    TabTransposer.transposeChord(ev.chordName, totalShift)
                } else ev.chordName
                ev.copy(chordName = transposed)
            }
            val mergedAll = mutableListOf<ChordEvent>()
            for (ev in transposedList) {
                val last = mergedAll.lastOrNull()
                if (last != null && last.chordName == ev.chordName) {
                    val combinedDur = (ev.timestampSeconds + ev.durationSeconds) - last.timestampSeconds
                    mergedAll[mergedAll.lastIndex] = last.copy(durationSeconds = combinedDur)
                } else {
                    mergedAll.add(ev)
                }
            }
            mergedAll
        }
    }

    val displayChords = remember(effectiveTimeline) {
        effectiveTimeline.map { it.chordName }.filter { it.isNotBlank() }.distinct()
    }

    val totalShift = pitchShiftSteps - capoPosition
    val effectiveKey = remember(analysisResult?.detectedKey, totalShift) {
        val rawKey = analysisResult?.detectedKey ?: "C Major"
        if (totalShift != 0) {
            val keyRoot = rawKey.substringBefore(" ").trim()
            val suffix = if (rawKey.contains("Minor")) " Minor" else " Major"
            val transposedRoot = TabTransposer.transposeChord(keyRoot, totalShift)
            "$transposedRoot$suffix"
        } else {
            rawKey
        }
    }

    val dynamicFormattedTab = remember(effectiveTimeline, effectiveKey, songTitle, artistName, capoPosition, analysisResult, customLyrics, syncedLyrics) {
        if (analysisResult == null && effectiveTimeline.isEmpty()) {
            "No tab generated yet. Import an audio file or select a Studio Track to analyze."
        } else {
            AudioAnalysisEngine.generateFormattedTab(
                title = songTitle,
                artist = artistName,
                key = effectiveKey,
                bpm = analysisResult?.detectedBpm ?: 120,
                capo = capoPosition,
                strumming = analysisResult?.suggestedStrumming ?: "D - D U - U D U",
                chords = effectiveTimeline,
                providedLyrics = customLyrics.takeIf { it.isNotBlank() },
                syncedLyrics = syncedLyrics
            )
        }
    }

    // Active Chord Event at current smooth playback timestamp
    val displayPlayTime = if (isPlaying) smoothPlayTimeSeconds else currentPlayTimeSeconds
    val activeEventIndex = remember(displayPlayTime, effectiveTimeline) {
        val found = effectiveTimeline.indexOfFirst { ev ->
            displayPlayTime >= ev.timestampSeconds && displayPlayTime < (ev.timestampSeconds + ev.durationSeconds)
        }
        if (found >= 0) found
        else {
            val next = effectiveTimeline.indexOfFirst { it.timestampSeconds >= displayPlayTime }
            if (next >= 0) next else 0
        }
    }
    val activeChordEvent = effectiveTimeline.getOrNull(activeEventIndex) ?: effectiveTimeline.firstOrNull() ?: ChordEvent(0.0, "", 0.95, 2.0, 1)
    val activeChordName = activeChordEvent.chordName

    // Segment Chords (ss2): displays the active segment chord (current) and the upcoming segment chords
    val segmentEvents = remember(activeEventIndex, effectiveTimeline) {
        if (effectiveTimeline.isEmpty()) emptyList()
        else {
            val count = effectiveTimeline.size
            (0 until min(3, count)).map { offset ->
                effectiveTimeline[(activeEventIndex + offset) % count]
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(ColorObsidianBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // 1. TOP STUDIO STATUS & NAVIGATION BAR
            // ==========================================
            Surface(
                color = ColorSurfaceDark,
                border = BorderStroke(1.dp, ColorCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Single Input Button + Song Title
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showImportDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorAccentBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Add Audio / Mic",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.widthIn(max = 300.dp)) {
                            Text(
                                text = if (songTitle.isNotBlank()) songTitle else "No Audio Loaded",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (songTitle.isNotBlank()) ColorTextPrimary else ColorTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (artistName.isNotBlank()) artistName else "Upload audio to begin chord analysis",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Center: Key, Time Signature, Tempo Badges (Clean Professional Icons, Metronome for Tempo)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Key Badge
                        val currentKey = analysisResult?.detectedKey
                        val displayKey = if (currentKey != null) {
                            if (pitchShiftSteps != 0) TabTransposer.transposeChord(currentKey.substringBefore(" "), pitchShiftSteps) else currentKey
                        } else "--"
                        StudioMetricPill(
                            icon = Icons.Default.MusicNote,
                            label = displayKey,
                            badgeColor = Color(0xFF38BDF8)
                        )

                        // Time Signature Badge
                        StudioMetricPill(
                            icon = null,
                            label = analysisResult?.timeSignature ?: "--",
                            badgeColor = ColorTextSecondary
                        )

                        // Tempo (BPM) Badge with AvTimer Icon
                        StudioMetricPill(
                            icon = Icons.Default.AvTimer,
                            label = analysisResult?.detectedBpm?.toString() ?: "--",
                            badgeColor = Color(0xFF34D399)
                        )
                    }

                    // Right: View Switcher & Lyrics Toggle
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lyrics Toggle Pill - Perfectly Aligned
                        Surface(
                            onClick = { showLyricsDrawer = !showLyricsDrawer },
                            shape = RoundedCornerShape(8.dp),
                            color = if (showLyricsDrawer) ColorAccentBlue else ColorSurfaceCardElevated,
                            border = BorderStroke(1.dp, if (showLyricsDrawer) ColorAccentBlue else ColorCardBorder),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Lyrics",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 12.5.sp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    imageVector = if (showLyricsDrawer) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // Switch to Classic Tab View
                        OutlinedButton(
                            onClick = {
                                activeViewMode = if (activeViewMode == "STUDIO_TIMELINE") "CLASSIC_TAB" else "STUDIO_TIMELINE"
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ColorCardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = ColorSurfaceDark),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = if (activeViewMode == "STUDIO_TIMELINE") Icons.Default.Description else Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = ColorAccentBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (activeViewMode == "STUDIO_TIMELINE") "Tab Sheet" else "Studio View",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = Color(0xFF7F1D1D).copy(alpha = 0.90f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // ==========================================
            // 2. MAIN STAGE (DAW WORKSTATION)
            // ==========================================
            if (activeViewMode == "STUDIO_TIMELINE") {
                if (analysisResult == null) {
                    StudioBlankWorkstation(
                        isAnalyzing = isAnalyzing,
                        analysisProgress = analysisProgress,
                        currentStepMessage = currentStepMessage,
                        errorMessage = errorMessage,
                        onDismissError = { errorMessage = null },
                        onUploadClick = { openFilePickerAndAnalyze() },
                        onChooseDemoOrRecord = { showImportDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // A. SYNCHRONIZED AUDIO WAVEFORM & TIMELINE (ss1)
                        StudioWaveformTimeline(
                            currentTimeSeconds = if (isPlaying) smoothPlayTimeSeconds else currentPlayTimeSeconds,
                            totalDurationSeconds = totalDurationSeconds,
                            waveformEnvelope = analysisResult?.waveformEnvelope ?: FloatArray(0),
                            chordTimeline = effectiveTimeline,
                            activeChordName = activeChordName,
                            syncedLyrics = syncedLyrics,
                            onSeekTo = { target ->
                                val safe = target.coerceIn(0.0, totalDurationSeconds)
                                currentPlayTimeSeconds = safe
                                smoothPlayTimeSeconds = safe
                                lastAudioClockSeconds = safe
                                lastAudioClockNanos = System.nanoTime()
                                StudioAudioPlayer.seek(safe)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )

                        // B. CONTROL & FILTER STRIP (As circled in orange by user!)
                        StudioControlStrip(
                            chordFilterMode = chordFilterMode,
                            onChordFilterChange = { chordFilterMode = it },
                            showPositions = showPositions,
                            onTogglePositions = { showPositions = it },
                            capoPosition = capoPosition,
                            onCapoChange = { capoPosition = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // C. PARTICULAR SEGMENT CHORD FRETBOARD CARDS (ss2)
                        StudioChordShowcase(
                            segmentChords = segmentEvents,
                            activeChordName = activeChordName,
                            showPositions = showPositions,
                            onSeekToTimestamp = { target ->
                                StudioAudioPlayer.seek(target)
                                currentPlayTimeSeconds = target
                                smoothPlayTimeSeconds = target
                                lastAudioClockSeconds = target
                                lastAudioClockNanos = System.nanoTime()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            } else {
                // Classic Tab View
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Surface(
                        color = ColorSurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ColorCardBorder),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                            Text(
                                text = dynamicFormattedTab,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.5.sp,
                                lineHeight = 22.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 3. BOTTOM STUDIO AUDIO CONSOLE / DOCK
            // ==========================================
            StudioBottomConsole(
                songTitle = songTitle,
                artistName = artistName,
                isPlaying = isPlaying,
                onTogglePlay = {
                    if (isPlaying) {
                        StudioAudioPlayer.pause()
                        isPlaying = false
                    } else {
                        if (currentPlayTimeSeconds >= 0.0) {
                            StudioAudioPlayer.play(currentPlayTimeSeconds)
                        }
                        isPlaying = true
                        lastAudioClockSeconds = currentPlayTimeSeconds
                        lastAudioClockNanos = System.nanoTime()
                    }
                },
                currentTimeSeconds = if (isPlaying) smoothPlayTimeSeconds else currentPlayTimeSeconds,
                totalDurationSeconds = totalDurationSeconds,
                onSeek = { target ->
                    val safe = target.coerceIn(0.0, totalDurationSeconds)
                    currentPlayTimeSeconds = safe
                    smoothPlayTimeSeconds = safe
                    lastAudioClockSeconds = safe
                    lastAudioClockNanos = System.nanoTime()
                    StudioAudioPlayer.seek(safe)
                },
                onSkipBackward = {
                    val target = (currentPlayTimeSeconds - 5.0).coerceAtLeast(0.0)
                    StudioAudioPlayer.seek(target)
                    currentPlayTimeSeconds = target
                    smoothPlayTimeSeconds = target
                    lastAudioClockSeconds = target
                    lastAudioClockNanos = System.nanoTime()
                },
                onSkipForward = {
                    val target = (currentPlayTimeSeconds + 5.0).coerceAtMost(totalDurationSeconds)
                    StudioAudioPlayer.seek(target)
                    currentPlayTimeSeconds = target
                    smoothPlayTimeSeconds = target
                    lastAudioClockSeconds = target
                    lastAudioClockNanos = System.nanoTime()
                },
                pitchShift = pitchShiftSteps,
                onPitchChange = {
                    pitchShiftSteps = it
                    StudioAudioPlayer.pitchShift = it
                },
                speed = speedMultiplier,
                onSpeedChange = { speedMultiplier = it },
                isLooping = isLooping,
                onToggleLoop = {
                    isLooping = !isLooping
                    StudioAudioPlayer.isLooping = isLooping
                },
                volume = volumeLevel,
                onVolumeChange = {
                    volumeLevel = it
                    StudioAudioPlayer.volume = it
                },
                isMuted = isMuted,
                onToggleMute = {
                    isMuted = !isMuted
                    StudioAudioPlayer.isMuted = isMuted
                },
                onOpenInViewer = {
                    val newId = "studio_" + UUID.randomUUID().toString().take(8)
                    val newTab = Tab(
                        tabId = newId,
                        songName = songTitle,
                        artistName = artistName,
                        tonalityName = effectiveKey,
                        tuning = "E A D G B E",
                        capo = capoPosition,
                        type = "Chords",
                        content = dynamicFormattedTab,
                        contributorUserName = "AI Audio Studio"
                    )
                    coroutineScope.launch {
                        DesktopDatabase.instance.saveTab(newTab)
                        onOpenInTabViewer(newId)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ==========================================
        // 4. IMPORT & RECORD DIALOG (When User Clicks Import)
        // ==========================================
        if (showImportDialog) {
            Dialog(onDismissRequest = { if (!isAnalyzing) showImportDialog = false }) {
                StudioImportModal(
                    selectedFile = selectedFile,
                    onSelectFile = { file ->
                        selectedFile = file
                        selectedDemo = null
                        micRecordedSamples = null
                        val (resolvedTitle, resolvedArtist) = resolveTitleAndArtist(file)
                        songTitle = resolvedTitle
                        artistName = resolvedArtist
                        customLyrics = ""
                        syncedLyrics = emptyList()
                        showImportDialog = false
                        handleStartAnalysis(file)
                    },
                    isRecordingMic = isRecordingMic,
                    micRecordingTime = micRecordingTimeSeconds,
                    onToggleMic = {
                        if (isRecordingMic) {
                            isRecordingMic = false
                            micTargetLine?.stop()
                            micTargetLine?.close()
                            micTargetLine = null
                        } else {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val format = AudioFormat(16000f, 16, 1, true, false)
                                    val info = DataLine.Info(TargetDataLine::class.java, format)
                                    val line = AudioSystem.getLine(info) as TargetDataLine
                                    line.open(format)
                                    line.start()
                                    micTargetLine = line
                                    isRecordingMic = true
                                    micRecordingTimeSeconds = 0

                                    val out = ByteArrayOutputStream()
                                    val buf = ByteArray(2048)

                                    launch {
                                        while (isRecordingMic) {
                                            delay(1000L)
                                            micRecordingTimeSeconds++
                                        }
                                    }

                                    while (isRecordingMic) {
                                        val c = line.read(buf, 0, buf.size)
                                        if (c > 0) out.write(buf, 0, c)
                                    }

                                    val raw = out.toByteArray()
                                    val fl = FloatArray(raw.size / 2)
                                    for (i in fl.indices) {
                                        val low = raw[i * 2].toInt() and 0xFF
                                        val high = raw[i * 2 + 1].toInt()
                                        fl[i] = ((high shl 8) or low) / 32768.0f
                                    }
                                    micRecordedSamples = fl
                                    selectedFile = null
                                    selectedDemo = null
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                    isRecordingMic = false
                                }
                            }
                        }
                    },
                    isAnalyzing = isAnalyzing,
                    analysisProgress = analysisProgress,
                    stepMessage = currentStepMessage,
                    errorMessage = errorMessage,
                    onDismissError = { errorMessage = null },
                    onStartAnalysis = { handleStartAnalysis() },
                    onClose = { showImportDialog = false }
                )
            }
        }

        if (showLyricsDrawer) {
            StudioLyricsModal(
                songTitle = songTitle,
                artistName = artistName,
                lyricsText = customLyrics,
                syncedLyrics = syncedLyrics,
                currentTimeSeconds = if (isPlaying) smoothPlayTimeSeconds else currentPlayTimeSeconds,
                onSeekTo = { target ->
                    StudioAudioPlayer.seek(target)
                    currentPlayTimeSeconds = target
                    smoothPlayTimeSeconds = target
                    lastAudioClockSeconds = target
                    lastAudioClockNanos = System.nanoTime()
                },
                onUpdateLyrics = { newLyrics ->
                    customLyrics = newLyrics
                    syncedLyrics = LyricsExtractor.parseLrc(newLyrics)
                },
                onApplySearchResult = { res ->
                    customLyrics = res.toLrcOrPlainText()
                    syncedLyrics = res.syncedLyrics
                    if (!res.detectedTitle.isNullOrBlank()) songTitle = res.detectedTitle
                    if (!res.detectedArtist.isNullOrBlank()) artistName = res.detectedArtist
                },
                onFetchOnline = { searchT, searchA ->
                    coroutineScope.launch {
                        isFetchingLyricsOnline = true
                        val res = withContext(Dispatchers.IO) {
                            LyricsExtractor.fetchOnlineLyrics(searchT, searchA, totalDurationSeconds)
                        }
                        isFetchingLyricsOnline = false
                        if (res != null) {
                            customLyrics = res.toLrcOrPlainText()
                            syncedLyrics = res.syncedLyrics
                            if (!res.detectedTitle.isNullOrBlank()) songTitle = res.detectedTitle
                            if (!res.detectedArtist.isNullOrBlank()) artistName = res.detectedArtist
                        } else {
                            errorMessage = "No online lyrics found for '$searchT'. You can transcribe vocals directly with AI or paste lyrics in Edit."
                        }
                    }
                },
                isFetchingOnline = isFetchingLyricsOnline,
                onTranscribeAi = { model, lang ->
                    coroutineScope.launch {
                        isTranscribingLyricsAi = true
                        aiTranscribeErrorText = null
                        aiLyricsProgressText = "Initializing Whisper AI..."
                        val res = withContext(Dispatchers.IO) {
                            LyricsExtractor.transcribeWithAi(
                                audioFile = selectedFile,
                                audioSamples = currentAudioSamples ?: micRecordedSamples,
                                modelName = model,
                                language = lang,
                                onProgress = { p ->
                                    aiLyricsProgressText = p
                                }
                            )
                        }
                        isTranscribingLyricsAi = false
                        if (res != null) {
                            customLyrics = res.toLrcOrPlainText()
                            syncedLyrics = res.syncedLyrics
                            aiTranscribeErrorText = null
                        } else {
                            val detail = LyricsExtractor.lastError
                            aiTranscribeErrorText = if (!detail.isNullOrBlank()) detail
                                else "No lyrics could be extracted. The track may be instrumental or audio quality is low."
                        }
                    }
                },
                isTranscribingAi = isTranscribingLyricsAi,
                aiProgressText = aiLyricsProgressText,
                aiTranscribeError = aiTranscribeErrorText,
                audioSourceAvailable = selectedFile != null || currentAudioSamples != null || micRecordedSamples != null,
                onClose = { showLyricsDrawer = false; aiTranscribeErrorText = null }
            )
        }
    }
}

// ==========================================
// SUB-COMPONENTS FOR STUDIO WORKSTATION
// ==========================================

/**
 * Idle Blank Workstation State when no audio has been uploaded yet
 */
@Composable
fun StudioBlankWorkstation(
    isAnalyzing: Boolean,
    analysisProgress: Float,
    currentStepMessage: String,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {},
    onUploadClick: () -> Unit,
    onChooseDemoOrRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceDark),
        border = BorderStroke(1.dp, ColorCardBorder),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isAnalyzing) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.widthIn(max = 480.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2438)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { analysisProgress },
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF262C3F),
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(54.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "Analyzing Audio & Extracting Chords...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = currentStepMessage,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { analysisProgress },
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF1E2333),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "${(analysisProgress * 100).toInt()}% Complete",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.widthIn(max = 520.dp)
                ) {
                    // Glowing Audio Upload Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            )
                            .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Studio Audio Workstation",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Upload an audio file (MP3, WAV) or record from microphone.\nThe on-device AI will extract the real audio waveform, detect tempo and key, and generate synchronous chord progressions.",
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    if (errorMessage != null) {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = Color(0xFF7F1D1D).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = errorMessage,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFEE2E2),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onDismissError, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onUploadClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorAccentBlue),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Upload Audio File", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onChooseDemoOrRecord,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ColorCardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = ColorSurfaceCardElevated),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = ColorAccentBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Record from Microphone", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Color(0xFFCBD5E1))
                        }
                    }
                }
            }
        }
    }
}

val MetronomeIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Metronome",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Metronome body trapezoid & base
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8f, 20.5f)
            lineTo(10.5f, 4f)
            lineTo(13.5f, 4f)
            lineTo(16f, 20.5f)
            close()
            // Base feet line
            moveTo(5.5f, 20.5f)
            lineTo(18.5f, 20.5f)
            // Pendulum needle
            moveTo(12f, 17.5f)
            lineTo(15.8f, 6.5f)
        }
        // Bob weight on the pendulum needle
        path(
            fill = SolidColor(Color.White)
        ) {
            moveTo(13.6f, 8.8f)
            lineTo(16.6f, 9.8f)
            lineTo(15.8f, 12f)
            lineTo(12.8f, 11f)
            close()
        }
    }.build()
}

@Composable
fun StudioMetricPill(
    icon: ImageVector? = null,
    label: String,
    badgeColor: Color,
    iconTint: Color = badgeColor
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ColorSurfaceCardElevated,
        border = BorderStroke(1.dp, ColorCardBorder),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                color = ColorTextPrimary
            )
        }
    }
}

/**
 * High-Density Scrolling Audio Waveform with Moving Playhead and Floating Chord Pills (ss1)
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StudioWaveformTimeline(
    currentTimeSeconds: Double,
    totalDurationSeconds: Double,
    waveformEnvelope: FloatArray,
    chordTimeline: List<ChordEvent>,
    activeChordName: String,
    syncedLyrics: List<SyncedLyricLine> = emptyList(),
    onSeekTo: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayTimeState by rememberUpdatedState(currentTimeSeconds)
    val onSeekState by rememberUpdatedState(onSeekTo)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceDark),
        border = BorderStroke(1.dp, ColorCardBorder),
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val totalHeightPx = constraints.maxHeight.toFloat()
            val centerX = totalWidthPx / 2f

            // 8 seconds visible window matching ss1
            val visibleWindowSeconds = 8.0
            val pixelsPerSecond = totalWidthPx / visibleWindowSeconds

            // 1. Time Ruler Bar at the Top (Always starts at 00:00)
            val maxRulerSec = ceil(totalDurationSeconds).toInt()
            val startSec = floor(currentTimeSeconds - visibleWindowSeconds / 2.0).toInt().coerceAtLeast(0)
            val endSec = ceil(currentTimeSeconds + visibleWindowSeconds / 2.0 + 1.0).toInt().coerceAtMost(maxRulerSec)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .padding(top = 6.dp)
            ) {
                for (s in startSec..endSec) {
                    if (s < 0) continue
                    val secX = centerX + ((s - currentTimeSeconds) * pixelsPerSecond).toFloat()
                    if (secX in -40f..(totalWidthPx + 40f)) {
                        Text(
                            text = "| ${formatSecondsToRuler(s.toDouble())}",
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.offset { IntOffset(secX.toInt(), 0) }
                        )
                    }
                }
            }

            // 2. Waveform Canvas with Central Playhead, Click-and-Hold Drag to Scroll, and Tap-to-Seek
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 26.dp, bottom = 10.dp)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .onPointerEvent(PointerEventType.Scroll) { event ->
                        val scrollDelta = event.changes.firstOrNull()?.scrollDelta
                        if (scrollDelta != null) {
                            val dx = if (kotlin.math.abs(scrollDelta.x) > 0.001f) scrollDelta.x else scrollDelta.y
                            val deltaSec = (dx * 0.35).toDouble()
                            val target = (currentPlayTimeState + deltaSec).coerceIn(0.0, totalDurationSeconds)
                            onSeekState(target)
                        }
                    }
                    .pointerInput(pixelsPerSecond, centerX, totalDurationSeconds) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downPos = down.position
                            var totalDragDistance = 0f
                            var lastPos = downPos
                            var isDragging = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!change.pressed) {
                                    // Pointer released
                                    if (!isDragging && totalDragDistance < 8f) {
                                        // Tap / click without drag: seek directly to clicked time
                                        val clickedTime = (currentPlayTimeState + (downPos.x - centerX) / pixelsPerSecond).coerceIn(0.0, totalDurationSeconds)
                                        onSeekState(clickedTime)
                                    }
                                    break
                                }

                                val deltaX = change.position.x - lastPos.x
                                totalDragDistance += kotlin.math.abs(deltaX)

                                if (totalDragDistance >= 5f) {
                                    isDragging = true
                                    change.consume()
                                    val deltaSeconds = (-deltaX / pixelsPerSecond).toDouble()
                                    val newTime = (currentPlayTimeState + deltaSeconds).coerceIn(0.0, totalDurationSeconds)
                                    onSeekState(newTime)
                                    lastPos = change.position
                                }
                            }
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val midY = canvasHeight / 2f
                val barSpacingPx = 7.dp.toPx()
                val barWidth = 3.5.dp.toPx()

                val barIntervalSeconds = if (pixelsPerSecond > 0f) (barSpacingPx.toDouble() / pixelsPerSecond) else 0.05
                val minBar = floor((currentTimeSeconds - (centerX / pixelsPerSecond)) / barIntervalSeconds).toInt().coerceAtLeast(0)
                val maxBar = ceil((currentTimeSeconds + ((canvasWidth - centerX) / pixelsPerSecond)) / barIntervalSeconds).toInt().coerceAtMost(((totalDurationSeconds) / barIntervalSeconds).toInt())

                for (b in minBar..maxBar) {
                    val barTime = b * barIntervalSeconds
                    val barX = centerX + ((barTime - currentTimeSeconds) * pixelsPerSecond).toFloat()

                    if (barX in -barSpacingPx..(canvasWidth + barSpacingPx)) {
                        if (barTime in 0.0..totalDurationSeconds) {
                            val frac = if (totalDurationSeconds > 0) (barTime / totalDurationSeconds).coerceIn(0.0, 1.0) else 0.0
                            val envIdx = if (waveformEnvelope.isNotEmpty()) {
                                (frac * (waveformEnvelope.size - 1)).toInt().coerceIn(0, waveformEnvelope.size - 1)
                            } else 0
                            val rawAmp = if (waveformEnvelope.isNotEmpty()) waveformEnvelope[envIdx] else 0.25f
                            val ampHeight = (rawAmp * canvasHeight * 0.72f).coerceIn(6.dp.toPx(), canvasHeight * 0.88f)

                            val isPlayed = barX <= centerX
                            val barColor = if (isPlayed) Color(0xFFF1F5F9) else Color(0xFF334155).copy(alpha = 0.65f)

                            drawLine(
                                color = barColor,
                                start = Offset(barX, midY - ampHeight / 2f),
                                end = Offset(barX, midY + ampHeight / 2f),
                                strokeWidth = barWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }

                // Green/Cyan Dashed Line for 0.0s Song Start Boundary
                val zeroStartX = centerX + ((0.0 - currentTimeSeconds) * pixelsPerSecond).toFloat()
                if (zeroStartX in 0f..canvasWidth) {
                    drawLine(
                        color = Color(0xFF22C55E).copy(alpha = 0.85f),
                        start = Offset(zeroStartX, 0f),
                        end = Offset(zeroStartX, canvasHeight),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }

                // Vertical Chord Change Measure Dividers
                for (chord in chordTimeline) {
                    val chordStartX = centerX + ((chord.timestampSeconds - currentTimeSeconds) * pixelsPerSecond).toFloat()
                    if (chordStartX in 0f..canvasWidth) {
                        drawLine(
                            color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                            start = Offset(chordStartX, 0f),
                            end = Offset(chordStartX, canvasHeight),
                            strokeWidth = 1.2.dp.toPx()
                        )
                    }
                }

                // Vertical Cyan Playhead Line in Center (Matching ss1)
                drawLine(
                    color = Color(0xFF38BDF8),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, canvasHeight),
                    strokeWidth = 2.5.dp.toPx()
                )
            }

            // 3. Floating Chord Pill Blocks along Timeline (Matching ss1: A# 1, D# 3, F 5, Dm 7)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                chordTimeline.forEachIndexed { index, chord ->
                    if (chord.chordName.isNotBlank()) {
                        val startX = centerX + ((chord.timestampSeconds - currentTimeSeconds) * pixelsPerSecond).toFloat()
                        val chordDurationPx = (chord.durationSeconds * pixelsPerSecond).toFloat()
                        val endX = startX + chordDurationPx

                        // Visible whenever the chord's duration spans into the viewport
                        if (endX >= -40f && startX <= (totalWidthPx + 40f)) {
                            val isActive = currentTimeSeconds >= chord.timestampSeconds && currentTimeSeconds < (chord.timestampSeconds + chord.durationSeconds)

                            // Keep the chord pill centered within the visible part of its duration so it never disappears off-screen while playing
                            val visibleStart = max(startX, 0f)
                            val visibleEnd = min(endX, totalWidthPx)
                            val pillX = if (chordDurationPx <= 90f) {
                                startX + (chordDurationPx / 2f) - 38f
                            } else {
                                ((visibleStart + visibleEnd) / 2f) - 38f
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isActive) Color(0xFFFEF08A) else Color(0xFFFEF08A).copy(alpha = 0.78f),
                                border = if (isActive) BorderStroke(2.dp, Color(0xFFF59E0B)) else BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.45f)),
                                shadowElevation = if (isActive) 6.dp else 2.dp,
                                onClick = { onSeekTo(chord.timestampSeconds) },
                                modifier = Modifier
                                    .offset { IntOffset(pillX.toInt(), 0) }
                                    .width(76.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = chord.chordName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${chord.beatNumber}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Floating Active Chord Badge in Bottom Left (Matching ss1)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF141926).copy(alpha = 0.95f),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                        Spacer(Modifier.width(8.dp))
                        val badgeLabel = if (activeChordName.isNotBlank()) {
                            activeChordName
                        } else {
                            "Ready"
                        }
                        Text(
                            text = badgeLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFFF1F5F9)
                        )
                    }
                }
            }

            // 4.5. Floating Active Lyric Line (When Synced Lyrics Available)
            if (syncedLyrics.isNotEmpty()) {
                val currentLine = syncedLyrics.lastOrNull { it.timestampSeconds <= currentTimeSeconds }
                if (currentLine != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.90f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = currentLine.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF1F5F9),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 5. MIDIFY Badge in Bottom Right (Matching ss1)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE2E8F0),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = "MIDIFY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Control & Filter Strip (Highlighted with orange circle in user's screenshot)
 */
@Composable
fun StudioControlStrip(
    chordFilterMode: String,
    onChordFilterChange: (String) -> Unit,
    showPositions: Boolean,
    onTogglePositions: (Boolean) -> Unit,
    capoPosition: Int,
    onCapoChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF141724),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF22273A)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Chords Selector: [Basic] [All] (As circled by user)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Chords ⓘ",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(end = 12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF161926),
                    border = BorderStroke(1.dp, Color(0xFF262C3F)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isBasic = chordFilterMode == "Basic"
                        Surface(
                            onClick = { onChordFilterChange("Basic") },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isBasic) Color.White else Color.Transparent,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight().padding(horizontal = 14.dp)) {
                                Text(
                                    text = "Basic",
                                    fontSize = 12.sp,
                                    fontWeight = if (isBasic) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isBasic) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                )
                            }
                        }

                        val isAll = chordFilterMode == "All"
                        Surface(
                            onClick = { onChordFilterChange("All") },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAll) Color.White else Color.Transparent,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp)) {
                                Text(
                                    text = "All",
                                    fontSize = 12.sp,
                                    fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAll) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            VerticalDivider(modifier = Modifier.height(28.dp), color = Color(0xFF262C3F))

            // 2. Positions Toggle Switch (As circled by user)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Positions ⓘ",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(end = 12.dp)
                )

                Switch(
                    checked = showPositions,
                    onCheckedChange = onTogglePositions,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF22C55E),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF1E2334)
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }

            VerticalDivider(modifier = Modifier.height(28.dp), color = Color(0xFF262C3F))

            // 3. Capo Controller (As circled by user)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Capo",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(end = 10.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2334))
                            .clickable { if (capoPosition > -12) onCapoChange(capoPosition - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease Capo",
                            tint = if (capoPosition > -12) Color(0xFFCBD5E1) else Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E2334),
                        border = BorderStroke(1.dp, if (capoPosition < 0) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color(0xFF2B3349)),
                        modifier = Modifier.height(34.dp).widthIn(min = 38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp)) {
                            Text(
                                text = if (capoPosition > 0) "+$capoPosition" else "$capoPosition",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (capoPosition < 0) Color(0xFF38BDF8) else Color(0xFFF1F5F9)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2334))
                            .clickable { if (capoPosition < 12) onCapoChange(capoPosition + 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase Capo",
                            tint = if (capoPosition < 12) Color(0xFFCBD5E1) else Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Fretboard Chord Cards Stage (ss2)
 * Displays the current segment chord (with cyan glow) and subsequent upcoming segment chords
 */
@Composable
fun StudioChordShowcase(
    segmentChords: List<ChordEvent>,
    activeChordName: String,
    showPositions: Boolean,
    onSeekToTimestamp: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        segmentChords.take(3).forEachIndexed { index, chordEvent ->
            val isCurrent = index == 0
            StudioChordCard(
                chordName = chordEvent.chordName,
                isCurrent = isCurrent,
                showPositions = showPositions,
                onClick = { onSeekToTimestamp(chordEvent.timestampSeconds) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

/**
 * Individual Studio Chord Card matching ss2
 */
@Composable
fun StudioChordCard(
    chordName: String,
    isCurrent: Boolean,
    showPositions: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shapes = remember(chordName) { TabTransposer.getGuitarChordShapes(chordName) }
    var voicingIndex by remember(chordName) { mutableIntStateOf(0) }
    val shape = shapes.getOrElse(voicingIndex.coerceIn(0, shapes.size - 1)) {
        ChordShape(chordName, "Guitar", listOf(0, 2, 2, 1, 0, 0))
    }

    val baseFret = if (showPositions) shape.baseFret.coerceAtLeast(1) else 1
    val stringNotes = remember(shape, chordName) {
        TabTransposer.getGuitarStringNotes(shape, useFlats = chordName.contains("b"))
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFF131D2E) else Color(0xFF131722)
        ),
        border = BorderStroke(
            if (isCurrent) 1.5.dp else 1.dp,
            if (isCurrent) Color(0xFF38BDF8) else Color(0xFF222738)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Header: Chord Name in Cyan + Fret Number on Right (Matching ss2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chordName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF38BDF8) // Mint / Cyan
                )

                if (baseFret > 1) {
                    Text(
                        text = "$baseFret",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // 2. Open / Mute Indicators (X / O)
            val frets = shape.frets.take(6)
            Row(
                modifier = Modifier.width(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                frets.forEach { f ->
                    Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = when (f) {
                                0 -> "o"
                                -1 -> "✕"
                                else -> ""
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (f == 0) Color(0xFFCBD5E1) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // 3. Fretboard Diagram Grid with Cream Dots and Barre (Matching ss2)
            StudioFretboardCanvas(
                shape = shape,
                baseFret = baseFret,
                modifier = Modifier
                    .width(190.dp)
                    .height(170.dp)
            )

            // 4. String Note Names at the Bottom (Matching ss2: A# F A# D F A#)
            Text(
                text = stringNotes.joinToString("   "),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            // 5. Pagination Voicing Dots at Bottom (Matching ss2: • • • • •)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalVoicings = shapes.size.coerceAtMost(5)
                for (i in 0 until totalVoicings) {
                    val isSelected = i == voicingIndex
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 6.dp else 4.5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color(0xFF475569))
                            .clickable { voicingIndex = i }
                    )
                }
            }
        }
    }
}

/**
 * Fretboard Canvas drawing authentic strings, frets, finger dots and cream barre line (Matching ss2)
 */
@Composable
fun StudioFretboardCanvas(
    shape: ChordShape,
    baseFret: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stringCount = 6
        val fretCount = 5

        val stringSpacing = width / (stringCount - 1)
        val fretSpacing = height / fretCount

        // 1. Horizontal Fret Wire Lines
        for (f in 0..fretCount) {
            val y = f * fretSpacing
            val isNut = f == 0 && baseFret == 1
            drawLine(
                color = if (isNut) Color(0xFFF1F5F9) else Color(0xFF475569),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = if (isNut) 3.5.dp.toPx() else 1.2.dp.toPx()
            )
        }

        // 2. Vertical String Lines
        for (s in 0 until stringCount) {
            val x = s * stringSpacing
            drawLine(
                color = Color(0xFF64748B),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        // 3. Barre Pill (Cream Yellow barre line across barred strings matching ss2)
        if (shape.barres.isNotEmpty()) {
            val barreFret = shape.barres.first()
            val relativeBarreFret = if (baseFret > 1) (barreFret - baseFret + 1) else barreFret
            if (relativeBarreFret in 1..fretCount) {
                val barreY = (relativeBarreFret - 0.5f) * fretSpacing
                drawRoundRect(
                    color = Color(0xFFFDE68A), // Cream yellow
                    topLeft = Offset(0f, barreY - 6.5.dp.toPx()),
                    size = Size(width, 13.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.5.dp.toPx())
                )
            }
        }

        // 4. Finger Dots (Cream Yellow dots matching ss2)
        val frets = shape.frets.take(stringCount)

        for (s in 0 until stringCount) {
            val f = frets[s]
            if (f > 0) {
                val relativeFret = if (baseFret > 1) (f - baseFret + 1) else f
                if (relativeFret in 1..fretCount) {
                    val dotX = s * stringSpacing
                    val dotY = (relativeFret - 0.5f) * fretSpacing

                    drawCircle(
                        color = Color(0xFFFDE68A), // Cream dot
                        radius = 8.5.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }
            }
        }
    }
}

/**
 * PC-Optimized Bottom Transport Console / Dock
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioBottomConsole(
    songTitle: String,
    artistName: String,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    currentTimeSeconds: Double,
    totalDurationSeconds: Double,
    onSeek: (Double) -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    pitchShift: Int,
    onPitchChange: (Int) -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    isLooping: Boolean,
    onToggleLoop: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onOpenInViewer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ColorSurfaceDark,
        border = BorderStroke(1.dp, ColorCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            val isReady = totalDurationSeconds > 0.0

            // Scrubber Slider Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isReady) formatSecondsToRuler(currentTimeSeconds) else "00:00",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    color = ColorTextSecondary
                )

                Slider(
                    value = if (isReady) currentTimeSeconds.toFloat() else 0f,
                    onValueChange = { if (isReady) onSeek(it.toDouble()) },
                    valueRange = 0f..(if (isReady) totalDurationSeconds.toFloat() else 1f),
                    enabled = isReady,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(if (isReady) ColorAccentBlue else Color(0xFF475569), CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    },
                    track = { _ ->
                        val fraction = if (totalDurationSeconds > 0) ((currentTimeSeconds / totalDurationSeconds).toFloat()).coerceIn(0.0001f, 1f) else 0.0001f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(ColorCardBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .background(if (isReady) ColorAccentBlue else Color(0xFF475569))
                            )
                        }
                    }
                )

                Text(
                    text = if (isReady) formatSecondsToRuler(totalDurationSeconds) else "00:00",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    color = ColorTextSecondary
                )
            }

            Spacer(Modifier.height(8.dp))

            // Main Dock Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Album Art & Track Info (Matching Screenshot)
                Row(
                    modifier = Modifier.widthIn(max = 280.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isReady) Brush.linearGradient(listOf(Color(0xFFEA580C), Color(0xFF2563EB)))
                                else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isReady) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isReady) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = if (songTitle.isNotBlank()) songTitle else "No Audio Loaded",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (songTitle.isNotBlank()) Color(0xFFF1F5F9) else Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isReady) {
                                val subtitle = if (artistName.isNotBlank() && artistName != "Detected Audio") {
                                    artistName
                                } else {
                                    "Local Audio File"
                                }
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            } else {
                                Text("Upload audio above to analyze chords", fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Center: Transport Buttons (Skip, Play/Pause, Skip)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconButton(onClick = onSkipBackward, enabled = isReady) {
                        Icon(
                            Icons.Default.FastRewind,
                            contentDescription = "Seek -5s",
                            tint = if (isReady) Color(0xFFCBD5E1) else Color(0xFF475569),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Surface(
                        onClick = onTogglePlay,
                        shape = CircleShape,
                        color = if (isReady) Color(0xFF2563EB) else Color(0xFF1E2638),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = if (isReady) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(onClick = onSkipForward, enabled = isReady) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = "Seek +5s",
                            tint = if (isReady) Color(0xFFCBD5E1) else Color(0xFF475569),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Right: Pitch, Speed, Loop, Volume & Viewer Action (Matching Screenshot)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pitch Tile: [- Pitch +0 +]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161926),
                        border = BorderStroke(1.dp, Color(0xFF262C3F)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxHeight().padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onPitchChange(pitchShift - 1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease Pitch",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Box(
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Pitch ${if (pitchShift >= 0) "+$pitchShift" else pitchShift}",
                                    fontSize = 11.5.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFCBD5E1),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onPitchChange(pitchShift + 1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Pitch",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    // Speed Tile: [Speed x1.00]
                    Surface(
                        onClick = {
                            val nextSpeed = when (speed) {
                                0.75f -> 1.0f
                                1.0f -> 1.25f
                                else -> 0.75f
                            }
                            onSpeedChange(nextSpeed)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161926),
                        border = BorderStroke(1.dp, Color(0xFF262C3F)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "Speed x${"%.2f".format(speed)}",
                                fontSize = 11.5.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Loop Tile: [Loop]
                    Surface(
                        onClick = onToggleLoop,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLooping) Color(0xFF2563EB).copy(alpha = 0.25f) else Color(0xFF161926),
                        border = BorderStroke(1.dp, if (isLooping) Color(0xFF3B82F6) else Color(0xFF262C3F)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxHeight().padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "Loop",
                                fontSize = 11.5.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLooping) Color(0xFF60A5FA) else Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Volume Icon & Slider
                    IconButton(onClick = onToggleMute, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (isMuted || volume == 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Full Tab Viewer Button
                    Button(
                        onClick = onOpenInViewer,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 115.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tab Viewer", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Modal to Import Any Audio File or Record Audio via Microphone
 */
@Composable
fun StudioImportModal(
    selectedFile: File?,
    onSelectFile: (File) -> Unit,
    isRecordingMic: Boolean,
    micRecordingTime: Int,
    onToggleMic: () -> Unit,
    isAnalyzing: Boolean,
    analysisProgress: Float,
    stepMessage: String,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onStartAnalysis: () -> Unit,
    onClose: () -> Unit
) {
    val modalAudioPickerLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("mp3", "wav", "flac", "m4a", "aac", "ogg", "aif", "aiff", "au")),
        title = "Select Audio File (MP3 / WAV / FLAC / M4A)"
    ) { platformFile ->
        platformFile?.let { pFile ->
            onSelectFile(pFile.file)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121520)),
        border = BorderStroke(1.dp, Color(0xFF23283B)),
        modifier = Modifier.width(520.dp).padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Add Audio to Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFF1F5F9))
                    Spacer(Modifier.height(2.dp))
                    Text("Select a file or record singing/instruments live.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }

            Spacer(Modifier.height(18.dp))

            // Choose File or Mic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    onClick = {
                        modalAudioPickerLauncher.launch()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFile != null) Color(0xFF1E2638) else Color(0xFF161A26)
                    ),
                    border = BorderStroke(1.dp, if (selectedFile != null) Color(0xFF3B82F6) else Color(0xFF262C3F)),
                    modifier = Modifier.weight(1f).height(100.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(26.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = selectedFile?.name ?: "Browse Audio File",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = Color(0xFFF1F5F9),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "MP3, WAV, FLAC, M4A",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Card(
                    onClick = onToggleMic,
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecordingMic) Color(0xFF7F1D1D) else Color(0xFF161A26)
                    ),
                    border = BorderStroke(1.dp, if (isRecordingMic) Color(0xFFEF4444) else Color(0xFF262C3F)),
                    modifier = Modifier.weight(1f).height(100.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isRecordingMic) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isRecordingMic) Color(0xFFF87171) else Color(0xFF34D399),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (isRecordingMic) "Recording ${micRecordingTime}s" else "Record with Mic",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = Color(0xFFF1F5F9)
                        )
                        Text(
                            text = if (isRecordingMic) "Click to Stop" else "Live Audio Input",
                            fontSize = 10.5.sp,
                            color = if (isRecordingMic) Color(0xFFFCA5A5) else Color(0xFF64748B)
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = errorMessage, fontSize = 12.sp, color = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismissError, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Run Button
            Button(
                onClick = onStartAnalysis,
                enabled = !isAnalyzing && !isRecordingMic,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stepMessage, fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Analyze & Open in Studio Workstation", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }
    }
}

private fun formatSecondsToRuler(seconds: Double): String {
    val totalSec = kotlin.math.round(seconds.coerceAtLeast(0.0)).toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%02d:%02d".format(mins, secs)
}

/**
 * Modern Interactive Lyrics Studio Modal (Live Synced Karaoke, Plain Text Editor, Online Search, AI Transcribe)
 */
@Composable
fun StudioLyricsModal(
    songTitle: String,
    artistName: String,
    lyricsText: String,
    syncedLyrics: List<SyncedLyricLine>,
    currentTimeSeconds: Double,
    onSeekTo: (Double) -> Unit,
    onUpdateLyrics: (String) -> Unit,
    onApplySearchResult: (ExtractedLyricsResult) -> Unit = {},
    onFetchOnline: (title: String, artist: String) -> Unit,
    isFetchingOnline: Boolean,
    onTranscribeAi: (modelName: String, language: String) -> Unit,
    isTranscribingAi: Boolean,
    aiProgressText: String,
    aiTranscribeError: String? = null,
    audioSourceAvailable: Boolean,
    onClose: () -> Unit
) {
    var activeTab by remember(syncedLyrics.isNotEmpty(), lyricsText.isNotBlank()) {
        mutableStateOf(if (syncedLyrics.isNotEmpty()) "SYNCED" else if (lyricsText.isNotBlank()) "PLAIN" else "AI")
    }
    var editableLyrics by remember(lyricsText) {
        mutableStateOf(LyricsExtractor.cleanAndDeduplicateLyrics(lyricsText))
    }
    var searchTitle by remember(songTitle) { mutableStateOf(songTitle) }
    var searchArtist by remember(artistName) {
        mutableStateOf(if (artistName != "Local Audio File" && artistName != "Detected Audio") artistName else "")
    }
    var selectedModel by remember { mutableStateOf("tiny") }
    var selectedLanguage by remember { mutableStateOf("auto") }
    var aiTabError by remember(aiTranscribeError) { mutableStateOf(aiTranscribeError) }
    var cleanNoticeMessage by remember { mutableStateOf<String?>(null) }
    var onlineSearchResults by remember { mutableStateOf<List<ExtractedLyricsResult>>(emptyList()) }
    var onlineSearchMessage by remember { mutableStateOf<String?>(null) }
    var isSearchingOnlineLocal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Sync external error into tab error state
    LaunchedEffect(aiTranscribeError) {
        if (aiTranscribeError != null) aiTabError = aiTranscribeError
    }
    // Clear AI error when transcription starts
    LaunchedEffect(isTranscribingAi) {
        if (isTranscribingAi) aiTabError = null
    }

    val listState = rememberLazyListState()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorSurfaceDark),
            border = BorderStroke(1.dp, ColorCardBorder),
            modifier = Modifier
                .widthIn(min = 640.dp, max = 760.dp)
                .heightIn(min = 460.dp, max = 580.dp)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
                // Header (compact fixed height, no weights pushing down)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF06B6D4)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.Center) {
                            Text("Lyrics Studio", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFF1F5F9))
                            val sub = if (songTitle.isNotBlank()) "$songTitle • ${artistName.ifBlank { "Detected Audio" }}" else "No Audio Loaded"
                            Text(sub, fontSize = 11.5.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Status badge with centered dot shape
                        val badgeBg = when {
                            isTranscribingAi -> Color(0xFF4F46E5).copy(alpha = 0.25f)
                            syncedLyrics.isNotEmpty() -> Color(0xFF059669).copy(alpha = 0.2f)
                            lyricsText.isNotBlank() -> Color(0xFF2563EB).copy(alpha = 0.2f)
                            else -> Color(0xFF334155).copy(alpha = 0.3f)
                        }
                        val badgeBorder = when {
                            isTranscribingAi -> Color(0xFF818CF8)
                            syncedLyrics.isNotEmpty() -> Color(0xFF10B981)
                            lyricsText.isNotBlank() -> Color(0xFF3B82F6)
                            else -> Color(0xFF475569)
                        }
                        val badgeLabel = when {
                            isTranscribingAi -> "AI Transcribing..."
                            syncedLyrics.isNotEmpty() -> "Synced Karaoke"
                            lyricsText.isNotBlank() -> "Plain Text"
                            else -> "No Lyrics"
                        }
                        val badgeColor = when {
                            isTranscribingAi -> Color(0xFFA5B4FC)
                            syncedLyrics.isNotEmpty() -> Color(0xFF34D399)
                            lyricsText.isNotBlank() -> Color(0xFF60A5FA)
                            else -> Color(0xFF94A3B8)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeBg,
                            border = BorderStroke(1.dp, badgeBorder),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(badgeColor, CircleShape)
                                )
                                Text(
                                    text = badgeLabel,
                                    fontSize = 11.sp,
                                    lineHeight = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeColor
                                )
                            }
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        Triple("SYNCED", "Live Synced", Icons.Default.MusicNote),
                        Triple("PLAIN", "Edit Lyrics", Icons.Default.Edit),
                        Triple("SEARCH", "Search Online", Icons.Default.Search),
                        Triple("AI", "AI Transcribe", Icons.Default.AutoAwesome)
                    )
                    tabs.forEach { (tabId, label, icon) ->
                        val isSel = activeTab == tabId
                        Surface(
                            onClick = { activeTab = tabId },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF2563EB) else Color(0xFF161A28),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF3B82F6) else Color(0xFF23283B)),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSel) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Tab Content Body
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        "SYNCED" -> {
                            if (syncedLyrics.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.MusicOff, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(38.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("No time-synced lyrics found for this track.", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF94A3B8))
                                    Spacer(Modifier.height(6.dp))
                                    Text("Extract lyrics from singing vocals with AI, or search online database.", fontSize = 12.sp, color = Color(0xFF64748B))
                                    Spacer(Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { activeTab = "AI" },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("AI Transcribe", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { activeTab = "SEARCH" },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Search Online", fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                val activeIdx = syncedLyrics.indexOfLast { it.timestampSeconds <= currentTimeSeconds }
                                LaunchedEffect(activeIdx) {
                                    if (activeIdx >= 0) {
                                        listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
                                    }
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    itemsIndexed(syncedLyrics) { index, line ->
                                        val isActive = index == activeIdx
                                        val mm = (line.timestampSeconds / 60).toInt()
                                        val ss = (line.timestampSeconds % 60).toInt()
                                        val timeStr = "%02d:%02d".format(mm, ss)

                                        Surface(
                                            onClick = { onSeekTo(line.timestampSeconds) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isActive) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color.Transparent,
                                            border = if (isActive) BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)) else null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = timeStr,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (isActive) Color(0xFF38BDF8) else Color(0xFF475569),
                                                    modifier = Modifier.width(44.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = line.text,
                                                    fontSize = if (isActive) 14.5.sp else 13.sp,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isActive) Color.White else Color(0xFF94A3B8),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isActive) {
                                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "PLAIN" -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Utility Toolbar
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val nonBlankLines = editableLyrics.lines().count { it.isNotBlank() }
                                    val wordCount = editableLyrics.split(Regex("\\s+")).count { it.isNotBlank() }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF1E2438),
                                            border = BorderStroke(1.dp, Color(0xFF2E3856))
                                        ) {
                                            Text(
                                                text = "$nonBlankLines lines • $wordCount words",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        if (cleanNoticeMessage != null) {
                                            Text(
                                                text = cleanNoticeMessage ?: "",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF34D399)
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Clean & Deduplicate Button
                                        OutlinedButton(
                                            onClick = {
                                                val beforeCount = editableLyrics.lines().size
                                                val cleaned = LyricsExtractor.cleanAndDeduplicateLyrics(editableLyrics)
                                                val diff = beforeCount - cleaned.lines().size
                                                editableLyrics = cleaned
                                                cleanNoticeMessage = if (diff > 0) "Removed $diff repeating/tag lines" else "Cleaned formatting"
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp),
                                            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
                                        ) {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Clean Duplicates & Tags", fontSize = 11.sp, color = Color(0xFF93C5FD))
                                        }

                                        // Copy Button
                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(editableLyrics))
                                                cleanNoticeMessage = "Copied to clipboard!"
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp),
                                            border = BorderStroke(1.dp, Color(0xFF334155))
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Copy", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                        }

                                        // Clear Button
                                        OutlinedButton(
                                            onClick = {
                                                editableLyrics = ""
                                                cleanNoticeMessage = null
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp),
                                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Clear", fontSize = 11.sp, color = Color(0xFFF87171))
                                        }
                                    }
                                }

                                // Spacious Text Editor
                                OutlinedTextField(
                                    value = editableLyrics,
                                    onValueChange = {
                                        editableLyrics = it
                                        cleanNoticeMessage = null
                                    },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 13.sp,
                                        lineHeight = 22.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFF1F5F9)
                                    ),
                                    placeholder = {
                                        Text(
                                            "Paste song lyrics or LRC timestamps here...\nExample:\n[00:15.00] First line of song\n[00:20.50] Second line of song",
                                            color = Color(0xFF475569),
                                            fontSize = 12.5.sp,
                                            lineHeight = 20.sp
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF3B82F6),
                                        unfocusedBorderColor = Color(0xFF1E263C),
                                        focusedContainerColor = Color(0xFF090C16),
                                        unfocusedContainerColor = Color(0xFF090C16)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chords will automatically align with lyric lines upon saving.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF64748B)
                                    )

                                    Button(
                                        onClick = {
                                            val sanitized = LyricsExtractor.cleanAndDeduplicateLyrics(editableLyrics)
                                            editableLyrics = sanitized
                                            onUpdateLyrics(sanitized)
                                            activeTab = if (sanitized.contains("[")) "SYNCED" else "PLAIN"
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Save & Align Chords", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    }
                                }
                            }
                        }

                        "SEARCH" -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Search inputs row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = searchTitle,
                                        onValueChange = { searchTitle = it },
                                        label = { Text("Song Title / Track Name") },
                                        modifier = Modifier.weight(1.3f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF3B82F6),
                                            unfocusedBorderColor = Color(0xFF262C3F),
                                            focusedTextColor = Color(0xFFF1F5F9),
                                            unfocusedTextColor = Color(0xFFF1F5F9)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    OutlinedTextField(
                                        value = searchArtist,
                                        onValueChange = { searchArtist = it },
                                        label = { Text("Artist (Optional)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF3B82F6),
                                            unfocusedBorderColor = Color(0xFF262C3F),
                                            focusedTextColor = Color(0xFFF1F5F9),
                                            unfocusedTextColor = Color(0xFFF1F5F9)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Button(
                                        onClick = {
                                            if (searchTitle.isNotBlank()) {
                                                scope.launch {
                                                    isSearchingOnlineLocal = true
                                                    onlineSearchMessage = "Searching online database for \"$searchTitle\"..."
                                                    try {
                                                        val results = withContext(Dispatchers.IO) {
                                                            LyricsExtractor.searchOnlineLyricsList(searchTitle, searchArtist)
                                                        }
                                                        isSearchingOnlineLocal = false
                                                        onlineSearchResults = results
                                                        if (results.isNotEmpty()) {
                                                            onlineSearchMessage = "Found ${results.size} match${if (results.size > 1) "es" else ""} on LRCLIB:"
                                                        } else {
                                                            onlineSearchMessage = "No lyrics found on LRCLIB for \"$searchTitle\"."
                                                        }
                                                    } catch (e: Throwable) {
                                                        isSearchingOnlineLocal = false
                                                        onlineSearchResults = emptyList()
                                                        onlineSearchMessage = "Search error: ${e.message ?: "Unable to reach online database"}"
                                                    }
                                                }
                                            }
                                        },
                                        enabled = searchTitle.isNotBlank() && !isSearchingOnlineLocal,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        modifier = Modifier.height(52.dp).padding(top = 4.dp)
                                    ) {
                                        if (isSearchingOnlineLocal) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Searching...", fontSize = 12.sp)
                                        } else {
                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Search", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                        }
                                    }
                                }

                                if (onlineSearchMessage != null) {
                                    Text(
                                        text = onlineSearchMessage ?: "",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (onlineSearchResults.isNotEmpty()) Color(0xFF38BDF8) else Color(0xFFF87171)
                                    )
                                }

                                if (onlineSearchResults.isNotEmpty()) {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(onlineSearchResults) { item ->
                                            val isSynced = item.syncedLyrics.isNotEmpty()
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF131728),
                                                border = BorderStroke(1.dp, if (isSynced) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFF232D48)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text(item.detectedTitle ?: searchTitle, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFFF1F5F9))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = if (isSynced) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFF1E293B),
                                                                border = BorderStroke(1.dp, if (isSynced) Color(0xFF059669) else Color(0xFF334155))
                                                            ) {
                                                                Text(
                                                                    text = if (isSynced) "Karaoke Synced" else "Plain Text",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = if (isSynced) Color(0xFF34D399) else Color(0xFF94A3B8),
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                        Text("Artist: ${item.detectedArtist ?: "Unknown"}", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                                                        val preview = item.plainLyrics.lines().filter { it.isNotBlank() }.take(2).joinToString(" • ")
                                                        if (preview.isNotBlank()) {
                                                            Text(preview, fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                        }
                                                    }

                                                    Spacer(Modifier.width(12.dp))

                                                    Button(
                                                        onClick = {
                                                            val lrcFormatted = item.toLrcOrPlainText()
                                                            editableLyrics = lrcFormatted
                                                            onApplySearchResult(item)
                                                            activeTab = if (item.syncedLyrics.isNotEmpty()) "SYNCED" else "PLAIN"
                                                        },
                                                        shape = RoundedCornerShape(6.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Apply", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (onlineSearchMessage != null && !isSearchingOnlineLocal) {
                                    // Fallback options
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF131728),
                                        border = BorderStroke(1.dp, Color(0xFF232D48)),
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("Suggestions to find lyrics:", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = Color(0xFFF1F5F9))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        try {
                                                            val q = java.net.URLEncoder.encode("$searchTitle $searchArtist lyrics", "UTF-8")
                                                            java.awt.Desktop.getDesktop().browse(java.net.URI("https://www.google.com/search?q=$q"))
                                                        } catch (_: Exception) {}
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                                                    Spacer(Modifier.width(5.dp))
                                                    Text("Search on Google ↗", fontSize = 11.5.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = { activeTab = "AI" },
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                                                    Spacer(Modifier.width(5.dp))
                                                    Text("Transcribe with AI (Whisper) →", fontSize = 11.5.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = { activeTab = "PLAIN" },
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                                    Spacer(Modifier.width(5.dp))
                                                    Text("Paste Manually →", fontSize = 11.5.sp)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("Search LRCLIB for synchronized karaoke lyrics above,\nor find tracks by Hindi/Bengali/English title.", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }

                        "AI" -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Compact Banner
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF131728),
                                    border = BorderStroke(1.dp, Color(0xFF232D48)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF06B6D4)))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(verticalArrangement = Arrangement.Center) {
                                            Text("AI Vocal Transcriber (whisper.cpp)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFF1F5F9))
                                            Spacer(Modifier.height(2.dp))
                                            Text("Extracts time-synced lyrics directly from vocals using local Whisper AI.", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                                        }
                                    }
                                }

                                // Inline AI error display
                                if (aiTabError != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF7F1D1D).copy(alpha = 0.30f),
                                        border = BorderStroke(1.dp, Color(0xFFDC2626))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                                                Text("Transcription Failed", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFCA5A5))
                                                Spacer(Modifier.height(2.dp))
                                                Text(aiTabError ?: "", fontSize = 11.5.sp, color = Color(0xFFFECACA), lineHeight = 17.sp)
                                            }
                                            IconButton(onClick = { aiTabError = null }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }

                                // Model & Language Selector (Proportional weights for balanced button widths)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Model (2 options)
                                    Column(modifier = Modifier.weight(0.44f)) {
                                        Text("Whisper Model", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf("tiny" to "Tiny (Fast)", "base" to "Base (Std)").forEach { (mod, label) ->
                                                val isSel = selectedModel == mod
                                                Surface(
                                                    onClick = { selectedModel = mod },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) Color(0xFF312E81) else Color(0xFF161A28),
                                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF6366F1) else Color(0xFF23283B)),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 11.5.sp,
                                                            lineHeight = 11.5.sp,
                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSel) Color(0xFFC7D2FE) else Color(0xFF94A3B8),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Language (4 options)
                                    Column(modifier = Modifier.weight(0.56f)) {
                                        Text("Language", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf("auto" to "Auto", "en" to "EN", "hi" to "HI", "bn" to "BN").forEach { (lang, label) ->
                                                val isSel = selectedLanguage == lang
                                                Surface(
                                                    onClick = { selectedLanguage = lang },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) Color(0xFF312E81) else Color(0xFF161A28),
                                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF6366F1) else Color(0xFF23283B)),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 11.5.sp,
                                                            lineHeight = 11.5.sp,
                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSel) Color(0xFFC7D2FE) else Color(0xFF94A3B8),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Audio Source Status
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (audioSourceAvailable) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFF7F1D1D).copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, if (audioSourceAvailable) Color(0xFF059669) else Color(0xFFDC2626)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (audioSourceAvailable) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (audioSourceAvailable) Color(0xFF34D399) else Color(0xFFF87171),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = if (audioSourceAvailable) "Audio sound wave loaded and ready for AI processing" else "No audio source loaded yet",
                                            fontSize = 12.sp,
                                            lineHeight = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (audioSourceAvailable) Color(0xFFA7F3D0) else Color(0xFFFECACA)
                                        )
                                    }
                                }

                                // Transcribing progress box
                                if (isTranscribingAi) {
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                                        border = BorderStroke(1.dp, Color(0xFF4338CA)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color(0xFF818CF8),
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.5.dp
                                            )
                                            Text(
                                                text = aiProgressText.ifBlank { "Whisper AI listening to vocal sound wave..." },
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFE0E7FF),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // Action Button
                                Button(
                                    onClick = {
                                        aiTabError = null
                                        onTranscribeAi(selectedModel, selectedLanguage)
                                    },
                                    enabled = audioSourceAvailable && !isTranscribingAi,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4F46E5),
                                        disabledContainerColor = Color(0xFF1E2438),
                                        disabledContentColor = Color(0xFF64748B)
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (isTranscribingAi) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Transcribing Vocals with AI...",
                                                fontSize = 12.5.sp,
                                                lineHeight = 12.5.sp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Extract Lyrics from Audio with AI (Whisper)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }

                                // If synced lyrics exist, show preview info
                                if (syncedLyrics.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ColorSurfaceCard,
                                        border = BorderStroke(1.dp, ColorCardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = ColorAccentBlue, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("${syncedLyrics.size} time-synced lyric lines active", fontSize = 11.5.sp, color = ColorAccentBlue)
                                            }
                                            TextButton(onClick = { activeTab = "SYNCED" }, modifier = Modifier.height(28.dp)) {
                                                Text("View Live Karaoke", fontSize = 11.sp, color = ColorAccentBlue)
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
    }
}

