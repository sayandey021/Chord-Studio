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
     * Plays an authentic warm acoustic string pluck sound using acoustic modal
     * synthesis coupled with Helmholtz air cavity and spruce wooden soundboard resonance.
     * Eliminates buzzy electric harmonics for a deep, organic acoustic dreadnought and upright tone.
     */
    fun playTone(frequency: Double, durationMs: Int = 2400) {
        playbackJob?.cancel()
        playbackJob = CoroutineScope(Dispatchers.IO).launch {
            var line: SourceDataLine? = null
            try {
                val sampleRate = 44100.0
                val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
                val info = DataLine.Info(SourceDataLine::class.java, format)

                val outputDevice = com.gbros.tabslite.data.DesktopDatabase.instance.settingsFlow.value.audioOutputDevice
                line = AudioDeviceManager.openSourceLine(format, 8192, outputDevice)
                line.start()

                val audioBuffer = ByteArray(4096)
                var bufferByteIdx = 0
                val random = java.util.Random()

                // Acoustic string decay rates based on pitch (lower bass sustains longer)
                val baseDecay = when {
                    frequency < 60.0 -> 1.4    // Deep bass strings
                    frequency < 120.0 -> 1.8   // Low E/A acoustic guitar strings
                    frequency < 250.0 -> 2.3   // D/G strings
                    else -> 2.9                // Treble B/E strings
                }

                // Harmonic amplitude weights for phosphor bronze acoustic strings
                val h1_Amp = 0.85
                val h2_Amp = 0.42
                val h3_Amp = 0.20
                val h4_Amp = 0.08
                val h5_Amp = 0.03

                val h1_Decay = baseDecay
                val h2_Decay = baseDecay * 1.6
                val h3_Decay = baseDecay * 2.8
                val h4_Decay = baseDecay * 4.5
                val h5_Decay = baseDecay * 7.0

                // Wood Body Helmholtz Cavity & Spruce Soundboard Resonances
                val helmholtzFreq = 104.0
                val helmholtzDecay = 16.0
                val helmholtzAmp = if (frequency < 200.0) 0.28 else 0.12

                val spruceFreq = 212.0
                val spruceDecay = 22.0
                val spruceAmp = 0.18

                // Finger / Tortoise Pick soft impact transient
                var pickFilter = 0.0

                for (s in 0 until totalSamples) {
                    if (!isActive) break

                    val t = s / sampleRate

                    // 1. Organic Acoustic String Harmonics (Exponential Modal Decay)
                    val h1 = h1_Amp * exp(-h1_Decay * t) * sin(2.0 * PI * frequency * t)
                    val h2 = h2_Amp * exp(-h2_Decay * t) * sin(2.0 * PI * (frequency * 2.0) * t + 0.2)
                    val h3 = h3_Amp * exp(-h3_Decay * t) * sin(2.0 * PI * (frequency * 3.0) * t + 0.5)
                    val h4 = h4_Amp * exp(-h4_Decay * t) * sin(2.0 * PI * (frequency * 4.0) * t + 0.9)
                    val h5 = h5_Amp * exp(-h5_Decay * t) * sin(2.0 * PI * (frequency * 5.0) * t + 1.3)

                    val stringVibration = h1 + h2 + h3 + h4 + h5

                    // 2. Wooden Acoustic Body Impulse (Soundboard & Cavity "Thump")
                    val bodyHelmholtz = helmholtzAmp * exp(-helmholtzDecay * t) * sin(2.0 * PI * helmholtzFreq * t)
                    val bodySpruce = spruceAmp * exp(-spruceDecay * t) * sin(2.0 * PI * spruceFreq * t)

                    // 3. Soft Finger / Plectrum Touch Transient (< 40 ms)
                    val rawNoise = (random.nextDouble() * 2.0 - 1.0)
                    pickFilter = 0.82 * pickFilter + 0.18 * rawNoise
                    val pickPluck = if (t < 0.05) {
                        0.08 * exp(-55.0 * t) * pickFilter
                    } else {
                        0.0
                    }

                    // Combined Acoustic Signal
                    val combined = stringVibration + bodyHelmholtz + bodySpruce + pickPluck

                    // Gentle smooth fade-out at the very end
                    val fadeOut = if (s > totalSamples - 2500) {
                        (totalSamples - s) / 2500.0
                    } else {
                        1.0
                    }

                    // Soft wooden acoustic compression curve
                    val finalSample = (tanh(combined * 1.15) * fadeOut * 0.92).coerceIn(-1.0, 1.0)
                    val shortSample = (finalSample * 32767.0).roundToInt().toShort()

                    audioBuffer[bufferByteIdx++] = (shortSample.toInt() and 0xFF).toByte()
                    audioBuffer[bufferByteIdx++] = ((shortSample.toInt() shr 8) and 0xFF).toByte()

                    if (bufferByteIdx >= audioBuffer.size) {
                        line.write(audioBuffer, 0, bufferByteIdx)
                        bufferByteIdx = 0
                    }
                }

                if (bufferByteIdx > 0) {
                    line.write(audioBuffer, 0, bufferByteIdx)
                }

                line.drain()
            } catch (e: Exception) {
                // Ignore cancellation
            } finally {
                try {
                    line?.stop()
                    line?.close()
                } catch (_: Exception) {}
            }
        }
    }
}
