package com.gbros.tabslite.utilities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import javax.sound.midi.MidiChannel
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer

object MusicTheoryAudioPlayer {
    private var synth: Synthesizer? = null
    private var mainChannel: MidiChannel? = null
    private var percussionChannel: MidiChannel? = null
    private var playbackJob: Job? = null
    private var singleNoteJob: Job? = null
    private var tunerToneJob: Job? = null
    private val activeSoundingPitches = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    /**
     * Instantly silences and pauses all currently sounding notes.
     */
    fun silenceCurrentNotes() {
        try {
            mainChannel?.controlChange(64, 0) // Release sustain pedal
            for (p in activeSoundingPitches) {
                mainChannel?.noteOff(p)
            }
            activeSoundingPitches.clear()
            mainChannel?.allNotesOff()
        } catch (e: Exception) {
            // Ignore
        }
    }

    var activeStrumSound: StrumGuitarSound by mutableStateOf(StrumGuitarSound.ACOUSTIC_STEEL)
        private set

    fun setStrumGuitarSound(sound: StrumGuitarSound) {
        activeStrumSound = sound
        try {
            initMidi()
            mainChannel?.programChange(sound.programNumber)
            mainChannel?.controlChange(91, sound.reverbLevel)
            mainChannel?.controlChange(93, sound.chorusLevel)
            mainChannel?.controlChange(74, sound.brightness)
            mainChannel?.controlChange(71, sound.resonance)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var isPlaying: Boolean by mutableStateOf(false)
        internal set

    init {
        initMidi()
    }

    private fun initMidi() {
        try {
            if (synth == null || !synth!!.isOpen) {
                synth = MidiSystem.getSynthesizer()
                synth?.open()
                val channels = synth?.channels
                if (!channels.isNullOrEmpty()) {
                    mainChannel = channels[0] // Main melodic / chord instrument
                    // Set instrument to Acoustic Guitar (Steel string = 25 in General MIDI 0-indexed)
                    mainChannel?.programChange(25)
                    if (channels.size > 9) {
                        percussionChannel = channels[9] // General MIDI Channel 10 is percussion (index 9)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setInstrument(instrument: LearnInstrument, pianoTone: PianoTone = PianoTone.CONCERT_GRAND) {
        try {
            initMidi()
            val program = when (instrument) {
                LearnInstrument.GUITAR -> 25 // Acoustic Guitar (Steel)
                LearnInstrument.UKULELE -> 24 // Acoustic Guitar (Nylon)
                LearnInstrument.PIANO -> pianoTone.programNumber
                LearnInstrument.BASS -> 33 // Electric Bass (Finger)
            }
            mainChannel?.programChange(program)

            if (instrument == LearnInstrument.PIANO) {
                // Apply acoustic piano tone parameters via standard MIDI controllers
                mainChannel?.controlChange(91, pianoTone.reverbLevel) // Reverb Send
                mainChannel?.controlChange(93, pianoTone.chorusLevel) // Chorus Send
                mainChannel?.controlChange(74, pianoTone.brightness)  // Brightness (Filter Cutoff)
                mainChannel?.controlChange(71, pianoTone.resonance)   // Harmonic Resonance
            } else {
                mainChannel?.controlChange(91, 40)
                mainChannel?.controlChange(93, 0)
                mainChannel?.controlChange(74, 64)
                mainChannel?.controlChange(71, 64)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Strum a chord shape with an authentic downstroke brush delay between strings,
     * or delegate to rich acoustic piano voicing if the piano instrument is selected.
     */
    fun strumChord(
        shape: ChordShape,
        instrument: LearnInstrument = LearnInstrument.GUITAR,
        isArpeggio: Boolean = false,
        pianoTone: PianoTone = PianoTone.CONCERT_GRAND,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        if (instrument == LearnInstrument.PIANO) {
            val chordNotes = TabTransposer.getChordNotes(shape.chordName)
            val rootNote = TabTransposer.extractRootNote(shape.chordName).ifEmpty { "C" }
            playPianoChord(chordNotes, rootNote, pianoTone, isArpeggio, scope)
            return
        }

        stop()
        playbackJob = scope.launch {
            try {
                initMidi()
                setInstrument(instrument)

                val openPitches = instrument.openMidiPitches
                val stringCount = instrument.stringCount
                val frets = shape.frets.take(stringCount)

                // Calculate MIDI pitch for each string
                val activePitches = mutableListOf<Int>()
                frets.forEachIndexed { sIdx, fret ->
                    if (fret >= 0) {
                        val basePitch = openPitches.getOrElse(sIdx) { 40 }
                        activePitches.add(basePitch + fret)
                    }
                }

                if (activePitches.isEmpty()) return@launch

                isPlaying = true
                val delayMs = if (isArpeggio) 140L else 32L

                // Sound each note in order (low pitch to high pitch)
                for (pitch in activePitches) {
                    mainChannel?.noteOn(pitch, 88)
                    delay(delayMs)
                }

                delay(900L)

                // Silence notes gently
                for (pitch in activePitches) {
                    mainChannel?.noteOff(pitch)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isPlaying = false
            }
        }
    }

    /**
     * Play an authentic acoustic piano chord with real acoustic touch, sustain pedal and bass voicing
     */
    fun playPianoChord(
        chordNotes: List<String>,
        rootNote: String,
        pianoTone: PianoTone = PianoTone.CONCERT_GRAND,
        isArpeggio: Boolean = false,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        stop()
        playbackJob = scope.launch {
            try {
                initMidi()
                setInstrument(LearnInstrument.PIANO, pianoTone)
                isPlaying = true

                // Calculate MIDI pitches
                // 1. Left hand bass note in lower octave (C2-B2 = 36..47)
                val rootPitchClass = getPitchClass(rootNote)
                val bassPitch = 36 + rootPitchClass

                // 2. Right hand harmony notes voiced around Middle C (C4 = 60)
                val voicingPitches = mutableListOf<Int>()
                voicingPitches.add(48 + rootPitchClass) // C3 root anchor

                chordNotes.forEach { n ->
                    val pc = getPitchClass(n)
                    var pitch = 60 + pc
                    if (pitch < 60 + rootPitchClass) {
                        pitch += 12 // Ascending order
                    }
                    if (!voicingPitches.contains(pitch)) {
                        voicingPitches.add(pitch)
                    }
                }
                voicingPitches.sort()

                val allPitches = listOf(bassPitch) + voicingPitches

                // Engage Sustain Pedal (Damper pedal down) for acoustic resonance!
                mainChannel?.controlChange(64, 127)

                if (isArpeggio) {
                    // Arpeggiated fluid acoustic roll
                    mainChannel?.noteOn(bassPitch, 92)
                    delay(120L)
                    voicingPitches.forEachIndexed { idx, p ->
                        val velocity = if (idx == voicingPitches.size - 1) 94 else 82
                        mainChannel?.noteOn(p, velocity)
                        delay(110L)
                    }
                    delay(1600L)
                } else {
                    // Natural acoustic block chord with humanized micro-roll (8ms)
                    mainChannel?.noteOn(bassPitch, 94)
                    delay(8L)
                    voicingPitches.forEachIndexed { idx, p ->
                        val velocity = if (idx == voicingPitches.size - 1) 92 else 80
                        mainChannel?.noteOn(p, velocity)
                        delay(8L)
                    }
                    delay(2000L)
                }

                // Release notes gently
                allPitches.forEach { p -> mainChannel?.noteOff(p) }
                delay(200L)
                mainChannel?.controlChange(64, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    mainChannel?.controlChange(64, 0)
                } catch (e: Exception) { /* ignore */ }
                isPlaying = false
            }
        }
    }

    /**
     * Play notes directly from a scale with dynamic phrasing and selected instrument/piano tone
     */
    fun playScaleNotes(
        notes: List<String>,
        bpm: Int = 120,
        instrument: LearnInstrument = LearnInstrument.GUITAR,
        pianoTone: PianoTone = PianoTone.CONCERT_GRAND,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        onNoteStep: ((Int) -> Unit)? = null
    ) {
        stop()
        playbackJob = scope.launch {
            try {
                initMidi()
                setInstrument(instrument, pianoTone)
                isPlaying = true

                val isPiano = (instrument == LearnInstrument.PIANO)
                if (isPiano) {
                    mainChannel?.controlChange(64, 60) // Acoustic half-pedal resonance
                }

                // Convert note names to MIDI pitches starting from Middle C (C4 = 60)
                var currentOctave = 4
                var lastPitch = -1
                val pitches = notes.map { note ->
                    val pitchClass = getPitchClass(note)
                    var pitch = (currentOctave + 1) * 12 + pitchClass
                    if (lastPitch != -1 && pitch < lastPitch) {
                        currentOctave++
                        pitch = (currentOctave + 1) * 12 + pitchClass
                    }
                    lastPitch = pitch
                    pitch
                }

                // Ascending then descending run
                val sequence = pitches + pitches.reversed().drop(1)
                val stepDelay = (60_000L / bpm).coerceIn(120L, 800L)

                sequence.forEachIndexed { idx, pitch ->
                    val noteIdx = if (idx < pitches.size) idx else (pitches.size - 1 - (idx - pitches.size + 1))
                    withContext(Dispatchers.Main) {
                        onNoteStep?.invoke(noteIdx)
                    }

                    val velocity = if (isPiano) {
                        if (noteIdx == 0) 96 else 84 // Accent root note on acoustic piano
                    } else 92

                    mainChannel?.noteOn(pitch, velocity)
                    delay(stepDelay)
                    mainChannel?.noteOff(pitch)
                }

                delay(250L)
                if (isPiano) {
                    mainChannel?.controlChange(64, 0) // Release pedal
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    mainChannel?.controlChange(64, 0)
                } catch (e: Exception) { /* ignore */ }
                withContext(Dispatchers.Main) {
                    onNoteStep?.invoke(-1)
                }
                isPlaying = false
            }
        }
    }

    /**
     * Play a specific pitch using the selected instrument's sound (Guitar, Ukulele, Bass, Piano).
     */
    fun playInstrumentPitch(
        pitch: Int,
        instrument: LearnInstrument = LearnInstrument.GUITAR,
        pianoTone: PianoTone = PianoTone.CONCERT_GRAND,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        // Stop any previous note immediately when another note is pressed
        singleNoteJob?.cancel()
        tunerToneJob?.cancel()
        silenceCurrentNotes()

        singleNoteJob = scope.launch {
            try {
                initMidi()
                setInstrument(instrument, pianoTone)
                val isPiano = (instrument == LearnInstrument.PIANO)
                if (isPiano) {
                    mainChannel?.controlChange(64, 127) // Sustain pedal
                }
                val velocity = when (instrument) {
                    LearnInstrument.BASS -> 100
                    LearnInstrument.GUITAR -> 94
                    LearnInstrument.UKULELE -> 88
                    LearnInstrument.PIANO -> 92
                }
                mainChannel?.noteOn(pitch, velocity)
                activeSoundingPitches.add(pitch)

                val duration = when (instrument) {
                    LearnInstrument.BASS -> 1200L
                    LearnInstrument.GUITAR -> 1000L
                    LearnInstrument.UKULELE -> 850L
                    LearnInstrument.PIANO -> 900L
                }
                delay(duration)
                mainChannel?.noteOff(pitch)
                activeSoundingPitches.remove(pitch)

                if (isPiano) {
                    delay(150L)
                    mainChannel?.controlChange(64, 0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                activeSoundingPitches.remove(pitch)
            }
        }
    }

    /**
     * Play a note on a specific string and fret for any stringed instrument (Guitar, Ukulele, Bass).
     */
    fun playFretNote(
        instrument: LearnInstrument,
        stringIdx: Int,
        fret: Int,
        pianoTone: PianoTone = PianoTone.CONCERT_GRAND,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        val basePitch = instrument.openMidiPitches.getOrElse(stringIdx) { 40 }
        val pitch = basePitch + fret
        playInstrumentPitch(pitch, instrument, pianoTone, scope)
    }

    /**
     * Play a single preview note with selected instrument sound (Guitar, Ukulele, Bass, Piano).
     */
    fun playInstrumentNote(
        note: String,
        instrument: LearnInstrument,
        pianoTone: PianoTone = PianoTone.CONCERT_GRAND,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        val baseOctave = when (instrument) {
            LearnInstrument.BASS -> 2
            LearnInstrument.GUITAR -> 3
            LearnInstrument.UKULELE -> 4
            LearnInstrument.PIANO -> 4
        }
        val pitch = (baseOctave + 1) * 12 + getPitchClass(note)
        playInstrumentPitch(pitch, instrument, pianoTone, scope)
    }

    /**
     * Play a single preview note with acoustic piano sustain
     */
    fun playSingleNote(
        note: String,
        octave: Int = 4,
        pianoTone: PianoTone = PianoTone.CONCERT_GRAND,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        val pitch = (octave + 1) * 12 + getPitchClass(note)
        playInstrumentPitch(pitch, LearnInstrument.PIANO, pianoTone, scope)
    }

    /**
     * Loops a strumming pattern in real-time with synchronized metronome clicks, human brush sweeps,
     * and authentic acoustic or electric guitar tones (Acoustic Steel, Nylon, Clean Electric, Crunch, Jazz, 12-String).
     */
    fun playStrummingLoop(
        pattern: StrumPattern,
        bpm: Int,
        chordName: String = "G",
        sound: StrumGuitarSound = activeStrumSound,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        onBeatChanged: (Int) -> Unit
    ) {
        stop()
        setStrumGuitarSound(sound)
        playbackJob = scope.launch {
            try {
                initMidi()
                setStrumGuitarSound(sound)
                isPlaying = true

                // Calculate MIDI pitches for playback chord
                val shapes = TabTransposer.getGuitarChordShapes(chordName)
                val shape = shapes.firstOrNull() ?: ChordShape(chordName, "Guitar", listOf(3, 2, 0, 0, 0, 3), listOf(2, 1, 0, 0, 0, 3), 1)
                val activePitches = mutableListOf<Int>()
                shape.frets.take(6).forEachIndexed { sIdx, fret ->
                    if (fret >= 0) {
                        val base = LearnInstrument.GUITAR.openMidiPitches.getOrElse(sIdx) { 40 }
                        activePitches.add(base + fret)
                    }
                }

                val bassPitch = activePitches.firstOrNull() ?: 43

                // Calculate subdivision delay based on beats count
                val beatsCount = pattern.beats.size
                val beatIntervalMs = when (pattern.timeSignature) {
                    "3/4" -> if (beatsCount == 3) (60_000L / bpm) else (60_000L / (bpm * 2))
                    "6/8" -> (60_000L / (bpm * 3)).coerceAtLeast(60L)
                    "2/4" -> (60_000L / (bpm * 2)).coerceAtLeast(60L)
                    "12/8" -> (60_000L / (bpm * 3)).coerceAtLeast(50L)
                    "5/4" -> if (beatsCount == 5) (60_000L / bpm).coerceAtLeast(60L) else (60_000L / (bpm * 2)).coerceAtLeast(60L)
                    else -> if (beatsCount <= 4) (60_000L / bpm) else if (beatsCount > 8) (60_000L / (bpm * 4)) else (60_000L / (bpm * 2)) // 4/4
                }.coerceIn(50L, 1000L)

                val brushDelay = if (beatsCount > 8) 2L else 3L

                while (isActive) {
                    for (i in pattern.beats.indices) {
                        if (!isActive) break

                        withContext(Dispatchers.Main) {
                            onBeatChanged(i)
                        }

                        val beat = pattern.beats[i]

                        // Play percussion click on primary beats
                        if (beat.isPrimaryBeat) {
                            percussionChannel?.noteOn(76, 75) // Woodblock High
                        } else {
                            percussionChannel?.noteOn(77, 45) // Woodblock Low
                        }

                        val currentSound = activeStrumSound
                        val isElectric = currentSound.isElectric

                        // Play chord strum based on stroke type with organic brush stagger
                        when (beat.stroke) {
                            StrumStrokeType.DOWN -> {
                                for (p in activePitches) {
                                    mainChannel?.noteOn(p, if (isElectric) 84 else 78)
                                    delay(brushDelay)
                                }
                            }
                            StrumStrokeType.ACCENT_DOWN -> {
                                for (p in activePitches) {
                                    mainChannel?.noteOn(p, if (isElectric) 105 else 98)
                                    delay(brushDelay)
                                }
                            }
                            StrumStrokeType.UP -> {
                                for (p in activePitches.reversed()) {
                                    mainChannel?.noteOn(p, if (isElectric) 76 else 70)
                                    delay(brushDelay)
                                }
                            }
                            StrumStrokeType.ACCENT_UP -> {
                                for (p in activePitches.reversed()) {
                                    mainChannel?.noteOn(p, if (isElectric) 96 else 90)
                                    delay(brushDelay)
                                }
                            }
                            StrumStrokeType.BASS -> {
                                mainChannel?.noteOn(bassPitch, if (isElectric) 100 else 95)
                            }
                            StrumStrokeType.MUTE -> {
                                // Short percussive slap + chop
                                percussionChannel?.noteOn(37, 85) // Side Stick
                                if (isElectric) {
                                    for (p in activePitches.take(3)) mainChannel?.noteOn(p, 55)
                                    delay(15L)
                                    for (p in activePitches.take(3)) mainChannel?.noteOff(p)
                                }
                            }
                            StrumStrokeType.REST -> {
                                // Rest - no strum
                            }
                        }

                        delay((beatIntervalMs * 0.7f).toLong().coerceAtLeast(30L))

                        // Release notes
                        for (p in activePitches) mainChannel?.noteOff(p)
                        mainChannel?.noteOff(bassPitch)

                        delay((beatIntervalMs * 0.3f).toLong().coerceAtLeast(20L))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    onBeatChanged(-1)
                }
                isPlaying = false
            }
        }
    }

    /**
     * Play an authentic natural reference pitch with specified instrument program, natural attack and warm acoustic sustain.
     * Automatically stops and pauses any previous note when another note is pressed.
     */
    fun playTunerTone(
        pitch: Int,
        programNumber: Int = 25,
        durationMs: Long? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        // Immediately cancel previous note job and silence active notes
        tunerToneJob?.cancel()
        singleNoteJob?.cancel()
        silenceCurrentNotes()

        // Short duration for violin (40) and mandolin (45), natural pluck for others
        val effectiveDuration = durationMs ?: when (programNumber) {
            40 -> 800L   // Violin short stroke
            45 -> 650L   // Mandolin / Pizzicato short pluck
            105 -> 1400L // Banjo
            24 -> 1600L  // Ukulele
            else -> 2000L // Guitar & Bass
        }

        tunerToneJob = scope.launch {
            try {
                initMidi()
                mainChannel?.programChange(programNumber)
                mainChannel?.controlChange(91, if (programNumber == 40 || programNumber == 45) 35 else 55) // Rich natural reverb
                mainChannel?.controlChange(74, 75) // Brightness
                mainChannel?.controlChange(71, 64) // Harmonic resonance
                val velocity = when (programNumber) {
                    in 32..39 -> 102 // Bass punch
                    105 -> 98       // Banjo crisp twang
                    40 -> 94        // Violin smooth stroke
                    45 -> 96        // Mandolin crisp attack
                    else -> 96      // Guitar & Ukulele natural pluck
                }
                mainChannel?.noteOn(pitch, velocity)
                activeSoundingPitches.add(pitch)

                delay(effectiveDuration)
                mainChannel?.noteOff(pitch)
                activeSoundingPitches.remove(pitch)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                activeSoundingPitches.remove(pitch)
            }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        tunerToneJob?.cancel()
        tunerToneJob = null
        singleNoteJob?.cancel()
        singleNoteJob = null
        silenceCurrentNotes()
        isPlaying = false
    }

    private fun getPitchClass(note: String): Int {
        val clean = note.trim()
        val idxSharp = MusicTheoryData.NOTES_SHARP.indexOf(clean)
        if (idxSharp != -1) return idxSharp
        val idxFlat = MusicTheoryData.NOTES_FLAT.indexOf(clean)
        if (idxFlat != -1) return idxFlat
        return 0
    }
}
