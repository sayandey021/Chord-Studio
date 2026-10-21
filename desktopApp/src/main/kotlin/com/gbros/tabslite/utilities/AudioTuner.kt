package com.gbros.tabslite.utilities

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.*
import kotlin.math.*

data class TuningString(
    val name: String,
    val octave: Int,
    val frequency: Double,
    val noteName: String = "$name$octave"
)

data class InstrumentCategory(
    val id: String,
    val name: String,
    val iconKey: String = "guitar",
    val stringCount: Int
)

data class TuningPreset(
    val name: String,
    val categoryId: String, // "guitar_6", "guitar_7", "ukulele", "bass", "banjo", "violin"
    val strings: List<TuningString>
)

data class TunerState(
    val isListening: Boolean = false,
    val detectedFrequency: Double = 0.0,
    val targetString: TuningString? = null,
    val closestNoteName: String = "--",
    val centsOffset: Double = 0.0,
    val isInTune: Boolean = false,
    val audioLevel: Double = 0.0,
    val statusMessage: String = "Play a string or enable microphone"
)

object AudioTuner {
    val INSTRUMENTS = listOf(
        InstrumentCategory("guitar_6", "6-String Acoustic Guitar", "guitar", 6),
        InstrumentCategory("guitar_7", "7-String Guitar", "guitar", 7),
        InstrumentCategory("ukulele", "Acoustic Ukulele", "ukulele", 4),
        InstrumentCategory("bass", "Bass Guitar", "bass", 4),
        InstrumentCategory("banjo", "5-String Banjo", "banjo", 5),
        InstrumentCategory("violin", "Violin / Mandolin", "violin", 4)
    )

    val PRESETS = listOf(
        // === 6-STRING ACOUSTIC GUITAR ===
        TuningPreset(
            name = "Standard (EADGBE)",
            categoryId = "guitar_6",
            strings = listOf(
                TuningString("E", 2, 82.41),
                TuningString("A", 2, 110.00),
                TuningString("D", 3, 146.83),
                TuningString("G", 3, 196.00),
                TuningString("B", 3, 246.94),
                TuningString("E", 4, 329.63)
            )
        ),
        TuningPreset(
            name = "Drop D (DADGBE)",
            categoryId = "guitar_6",
            strings = listOf(
                TuningString("D", 2, 73.42),
                TuningString("A", 2, 110.00),
                TuningString("D", 3, 146.83),
                TuningString("G", 3, 196.00),
                TuningString("B", 3, 246.94),
                TuningString("E", 4, 329.63)
            )
        ),
        TuningPreset(
            name = "Half-Step Down (Eb)",
            categoryId = "guitar_6",
            strings = listOf(
                TuningString("Eb", 2, 77.78),
                TuningString("Ab", 2, 103.83),
                TuningString("Db", 3, 138.59),
                TuningString("Gb", 3, 185.00),
                TuningString("Bb", 3, 233.08),
                TuningString("Eb", 4, 311.13)
            )
        ),
        TuningPreset(
            name = "DADGAD",
            categoryId = "guitar_6",
            strings = listOf(
                TuningString("D", 2, 73.42),
                TuningString("A", 2, 110.00),
                TuningString("D", 3, 146.83),
                TuningString("G", 3, 196.00),
                TuningString("A", 3, 220.00),
                TuningString("D", 4, 293.66)
            )
        ),
        TuningPreset(
            name = "Open D (DADF#AD)",
            categoryId = "guitar_6",
            strings = listOf(
                TuningString("D", 2, 73.42),
                TuningString("A", 2, 110.00),
                TuningString("D", 3, 146.83),
                TuningString("F#", 3, 185.00),
                TuningString("A", 3, 220.00),
                TuningString("D", 4, 293.66)
            )
        ),
        TuningPreset(
            name = "Open G (DGDGBD)",
            categoryId = "guitar_6",
            strings = listOf(
                TuningString("D", 2, 73.42),
                TuningString("G", 2, 98.00),
                TuningString("D", 3, 146.83),
                TuningString("G", 3, 196.00),
                TuningString("B", 3, 246.94),
                TuningString("D", 4, 293.66)
            )
        ),
        TuningPreset(
            name = "Open C (CGCGCE)",
            categoryId = "guitar_6",
            strings = listOf(
                TuningString("C", 2, 65.41),
                TuningString("G", 2, 98.00),
                TuningString("C", 3, 130.81),
                TuningString("G", 3, 196.00),
                TuningString("C", 4, 261.63),
                TuningString("E", 4, 329.63)
            )
        ),
        TuningPreset(
            name = "Drop C (CGCFAD)",
            categoryId = "guitar_6",
            strings = listOf(
                TuningString("C", 2, 65.41),
                TuningString("G", 2, 98.00),
                TuningString("C", 3, 130.81),
                TuningString("F", 3, 174.61),
                TuningString("A", 3, 220.00),
                TuningString("D", 4, 293.66)
            )
        ),

        // === 7-STRING GUITAR ===
        TuningPreset(
            name = "7-String Standard (BEADGBE)",
            categoryId = "guitar_7",
            strings = listOf(
                TuningString("B", 1, 61.74),
                TuningString("E", 2, 82.41),
                TuningString("A", 2, 110.00),
                TuningString("D", 3, 146.83),
                TuningString("G", 3, 196.00),
                TuningString("B", 3, 246.94),
                TuningString("E", 4, 329.63)
            )
        ),
        TuningPreset(
            name = "7-String Drop A (AEADGBE)",
            categoryId = "guitar_7",
            strings = listOf(
                TuningString("A", 1, 55.00),
                TuningString("E", 2, 82.41),
                TuningString("A", 2, 110.00),
                TuningString("D", 3, 146.83),
                TuningString("G", 3, 196.00),
                TuningString("B", 3, 246.94),
                TuningString("E", 4, 329.63)
            )
        ),

        // === UKULELE ===
        TuningPreset(
            name = "Standard (GCEA)",
            categoryId = "ukulele",
            strings = listOf(
                TuningString("G", 4, 392.00),
                TuningString("C", 4, 261.63),
                TuningString("E", 4, 329.63),
                TuningString("A", 4, 440.00)
            )
        ),
        TuningPreset(
            name = "Low G (G3 C4 E4 A4)",
            categoryId = "ukulele",
            strings = listOf(
                TuningString("G", 3, 196.00),
                TuningString("C", 4, 261.63),
                TuningString("E", 4, 329.63),
                TuningString("A", 4, 440.00)
            )
        ),
        TuningPreset(
            name = "D-Tuning (ADF#B)",
            categoryId = "ukulele",
            strings = listOf(
                TuningString("A", 4, 440.00),
                TuningString("D", 4, 293.66),
                TuningString("F#", 4, 369.99),
                TuningString("B", 4, 493.88)
            )
        ),
        TuningPreset(
            name = "Baritone (DGBE)",
            categoryId = "ukulele",
            strings = listOf(
                TuningString("D", 3, 146.83),
                TuningString("G", 3, 196.00),
                TuningString("B", 3, 246.94),
                TuningString("E", 4, 329.63)
            )
        ),

        // === BASS GUITAR ===
        TuningPreset(
            name = "4-String Standard (EADG)",
            categoryId = "bass",
            strings = listOf(
                TuningString("E", 1, 41.20),
                TuningString("A", 1, 55.00),
                TuningString("D", 2, 73.42),
                TuningString("G", 2, 98.00)
            )
        ),
        TuningPreset(
            name = "4-String Drop D (DADG)",
            categoryId = "bass",
            strings = listOf(
                TuningString("D", 1, 36.71),
                TuningString("A", 1, 55.00),
                TuningString("D", 2, 73.42),
                TuningString("G", 2, 98.00)
            )
        ),
        TuningPreset(
            name = "5-String Standard (BEADG)",
            categoryId = "bass",
            strings = listOf(
                TuningString("B", 0, 30.87),
                TuningString("E", 1, 41.20),
                TuningString("A", 1, 55.00),
                TuningString("D", 2, 73.42),
                TuningString("G", 2, 98.00)
            )
        ),

        // === BANJO ===
        TuningPreset(
            name = "Open G (gDGBD)",
            categoryId = "banjo",
            strings = listOf(
                TuningString("g", 4, 392.00),
                TuningString("D", 3, 146.83),
                TuningString("G", 3, 196.00),
                TuningString("B", 3, 246.94),
                TuningString("D", 4, 293.66)
            )
        ),
        TuningPreset(
            name = "Standard C (gCGBD)",
            categoryId = "banjo",
            strings = listOf(
                TuningString("g", 4, 392.00),
                TuningString("C", 3, 130.81),
                TuningString("G", 3, 196.00),
                TuningString("B", 3, 246.94),
                TuningString("D", 4, 293.66)
            )
        ),
        TuningPreset(
            name = "Double C (gCGCD)",
            categoryId = "banjo",
            strings = listOf(
                TuningString("g", 4, 392.00),
                TuningString("C", 3, 130.81),
                TuningString("G", 3, 196.00),
                TuningString("C", 4, 261.63),
                TuningString("D", 4, 293.66)
            )
        ),

        // === VIOLIN / MANDOLIN ===
        TuningPreset(
            name = "Violin Standard (GDAE)",
            categoryId = "violin",
            strings = listOf(
                TuningString("G", 3, 196.00),
                TuningString("D", 4, 293.66),
                TuningString("A", 4, 440.00),
                TuningString("E", 5, 659.25)
            )
        ),
        TuningPreset(
            name = "Mandolin Standard (GDAE)",
            categoryId = "violin",
            strings = listOf(
                TuningString("G", 3, 196.00),
                TuningString("D", 4, 293.66),
                TuningString("A", 4, 440.00),
                TuningString("E", 5, 659.25)
            )
        ),
        TuningPreset(
            name = "Mandolin Open G (GDGD)",
            categoryId = "violin",
            strings = listOf(
                TuningString("G", 3, 196.00),
                TuningString("D", 4, 293.66),
                TuningString("G", 4, 392.00),
                TuningString("D", 5, 587.33)
            )
        ),
        TuningPreset(
            name = "Violin Cross Tuning (AEAE)",
            categoryId = "violin",
            strings = listOf(
                TuningString("A", 3, 220.00),
                TuningString("E", 4, 329.63),
                TuningString("A", 4, 440.00),
                TuningString("E", 5, 659.25)
            )
        )
    )

    private val _tunerState = MutableStateFlow(TunerState())
    val tunerState: StateFlow<TunerState> = _tunerState.asStateFlow()

    private var captureJob: Job? = null
    private var playbackJob: Job? = null
    private var targetDataLine: TargetDataLine? = null
    private var activePreset: TuningPreset = PRESETS.first()
    private var lockedString: TuningString? = null

    fun setPreset(preset: TuningPreset) {
        activePreset = preset
        lockedString = null
    }

    fun setLockedString(str: TuningString?) {
        lockedString = str
    }

    fun startListening() {
        if (captureJob?.isActive == true) return

        _tunerState.value = _tunerState.value.copy(isListening = true, statusMessage = "Listening for audio...")

        captureJob = CoroutineScope(Dispatchers.IO).launch {
            val sampleRate = 44100f
            val bufferSize = 4096
            val format = AudioFormat(sampleRate, 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)

            if (!AudioSystem.isLineSupported(info)) {
                _tunerState.value = _tunerState.value.copy(
                    isListening = false,
                    statusMessage = "Microphone format not supported on this device"
                )
                return@launch
            }

            try {
                val inputDevice = com.gbros.tabslite.data.DesktopDatabase.instance.settingsFlow.value.audioInputDevice
                val line = AudioDeviceManager.openTargetLine(format, bufferSize, inputDevice)
                targetDataLine = line
                line.start()

                val audioBuffer = ByteArray(bufferSize)
                val shortBuffer = ShortArray(bufferSize / 2)

                while (isActive) {
                    val bytesRead = line.read(audioBuffer, 0, audioBuffer.size)
                    if (bytesRead <= 0) continue

                    ByteBuffer.wrap(audioBuffer, 0, bytesRead)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer()
                        .get(shortBuffer, 0, bytesRead / 2)

                    // Calculate RMS Audio Level
                    var sumSquares = 0.0
                    for (i in 0 until bytesRead / 2) {
                        val norm = shortBuffer[i] / 32768.0
                        sumSquares += norm * norm
                    }
                    val rms = sqrt(sumSquares / (bytesRead / 2))
                    val audioLevel = (rms * 5.0).coerceIn(0.0, 1.0)

                    if (rms > 0.015) { // Silence threshold
                        val pitchHz = detectPitchAutocorrelation(shortBuffer, bytesRead / 2, sampleRate)

                        if (pitchHz in 35.0..1000.0) {
                            processDetectedPitch(pitchHz, audioLevel)
                        }
                    } else {
                        // Low signal / decaying
                        _tunerState.value = _tunerState.value.copy(
                            audioLevel = audioLevel,
                            statusMessage = if (_tunerState.value.isInTune) "In Tune!" else "Pluck string..."
                        )
                    }
                }
            } catch (e: Exception) {
                println("Tuner microphone exception: ${e.message}")
                _tunerState.value = _tunerState.value.copy(
                    isListening = false,
                    statusMessage = "Audio input error: ${e.message}"
                )
            } finally {
                try {
                    targetDataLine?.stop()
                    targetDataLine?.close()
                } catch (_: Exception) {}
            }
        }
    }

    fun stopListening() {
        captureJob?.cancel()
        captureJob = null
        try {
            targetDataLine?.stop()
            targetDataLine?.close()
        } catch (_: Exception) {}
        _tunerState.value = _tunerState.value.copy(isListening = false, statusMessage = "Tuner paused")
    }

    private fun processDetectedPitch(pitchHz: Double, audioLevel: Double) {
        // Find closest string in active preset (or use locked string if user selected one)
        val target = lockedString ?: activePreset.strings.minByOrNull { str ->
            abs(1200 * log2(pitchHz / str.frequency))
        } ?: activePreset.strings.first()

        val cents = 1200 * log2(pitchHz / target.frequency)
        val clampedCents = cents.coerceIn(-50.0, 50.0)
        val inTune = abs(cents) <= 3.5

        val status = when {
            inTune -> "PERFECT (IN TUNE)"
            cents < 0 -> "Too Flat (${abs(cents).roundToInt()} cents down)"
            else -> "Too Sharp (${cents.roundToInt()} cents up)"
        }

        _tunerState.value = TunerState(
            isListening = true,
            detectedFrequency = pitchHz,
            targetString = target,
            closestNoteName = target.noteName,
            centsOffset = clampedCents,
            isInTune = inTune,
            audioLevel = audioLevel,
            statusMessage = status
        )
    }

    /**
     * Normalized Square Difference Autocorrelation Pitch Detection
     */
    private fun detectPitchAutocorrelation(audioData: ShortArray, length: Int, sampleRate: Float): Double {
        val minFreq = 38.0
        val maxFreq = 950.0
        val minPeriod = (sampleRate / maxFreq).toInt()
        val maxPeriod = (sampleRate / minFreq).toInt().coerceAtMost(length / 2)

        var bestPeriod = -1
        var bestCorrelation = 0.0

        // Compute energy of first window
        var energy1 = 0.0
        for (i in 0 until length / 2) {
            energy1 += audioData[i].toDouble() * audioData[i]
        }

        for (period in minPeriod..maxPeriod) {
            var sum = 0.0
            var energy2 = 0.0
            for (i in 0 until length / 2) {
                sum += audioData[i].toDouble() * audioData[i + period]
                energy2 += audioData[i + period].toDouble() * audioData[i + period]
            }

            val denom = sqrt(energy1 * energy2)
            if (denom > 0) {
                val normalizedCorr = sum / denom
                if (normalizedCorr > bestCorrelation && normalizedCorr > 0.70) {
                    bestCorrelation = normalizedCorr
                    bestPeriod = period
                }
            }
        }

        if (bestPeriod == -1) return 0.0

        // Parabolic interpolation around peak
        return (sampleRate / bestPeriod.toDouble())
    }

    /**
     * Plays an authentic natural instrument tone matching the Learn Hub experience,
     * utilizing real sampled acoustic guitar, ukulele, bass, banjo, violin, and mandolin programs.
     * Automatically pauses previously playing notes and uses short, crisp durations for violin and mandolin.
     */
    fun playStringTone(
        string: TuningString,
        categoryId: String = "guitar_6",
        presetName: String = "",
        durationMs: Long? = null
    ) {
        val program = getProgramForCategory(categoryId, presetName)
        val pitch = getMidiPitch(string)
        val duration = durationMs ?: when {
            presetName.contains("mandolin", ignoreCase = true) -> 650L // Crisp mandolin pluck
            categoryId.equals("violin", true) -> 800L                   // Short crisp violin stroke
            categoryId.equals("banjo", true) -> 1400L
            categoryId.equals("ukulele", true) -> 1600L
            else -> 2000L
        }
        MusicTheoryAudioPlayer.playTunerTone(pitch, program, duration)
    }

    /**
     * Overload for playing natural tones directly from frequency or legacy calls
     */
    fun playTone(
        frequency: Double,
        durationMs: Long? = null,
        categoryId: String = "guitar_6",
        presetName: String = ""
    ) {
        val program = getProgramForCategory(categoryId, presetName)
        val pitch = if (frequency > 0.0) {
            (69.0 + 12.0 * kotlin.math.log2(frequency / 440.0)).roundToInt().coerceIn(12, 108)
        } else 60
        val duration = durationMs ?: when {
            presetName.contains("mandolin", ignoreCase = true) -> 650L
            categoryId.equals("violin", true) -> 800L
            categoryId.equals("banjo", true) -> 1400L
            categoryId.equals("ukulele", true) -> 1600L
            else -> 2000L
        }
        MusicTheoryAudioPlayer.playTunerTone(pitch, program, duration)
    }

    private fun getProgramForCategory(categoryId: String, presetName: String = ""): Int {
        val isMandolin = presetName.contains("mandolin", ignoreCase = true) || categoryId.contains("mandolin", ignoreCase = true)
        return when {
            isMandolin -> 45 // Pizzicato Strings / Mandolin Pluck
            categoryId.lowercase() == "violin" -> 40 // Violin
            categoryId.lowercase() == "ukulele" -> 24 // Acoustic Guitar (Nylon) / Ukulele
            categoryId.lowercase() == "bass" -> 33    // Electric / Acoustic Bass
            categoryId.lowercase() == "banjo" -> 105  // Banjo
            else -> 25      // 6-String and 7-String Acoustic Guitar (Steel)
        }
    }

    private fun getMidiPitch(string: TuningString): Int {
        val clean = string.name.trim()
        val pitchClass = when {
            clean.equals("C", true) -> 0
            clean.equals("C#", true) || clean.equals("Db", true) -> 1
            clean.equals("D", true) -> 2
            clean.equals("D#", true) || clean.equals("Eb", true) -> 3
            clean.equals("E", true) -> 4
            clean.equals("F", true) -> 5
            clean.equals("F#", true) || clean.equals("Gb", true) -> 6
            clean.equals("G", true) || clean.equals("g", true) -> 7
            clean.equals("G#", true) || clean.equals("Ab", true) -> 8
            clean.equals("A", true) -> 9
            clean.equals("A#", true) || clean.equals("Bb", true) -> 10
            clean.equals("B", true) -> 11
            else -> (69.0 + 12.0 * kotlin.math.log2(string.frequency / 440.0)).roundToInt() % 12
        }
        return ((string.octave + 1) * 12 + pitchClass).coerceIn(12, 108)
    }

    fun stopPlayback() {
        MusicTheoryAudioPlayer.stop()
    }
}
