package com.gbros.tabslite.utilities

import kotlinx.coroutines.*
import javax.sound.sampled.*
import javax.swing.SwingUtilities
import kotlin.math.min

/**
 * 100% On-Device Real-Time Studio Audio Player
 * Streams PCM samples directly through the PC sound card with sample-accurate playback clock.
 */
object StudioAudioPlayer {
    private var dataLine: SourceDataLine? = null
    private var playbackJob: Job? = null
    private var audioSamples: FloatArray = FloatArray(0)
    private var sampleRate: Float = 16000f

    @Volatile
    var isPlaying: Boolean = false
        private set

    @Volatile
    var currentPlayTimeSeconds: Double = 0.0
        private set

    @Volatile
    var volume: Float = 0.85f

    @Volatile
    var isMuted: Boolean = false

    @Volatile
    var isLooping: Boolean = false

    @Volatile
    var speed: Float = 1.0f

    @Volatile
    var pitchShift: Int = 0

    private const val GRAIN_WINDOW_SIZE = 1024
    private const val GRAIN_HALF_SIZE = 512
    private val HANN_WINDOW = FloatArray(GRAIN_WINDOW_SIZE) { i ->
        (0.5 * (1.0 - kotlin.math.cos(2.0 * Math.PI * i / GRAIN_WINDOW_SIZE))).toFloat()
    }

    private fun sampleAt(pos: Double): Float {
        if (pos < 0.0) return 0f
        val idx = pos.toInt()
        if (idx >= audioSamples.size - 1) {
            return if (idx in audioSamples.indices) audioSamples[idx] else 0f
        }
        val frac = (pos - idx).toFloat()
        return audioSamples[idx] * (1f - frac) + audioSamples[idx + 1] * frac
    }

    var onProgressUpdate: ((Double) -> Unit)? = null
    var onPlaybackFinished: (() -> Unit)? = null

    fun loadAudio(samples: FloatArray, rate: Float = 16000f) {
        stop()
        audioSamples = samples
        sampleRate = rate
        currentPlayTimeSeconds = 0.0
    }

    fun getDurationSeconds(): Double {
        if (audioSamples.isEmpty() || sampleRate <= 0f) return 0.0
        return audioSamples.size.toDouble() / sampleRate
    }

    fun play(fromSeconds: Double? = null) {
        if (audioSamples.isEmpty()) return
        stop()

        val startSec = fromSeconds ?: currentPlayTimeSeconds
        val totalSec = getDurationSeconds()
        val safeStartSec = if (totalSec > 0.0) startSec.coerceIn(0.0, totalSec) else 0.0
        currentPlayTimeSeconds = safeStartSec

        isPlaying = true

        playbackJob = CoroutineScope(Dispatchers.IO).launch {
            var line: SourceDataLine? = null
            try {
                val format = AudioFormat(sampleRate, 16, 1, true, false)
                val outputDevice = com.gbros.tabslite.data.DesktopDatabase.instance.settingsFlow.value.audioOutputDevice
                line = AudioDeviceManager.openSourceLine(format, 4096, outputDevice)
                line.start()
                dataLine = line

                var currentSamplePos = safeStartSec * sampleRate
                var grainPhase = 0
                var grain0Anchor = currentSamplePos
                val initialRatio = if (pitchShift != 0) Math.pow(2.0, pitchShift.toDouble() / 12.0) else 1.0
                var grain1Anchor = currentSamplePos - (GRAIN_HALF_SIZE * initialRatio)

                val bufferSize = 512
                val byteBuffer = ByteArray(bufferSize * 2)

                while (isPlaying && isActive) {
                    if (currentSamplePos >= audioSamples.size) {
                        if (isLooping) {
                            currentSamplePos = 0.0
                            currentPlayTimeSeconds = 0.0
                            grainPhase = 0
                            grain0Anchor = 0.0
                            grain1Anchor = - (GRAIN_HALF_SIZE * initialRatio)
                        } else {
                            break
                        }
                    }

                    val effectiveVolume = if (isMuted) 0f else volume
                    val currentSpeed = speed.coerceIn(0.25f, 3.0f)
                    val currentPitch = pitchShift
                    val pitchRatio = if (currentPitch != 0) {
                        Math.pow(2.0, currentPitch.toDouble() / 12.0)
                    } else {
                        1.0
                    }

                    var byteIdx = 0
                    var framesFilled = 0

                    while (framesFilled < bufferSize && currentSamplePos < audioSamples.size) {
                        val rawSample: Float
                        if (currentPitch == 0) {
                            val idx = currentSamplePos.toInt()
                            rawSample = if (idx in audioSamples.indices) audioSamples[idx] else 0f
                            grainPhase = 0
                            grain0Anchor = currentSamplePos
                            grain1Anchor = currentSamplePos - GRAIN_HALF_SIZE
                        } else {
                            if (grainPhase == 0) {
                                grain0Anchor = currentSamplePos
                            } else if (grainPhase == GRAIN_HALF_SIZE) {
                                grain1Anchor = currentSamplePos
                            }

                            val p0 = grainPhase
                            val p1 = (grainPhase + GRAIN_HALF_SIZE) % GRAIN_WINDOW_SIZE

                            val s0 = sampleAt(grain0Anchor + p0 * pitchRatio)
                            val s1 = sampleAt(grain1Anchor + p1 * pitchRatio)

                            rawSample = HANN_WINDOW[p0] * s0 + HANN_WINDOW[p1] * s1
                            grainPhase = (grainPhase + 1) % GRAIN_WINDOW_SIZE
                        }

                        val scaled = (rawSample * effectiveVolume).coerceIn(-1.0f, 1.0f)
                        val pcmShort = (scaled * 32767.0f).toInt().coerceIn(-32768, 32767)

                        byteBuffer[byteIdx++] = (pcmShort and 0xFF).toByte()
                        byteBuffer[byteIdx++] = ((pcmShort shr 8) and 0xFF).toByte()

                        currentSamplePos += currentSpeed
                        framesFilled++
                    }

                    if (framesFilled == 0) break

                    val bytesToWrite = framesFilled * 2
                    var written = 0
                    while (written < bytesToWrite && isPlaying && isActive) {
                        val n = line.write(byteBuffer, written, bytesToWrite - written)
                        if (n > 0) written += n else break
                    }

                    currentPlayTimeSeconds = (currentSamplePos / sampleRate).coerceIn(0.0, totalSec)

                    val progressToReport = currentPlayTimeSeconds
                    SwingUtilities.invokeLater {
                        try {
                            onProgressUpdate?.invoke(progressToReport)
                        } catch (_: Exception) {}
                    }
                }

                line.drain()
            } catch (_: Exception) {
                // Coroutine cancellation or seek interrupt
            } finally {
                try {
                    line?.stop()
                    line?.close()
                } catch (_: Exception) {}
                dataLine = null
                if (isPlaying) {
                    isPlaying = false
                    SwingUtilities.invokeLater {
                        try {
                            onPlaybackFinished?.invoke()
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    fun pause() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            dataLine?.stop()
            dataLine?.flush()
            dataLine?.close()
        } catch (_: Exception) {}
        dataLine = null
    }

    fun stop() {
        pause()
    }

    fun seek(seconds: Double) {
        val totalSec = getDurationSeconds()
        val safe = if (totalSec > 0.0) seconds.coerceIn(0.0, totalSec) else 0.0
        val wasPlaying = isPlaying
        stop()
        currentPlayTimeSeconds = safe
        SwingUtilities.invokeLater {
            try {
                onProgressUpdate?.invoke(safe)
            } catch (_: Exception) {}
        }
        if (wasPlaying) {
            play(safe)
        }
    }
}
