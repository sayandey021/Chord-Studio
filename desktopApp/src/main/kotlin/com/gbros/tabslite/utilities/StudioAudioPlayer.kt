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

                val overlapSize = (sampleRate * 0.016f).toInt().coerceAtLeast(64)
                val windowSize = (sampleRate * 0.064f).toInt().coerceAtLeast(overlapSize * 2)
                val hopSize = windowSize - overlapSize
                val searchRange = (sampleRate * 0.012f).toInt().coerceAtLeast(32)

                val overlapBuffer = FloatArray(overlapSize)
                var overlapBufferValid = false

                val crossfadeWeights = FloatArray(overlapSize) { i ->
                    (0.5 * (1.0 - kotlin.math.cos(Math.PI * i / overlapSize))).toFloat()
                }

                val STAGING_CAPACITY = 8192
                val stagingBuffer = FloatArray(STAGING_CAPACITY)
                var stagingHead = 0
                var stagingTail = 0
                var stagingCount = 0

                val bufferSize = 512
                val byteBuffer = ByteArray(bufferSize * 2)

                while (isPlaying && isActive) {
                    if (currentSamplePos >= audioSamples.size && stagingCount == 0) {
                        if (isLooping) {
                            currentSamplePos = 0.0
                            currentPlayTimeSeconds = 0.0
                            overlapBufferValid = false
                            stagingHead = 0
                            stagingTail = 0
                            stagingCount = 0
                        } else {
                            break
                        }
                    }

                    val effectiveVolume = if (isMuted) 0f else volume
                    val currentSpeed = speed.coerceIn(0.25f, 3.0f).toDouble()
                    val currentPitch = pitchShift
                    val pitchRatio = if (currentPitch != 0) {
                        Math.pow(2.0, currentPitch.toDouble() / 12.0)
                    } else {
                        1.0
                    }

                    var byteIdx = 0
                    var framesFilled = 0

                    if (currentPitch == 0 && currentSpeed == 1.0) {
                        // Direct lossless streaming with 0 processing latency
                        while (framesFilled < bufferSize && stagingCount > 0) {
                            val rawSample = stagingBuffer[stagingTail]
                            stagingTail = (stagingTail + 1) % STAGING_CAPACITY
                            stagingCount--
                            val scaled = (rawSample * effectiveVolume).coerceIn(-1.0f, 1.0f)
                            val pcmShort = (scaled * 32767.0f).toInt().coerceIn(-32768, 32767)
                            byteBuffer[byteIdx++] = (pcmShort and 0xFF).toByte()
                            byteBuffer[byteIdx++] = ((pcmShort shr 8) and 0xFF).toByte()
                            framesFilled++
                        }
                        while (framesFilled < bufferSize && currentSamplePos < audioSamples.size) {
                            val idx = currentSamplePos.toInt()
                            val rawSample = if (idx in audioSamples.indices) audioSamples[idx] else 0f
                            val scaled = (rawSample * effectiveVolume).coerceIn(-1.0f, 1.0f)
                            val pcmShort = (scaled * 32767.0f).toInt().coerceIn(-32768, 32767)
                            byteBuffer[byteIdx++] = (pcmShort and 0xFF).toByte()
                            byteBuffer[byteIdx++] = ((pcmShort shr 8) and 0xFF).toByte()
                            currentSamplePos += 1.0
                            framesFilled++
                        }
                        overlapBufferValid = false
                    } else {
                        // High-Fidelity SOLA (Synchronized Overlap-Add) Pitch & Speed Engine
                        while (stagingCount < bufferSize && currentSamplePos < audioSamples.size) {
                            var bestOffset = 0
                            if (overlapBufferValid) {
                                var minDiff = Float.MAX_VALUE
                                val minD = (-searchRange).coerceAtLeast((-currentSamplePos / pitchRatio).toInt())
                                val maxD = searchRange.coerceAtMost(((audioSamples.size - 1 - currentSamplePos) / pitchRatio).toInt())

                                var d = minD
                                while (d <= maxD) {
                                    var diff = 0f
                                    var k = 0
                                    while (k < overlapSize) {
                                        val sCandidate = sampleAt(currentSamplePos + (d + k) * pitchRatio)
                                        diff += kotlin.math.abs(overlapBuffer[k] - sCandidate)
                                        if (diff >= minDiff) break
                                        k += 2
                                    }
                                    if (diff < minDiff) {
                                        minDiff = diff
                                        bestOffset = d
                                    }
                                    d += 2
                                }
                            }

                            val grainAnchor = currentSamplePos + bestOffset * pitchRatio

                            for (k in 0 until hopSize) {
                                val sNew = sampleAt(grainAnchor + k * pitchRatio)
                                val outSample = if (k < overlapSize && overlapBufferValid) {
                                    val w = crossfadeWeights[k]
                                    (1f - w) * overlapBuffer[k] + w * sNew
                                } else {
                                    sNew
                                }
                                if (stagingCount < STAGING_CAPACITY) {
                                    stagingBuffer[stagingHead] = outSample
                                    stagingHead = (stagingHead + 1) % STAGING_CAPACITY
                                    stagingCount++
                                }
                            }

                            for (k in 0 until overlapSize) {
                                overlapBuffer[k] = sampleAt(grainAnchor + (hopSize + k) * pitchRatio)
                            }
                            overlapBufferValid = true

                            currentSamplePos += hopSize * currentSpeed
                        }

                        while (framesFilled < bufferSize && stagingCount > 0) {
                            val rawSample = stagingBuffer[stagingTail]
                            stagingTail = (stagingTail + 1) % STAGING_CAPACITY
                            stagingCount--

                            val scaled = (rawSample * effectiveVolume).coerceIn(-1.0f, 1.0f)
                            val pcmShort = (scaled * 32767.0f).toInt().coerceIn(-32768, 32767)

                            byteBuffer[byteIdx++] = (pcmShort and 0xFF).toByte()
                            byteBuffer[byteIdx++] = ((pcmShort shr 8) and 0xFF).toByte()

                            framesFilled++
                        }
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
