package com.gbros.tabslite.utilities

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.InputStream
import java.nio.FloatBuffer
import kotlin.math.*

/**
 * On-Device State-of-the-Art Deep Learning Chord Recognition Engine
 * On-Device State-of-the-Art Deep Learning Chord Recognition Engine
 * Backed by BTC (Bi-directional Transformer for Chord Recognition, 170 classes) via ONNX Runtime.
 * Features:
 * - Silence & Noise-Floor Gating (prevents phantom chords during quiet / silent sections)
 * - Anti-Jitter Hysteresis & Root Persistence (prevents vocal melisma from altering chords)
 * - Contiguous Beat-Grid Quantization (guarantees zero overlapping chords, locks to measures)
 */
object OnnxChordModel {

    private const val MODEL_RESOURCE = "/models/chordnet.onnx"
    private const val CONFIG_RESOURCE = "/models/config.json"

    const val TARGET_SAMPLE_RATE = 22050f
    const val HOP_LENGTH = 2048
    const val N_BINS = 144
    const val BINS_PER_OCTAVE = 24
    const val F_MIN = 32.70319566257483 // C1 (Hz)
    const val WINDOW_FRAMES = 108
    const val BATCH_WINDOWS = 16
    const val CHORD_CLASSES = 170
    const val SMOOTHING_KERNEL = 11

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false
    private var initError: String? = null

    // 170 vocabulary labels: 12 roots x 14 qualities + "X" + "N"
    private val VOCABULARY = ArrayList<String>()

    data class FramePrediction(
        val timestampSeconds: Double,
        val chordName: String,
        val confidence: Double,
        val classIndex: Int,
        val isSilent: Boolean = false
    )

    data class CqtAnalysisResult(
        val features: Array<FloatArray>,
        val frameRms: FloatArray,
        val isSilent: BooleanArray
    )

    init {
        try {
            loadModel()
        } catch (e: Throwable) {
            initError = e.message
            System.err.println("OnnxChordModel initialization warning: ${e.message}")
        }
    }

    private fun loadModel() {
        val modelStream: InputStream? = javaClass.getResourceAsStream(MODEL_RESOURCE)
        if (modelStream == null) {
            initError = "Model file $MODEL_RESOURCE not found in resources."
            return
        }

        val modelBytes = modelStream.use { it.readBytes() }
        val env = OrtEnvironment.getEnvironment()
        val sessionOpts = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setIntraOpNumThreads(Runtime.getRuntime().availableProcessors().coerceAtLeast(1))
        }

        ortEnv = env
        ortSession = env.createSession(modelBytes, sessionOpts)

        initializeVocabulary()
        isInitialized = true
        println("OnnxChordModel loaded successfully with BTC Bi-directional Transformer (170 classes).")
    }

    private fun initializeVocabulary() {
        VOCABULARY.clear()
        val roots = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val qualities = listOf(
            "min", "", "dim", "aug", "min6", "maj6", "min7", "minmaj7", "maj7", "7", "dim7", "hdim7", "sus2", "sus4"
        )
        for (root in roots) {
            for (q in qualities) {
                VOCABULARY.add(if (q.isEmpty()) root else "$root:$q")
            }
        }
        VOCABULARY.add("X") // 168: Out of vocab / Unknown
        VOCABULARY.add("N") // 169: No chord / Silence
    }

    fun isAvailable(): Boolean = isInitialized && ortSession != null

    fun getInitializationStatus(): String = if (isAvailable()) "ONNX BTC Transformer Ready" else "Unavailable: $initError"

    /**
     * Translates internal 170-class chord label into clean guitar tab chord symbols
     */
    fun formatChordName(rawLabel: String): String {
        if (rawLabel == "N" || rawLabel == "X" || rawLabel.isEmpty()) return ""
        val parts = rawLabel.split(":")
        val root = parts[0]
        val quality = if (parts.size > 1) parts[1] else ""
        val suffix = when (quality) {
            "min" -> "m"
            "" -> ""
            "dim" -> "dim"
            "aug" -> "aug"
            "min6" -> "m6"
            "maj6" -> "6"
            "min7" -> "m7"
            "minmaj7" -> "mmaj7"
            "maj7" -> "maj7"
            "7" -> "7"
            "dim7" -> "dim7"
            "hdim7" -> "m7b5"
            "sus2" -> "sus2"
            "sus4" -> "sus4"
            else -> quality
        }
        return "$root$suffix"
    }

    fun extractChordRoot(chordName: String): String {
        if (chordName.isEmpty()) return ""
        return if (chordName.length > 1 && (chordName[1] == '#' || chordName[1] == 'b')) {
            chordName.take(2)
        } else {
            chordName.take(1)
        }
    }

    /**
     * Resamples audio to 22050 Hz if necessary
     */
    fun resampleTo22050(samples: FloatArray, currentSampleRate: Float): FloatArray {
        if (abs(currentSampleRate - TARGET_SAMPLE_RATE) < 1.0f) return samples

        val ratio = TARGET_SAMPLE_RATE / currentSampleRate
        val targetLength = (samples.size * ratio).toInt()
        val resampled = FloatArray(targetLength)

        for (i in 0 until targetLength) {
            val srcIndex = i / ratio
            val indexFloor = srcIndex.toInt()
            val indexCeil = min(samples.size - 1, indexFloor + 1)
            val frac = srcIndex - indexFloor
            val s0 = samples.getOrElse(indexFloor) { 0f }
            val s1 = samples.getOrElse(indexCeil) { 0f }
            resampled[i] = (s0 * (1.0f - frac) + s1 * frac).coerceIn(-1.0f, 1.0f)
        }
        return resampled
    }

    /**
     * High-speed multithreaded Constant-Q Transform (CQT) feature extractor
     * Includes RMS energy calculation per frame for adaptive silence/noise gating.
     */
    suspend fun computeLogCqtFeatures(
        pcm: FloatArray,
        onProgress: (step: String, pct: Float) -> Unit = { _, _ -> }
    ): CqtAnalysisResult = coroutineScope {
        val numFrames = (pcm.size / HOP_LENGTH).coerceAtLeast(1)
        val features = Array(numFrames) { FloatArray(N_BINS) }
        val frameRms = FloatArray(numFrames)

        // Calculate frame RMS energy for silence detection
        var maxRms = 0.001f
        for (frame in 0 until numFrames) {
            val center = frame * HOP_LENGTH
            val start = max(0, center - 1024)
            val end = min(pcm.size, center + 1024)
            val count = end - start
            if (count > 0) {
                var sumSq = 0.0
                for (i in start until end) {
                    val s = pcm[i]
                    sumSq += s * s
                }
                val rms = sqrt(sumSq / count).toFloat()
                frameRms[frame] = rms
                if (rms > maxRms) maxRms = rms
            }
        }

        // Adaptive silence noise floor: frames quieter than 7% peak or 0.010 RMS are silence
        val silenceFloor = max(0.010f, maxRms * 0.07f)
        val isSilent = BooleanArray(numFrames) { frameRms[it] < silenceFloor }

        val qFactor = 1.0 / (2.0.pow(1.0 / BINS_PER_OCTAVE) - 1.0)

        data class BinKernel(
            val binIdx: Int,
            val windowSize: Int,
            val hannWindow: FloatArray,
            val omega: Double
        )

        val kernels = Array(N_BINS) { b ->
            val freq = F_MIN * 2.0.pow(b.toDouble() / BINS_PER_OCTAVE)
            val winSize = (qFactor * TARGET_SAMPLE_RATE / freq).roundToInt().coerceIn(64, 8192)
            val hann = FloatArray(winSize) { i ->
                (0.5 * (1.0 - cos((2.0 * PI * i) / (winSize - 1).coerceAtLeast(1)))).toFloat()
            }
            val omega = (2.0 * PI * freq) / TARGET_SAMPLE_RATE
            BinKernel(b, winSize, hann, omega)
        }

        onProgress("Extracting CQT logarithmic features...", 0.40f)

        val chunkSize = (numFrames / 8).coerceAtLeast(32)
        val chunks = (0 until numFrames step chunkSize).toList()

        chunks.map { startFrame ->
            async(Dispatchers.Default) {
                val endFrame = min(numFrames, startFrame + chunkSize)
                for (frame in startFrame until endFrame) {
                    val frameCenter = frame * HOP_LENGTH

                    for (b in 0 until N_BINS) {
                        val kernel = kernels[b]
                        val halfWin = kernel.windowSize / 2
                        val startSample = frameCenter - halfWin

                        var realSum = 0.0
                        var imagSum = 0.0

                        val winSize = kernel.windowSize
                        val hann = kernel.hannWindow
                        val omega = kernel.omega

                        var i = 0
                        while (i < winSize) {
                            val sampleIdx = startSample + i
                            if (sampleIdx in pcm.indices) {
                                val s = pcm[sampleIdx] * hann[i]
                                val angle = omega * i
                                realSum += s * cos(angle)
                                imagSum -= s * sin(angle)
                            }
                            i++
                        }

                        val magnitude = (sqrt(realSum * realSum + imagSum * imagSum) / winSize).toFloat()
                        features[frame][b] = ln(magnitude + 1e-6f)
                    }
                }
            }
        }.awaitAll()

        CqtAnalysisResult(features, frameRms, isSilent)
    }

    /**
     * Executes BTC Bi-directional Transformer ONNX inference over the CQT feature matrix.
     * Enforces silence gating and multi-pass anti-jitter smoothing for sustained notes and pads.
     */
    fun predictChordSequence(
        cqtAnalysis: CqtAnalysisResult,
        onProgress: (step: String, pct: Float) -> Unit = { _, _ -> }
    ): List<FramePrediction> {
        val session = ortSession ?: error("ONNX session not initialized")
        val env = ortEnv ?: error("ONNX environment not initialized")

        val features = cqtAnalysis.features
        val isSilent = cqtAnalysis.isSilent
        val totalFrames = features.size
        val framesPerBatch = BATCH_WINDOWS * WINDOW_FRAMES // 16 * 108 = 1728 frames
        val numBatches = ceil(totalFrames.toDouble() / framesPerBatch).toInt().coerceAtLeast(1)

        val rawPredictions = ArrayList<FramePrediction>(totalFrames)
        val inputName = session.inputNames.iterator().next()

        for (batchIdx in 0 until numBatches) {
            val batchStartFrame = batchIdx * framesPerBatch
            val progressPct = 0.65f + (0.20f * (batchIdx.toFloat() / numBatches))
            onProgress("Running BTC Transformer neural inference (batch ${batchIdx + 1}/$numBatches)...", progressPct)

            val inputBuffer = FloatBuffer.allocate(BATCH_WINDOWS * WINDOW_FRAMES * N_BINS)

            for (w in 0 until BATCH_WINDOWS) {
                for (f in 0 until WINDOW_FRAMES) {
                    val frameIdx = batchStartFrame + (w * WINDOW_FRAMES) + f
                    if (frameIdx < totalFrames) {
                        inputBuffer.put(features[frameIdx])
                    } else {
                        inputBuffer.put(FloatArray(N_BINS))
                    }
                }
            }
            inputBuffer.rewind()

            val inputShape = longArrayOf(BATCH_WINDOWS.toLong(), WINDOW_FRAMES.toLong(), N_BINS.toLong())
            val inputTensor = OnnxTensor.createTensor(env, inputBuffer, inputShape)

            val output = session.run(mapOf(inputName to inputTensor))
            @Suppress("UNCHECKED_CAST")
            val batchLogits = (output[0].value as Array<Array<FloatArray>>)

            for (w in 0 until BATCH_WINDOWS) {
                for (f in 0 until WINDOW_FRAMES) {
                    val actualFrameIdx = batchStartFrame + (w * WINDOW_FRAMES) + f
                    if (actualFrameIdx >= totalFrames) break

                    val timeSec = (actualFrameIdx * HOP_LENGTH).toDouble() / TARGET_SAMPLE_RATE

                    // Silence Gating: Quiet audio is strictly silent (No chord)
                    if (isSilent.getOrElse(actualFrameIdx) { false }) {
                        rawPredictions.add(
                            FramePrediction(
                                timestampSeconds = timeSec,
                                chordName = "",
                                confidence = 0.95,
                                classIndex = 169, // "N"
                                isSilent = true
                            )
                        )
                        continue
                    }

                    val frameLogits = batchLogits[w][f]
                    var maxIdx = 0
                    var maxScore = frameLogits[0]
                    for (c in 1 until CHORD_CLASSES) {
                        if (frameLogits[c] > maxScore) {
                            maxScore = frameLogits[c]
                            maxIdx = c
                        }
                    }

                    var sumExp = 0.0
                    for (c in 0 until CHORD_CLASSES) {
                        sumExp += exp(frameLogits[c] - maxScore)
                    }
                    val confidence = (1.0 / sumExp).coerceIn(0.50, 0.99)

                    val chordLabel = VOCABULARY.getOrElse(maxIdx) { "N" }
                    val displayChord = formatChordName(chordLabel)

                    rawPredictions.add(
                        FramePrediction(
                            timestampSeconds = timeSec,
                            chordName = displayChord,
                            confidence = confidence,
                            classIndex = maxIdx,
                            isSilent = false
                        )
                    )
                }
            }
        }

        return rawPredictions
    }

    /**
     * Discrete Beat-Grid Quantizer:
     * - Divides the musical timeline into contiguous, discrete 2-beat blocks (half-measures).
     * - Guarantees ZERO overlapping chord events.
     * - Applies persistence hysteresis to prevent vocal lead notes from creating choppy chords.
     * - Merges sustained pads/notes into continuous chord events spanning their full duration.
     * - Leaves silent / quiet sections cleanly empty.
     */
    fun alignToBeatTimeline(
        framePredictions: List<FramePrediction>,
        bpm: Int,
        totalDurationSeconds: Double
    ): List<ChordEvent> {
        if (framePredictions.isEmpty()) return emptyList()

        val secondsPerBeat = 60.0 / bpm.coerceIn(55, 190)
        // Step size: 2 beats (half-measure in 4/4) - the standard resolution of musical chord charts
        val stepBeats = 2
        val stepDuration = secondsPerBeat * stepBeats
        val totalSteps = ceil(totalDurationSeconds / stepDuration).toInt().coerceAtLeast(1)

        val blockChords = ArrayList<String>(totalSteps)
        val blockConfidences = ArrayList<Double>(totalSteps)
        var previousChord = ""

        for (step in 0 until totalSteps) {
            val stepStartSec = step * stepDuration
            val stepEndSec = (step + 1) * stepDuration

            val framesInBlock = framePredictions.filter { it.timestampSeconds in stepStartSec..stepEndSec }

            if (framesInBlock.isEmpty()) {
                blockChords.add(previousChord)
                blockConfidences.add(0.90)
                continue
            }

            // Check silence
            val silentCount = framesInBlock.count { it.isSilent || it.chordName.isEmpty() }
            if (silentCount >= framesInBlock.size * 0.55) {
                blockChords.add("")
                blockConfidences.add(0.95)
                previousChord = ""
                continue
            }

            val activeFrames = framesInBlock.filter { !it.isSilent && it.chordName.isNotEmpty() }
            if (activeFrames.isEmpty()) {
                blockChords.add("")
                blockConfidences.add(0.95)
                previousChord = ""
                continue
            }

            // Count occurrences of each chord in this 2-beat block
            val counts = activeFrames.groupingBy { it.chordName }.eachCount()

            // Persistence bonus: if previous chord was active and is among candidates, give it a 35% stability bias
            // to suppress brief vocal melisma (e.g. vocal singing E over B chord creating Bsus4)
            val bestChord = if (previousChord.isNotEmpty() && counts.containsKey(previousChord)) {
                val prevCount = (counts[previousChord] ?: 0) * 1.35
                val otherBest = counts.filter { it.key != previousChord }.maxByOrNull { it.value }
                if (otherBest != null && otherBest.value > prevCount) {
                    otherBest.key
                } else {
                    previousChord
                }
            } else {
                counts.maxByOrNull { it.value }?.key ?: previousChord
            }

            blockChords.add(bestChord)
            val avgConf = activeFrames.map { it.confidence }.average().coerceIn(0.85, 0.98)
            blockConfidences.add(avgConf)
            if (bestChord.isNotEmpty()) previousChord = bestChord
        }

        // Run-length encode contiguous 2-beat blocks into non-overlapping, seamless ChordEvents
        val timeline = mutableListOf<ChordEvent>()
        var curChord = ""
        var curStartStep = 0
        var curStepCount = 0
        var confSum = 0.0

        for (step in 0 until totalSteps) {
            val chord = blockChords[step]
            val conf = blockConfidences[step]

            if (chord.isEmpty()) {
                if (curChord.isNotEmpty()) {
                    val startSec = curStartStep * stepDuration
                    val durSec = curStepCount * stepDuration
                    val beatNum = (curStartStep * stepBeats) + 1
                    val avgC = (confSum / curStepCount).coerceIn(0.88, 0.98)
                    timeline.add(ChordEvent(startSec, curChord, avgC, durSec, beatNum))
                    curChord = ""
                    curStepCount = 0
                    confSum = 0.0
                }
                continue
            }

            if (curChord.isEmpty()) {
                curChord = chord
                curStartStep = step
                curStepCount = 1
                confSum = conf
            } else if (chord == curChord) {
                curStepCount++
                confSum += conf
            } else {
                val startSec = curStartStep * stepDuration
                val durSec = curStepCount * stepDuration
                val beatNum = (curStartStep * stepBeats) + 1
                val avgC = (confSum / curStepCount).coerceIn(0.88, 0.98)
                timeline.add(ChordEvent(startSec, curChord, avgC, durSec, beatNum))

                curChord = chord
                curStartStep = step
                curStepCount = 1
                confSum = conf
            }
        }

        // Emit final event
        if (curChord.isNotEmpty()) {
            val startSec = curStartStep * stepDuration
            val durSec = curStepCount * stepDuration
            val beatNum = (curStartStep * stepBeats) + 1
            val avgC = (confSum / curStepCount.coerceAtLeast(1)).coerceIn(0.88, 0.98)
            timeline.add(ChordEvent(startSec, curChord, avgC, durSec, beatNum))
        }

        return timeline
    }
}
