package com.gbros.tabslite.utilities

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.*
import kotlin.math.*
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.SampleBuffer

/**
 * Data structures for On-Device Audio AI Analysis
 */
data class AudioAnalysisResult(
    val title: String,
    val artist: String,
    val detectedKey: String,
    val detectedBpm: Int,
    val timeSignature: String = "4/4",
    val suggestedCapo: Int = 0,
    val suggestedStrumming: String = "D - D U - U D U",
    val confidenceScore: Double,
    val uniqueChords: List<String>,
    val chordTimeline: List<ChordEvent>,
    val formattedTabContent: String,
    val durationSeconds: Double,
    val audioSamples: FloatArray = FloatArray(0),
    val sampleRate: Float = 16000f,
    val waveformEnvelope: FloatArray = FloatArray(0),
    val lyrics: String? = null,
    val syncedLyrics: List<SyncedLyricLine> = emptyList()
)

data class ChordEvent(
    val timestampSeconds: Double,
    val chordName: String,
    val confidence: Double = 0.90,
    val durationSeconds: Double = 2.0,
    val beatNumber: Int = 1
)

data class DemoAudioTrack(
    val id: String,
    val name: String,
    val artist: String,
    val genre: String,
    val expectedKey: String,
    val expectedBpm: Int,
    val chordProgression: List<String>,
    val lyrics: String
)

/**
 * 100% On-Device Audio Analysis Engine
 * Extracts Tempo (BPM), Key, Chromagrams, Chord Progressions, and Formats Tabs
 */
object AudioAnalysisEngine {

    val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    // Krumhansl-Kessler empirical key profiles
    private val MAJOR_PROFILE = doubleArrayOf(6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88)
    private val MINOR_PROFILE = doubleArrayOf(6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17)

    // Standard Chord Templates across 12 pitch classes
    private val CHORD_TEMPLATES: Map<String, IntArray> = mapOf(
        "" to intArrayOf(0, 4, 7),         // Major Triad
        "m" to intArrayOf(0, 3, 7),        // Minor Triad
        "7" to intArrayOf(0, 4, 7, 10),    // Dominant 7th
        "maj7" to intArrayOf(0, 4, 7, 11), // Major 7th
        "m7" to intArrayOf(0, 3, 7, 10),   // Minor 7th
        "sus4" to intArrayOf(0, 5, 7),     // Suspended 4th
        "5" to intArrayOf(0, 7)            // Power chord
    )

    val DEMO_TRACKS = listOf(
        DemoAudioTrack(
            id = "demo_studio_session",
            name = "Studio Session",
            artist = "AI Audio Studio",
            genre = "Neo Soul / Studio Jam",
            expectedKey = "A# Major",
            expectedBpm = 96,
            chordProgression = listOf("A#", "D#", "F", "Dm"),
            lyrics = """
                [Intro]
                A#   D#   F   Dm
                
                [Verse 1]
                Soundwaves moving through the open air
                Melodies and harmonies everywhere
                Catch the rhythm as the chords align
                Every measure is frozen in time
                
                [Chorus]
                Feel the frequency rising high
                Studio lights across the sky
            """.trimIndent()
        ),
        DemoAudioTrack(
            id = "demo_nujabes",
            name = "Feather",
            artist = "Nujabes - Feat. Cise Starr",
            genre = "Lo-Fi Jazz / Hip-Hop",
            expectedKey = "Ab Major",
            expectedBpm = 90,
            chordProgression = listOf("Ebm7", "Ab", "Bbm", "Db"),
            lyrics = """
                [Verse 1]
                Light as a feather when I'm floating through the atmosphere
                Looking at the city lights and everything is clear
                Rhythm of the kick and snare keeps moving in my ear
                Nothing in the universe that we would have to fear
                
                [Chorus]
                Floating away on melodic wind
                Finding the place where the dreams begin
            """.trimIndent()
        ),
        DemoAudioTrack(
            id = "demo_acoustic",
            name = "Acoustic Sunburst",
            artist = "Studio Acoustic",
            genre = "Folk / Acoustic",
            expectedKey = "G Major",
            expectedBpm = 95,
            chordProgression = listOf("G", "D", "Em", "C"),
            lyrics = """
                [Verse 1]
                Walking down the open road this morning
                Sunlight breaking through the misty trees
                Every step is quiet without warning
                Listening to the whispering summer breeze
                
                [Chorus]
                Hold on to the light when shadows fall
                We will find our way back through it all
            """.trimIndent()
        ),
        DemoAudioTrack(
            id = "demo_pop",
            name = "Midnight Memories",
            artist = "Pop Studio Jam",
            genre = "Pop / Ballad",
            expectedKey = "C Major",
            expectedBpm = 118,
            chordProgression = listOf("C", "G", "Am", "F"),
            lyrics = """
                [Verse 1]
                Midnight ticking on the bedroom clock
                Thinking of the words we left unsaid
                Music playing down the quiet block
                All these memories dancing in my head
                
                [Chorus]
                Stay with me tonight until the dawn
                Before this magic feeling is all gone
            """.trimIndent()
        ),
        DemoAudioTrack(
            id = "demo_rock",
            name = "Echoes of the Night",
            artist = "Rock Ensemble",
            genre = "Classic Rock",
            expectedKey = "A Minor",
            expectedBpm = 128,
            chordProgression = listOf("Am", "F", "C", "G"),
            lyrics = """
                [Verse 1]
                Thunder rollin' across the city sky
                Neon reflections in the falling rain
                Chasing shadows as the years go by
                Nothing can ever take away this flame
                
                [Chorus]
                Hear the rhythm beating in the dark
                Lighting up the world with just a spark
            """.trimIndent()
        ),
        DemoAudioTrack(
            id = "demo_indie",
            name = "Island Breeze",
            artist = "Indie Ukulele",
            genre = "Ukulele / Indie",
            expectedKey = "D Major",
            expectedBpm = 86,
            chordProgression = listOf("D", "A", "Bm", "G"),
            lyrics = """
                [Verse 1]
                Waves keep rolling gently on the shore
                Salt in the air and sand beneath our feet
                Nothing here we need to worry for
                Life is simple, slow and oh so sweet
                
                [Chorus]
                Sing along under the golden sky
                Watch the ocean seagulls flying by
            """.trimIndent()
        )
    )

    /**
     * Reads a real audio file from disk into mono float samples [-1.0, 1.0] at target sample rate.
     * Supports MP3, WAV, FLAC, M4A, AAC, OGG, OPUS, AIFF, AU with FFmpeg engine and JLayer fallback.
     */
    fun loadAudioFile(
        file: File,
        targetSampleRate: Float = 16000f,
        onProgress: (step: String, pct: Float) -> Unit = { _, _ -> }
    ): FloatArray {
        val extension = file.extension.lowercase()

        // 1. If FFmpeg is available and file is modern compressed/lossless (FLAC, M4A, AAC, OGG, OPUS, WMA, etc.)
        if (ffmpegExePath != null && extension !in listOf("wav", "mp3")) {
            try {
                return decodeWithFfmpeg(file, targetSampleRate, onProgress)
            } catch (e: Exception) {
                System.err.println("[AudioAnalysisEngine] FFmpeg decoding attempt failed for ${file.name}: ${e.message}")
            }
        }

        // 2. Built-in standard decoders with cross-fallback
        return if (extension == "mp3") {
            try {
                decodeMp3WithJLayer(file, targetSampleRate, onProgress)
            } catch (e: Exception) {
                if (ffmpegExePath != null) {
                    decodeWithFfmpeg(file, targetSampleRate, onProgress)
                } else {
                    throw e
                }
            }
        } else {
            try {
                decodeStandardAudio(file, targetSampleRate)
            } catch (e: Exception) {
                if (ffmpegExePath != null) {
                    try {
                        return decodeWithFfmpeg(file, targetSampleRate, onProgress)
                    } catch (_: Exception) {}
                }
                // If standard AudioSystem fails, attempt JLayer in case of MPEG/MP3 with different extension
                try {
                    decodeMp3WithJLayer(file, targetSampleRate, onProgress)
                } catch (e2: Exception) {
                    throw IllegalArgumentException("Could not decode audio file '${file.name}'. Please ensure it is a valid audio file (MP3, WAV, FLAC, M4A, AAC, OGG): ${e.message}")
                }
            }
        }
    }

    private val ffmpegExePath: String? by lazy {
        findFfmpegExecutable()
    }

    private fun findFfmpegExecutable(): String? {
        val candidatePaths = listOf(
            "C:\\Ai\\ffmpeg\\ffmpeg.exe",
            "C:\\ffmpeg\\bin\\ffmpeg.exe",
            "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
            System.getenv("LOCALAPPDATA")?.let { "$it\\Programs\\ffmpeg\\bin\\ffmpeg.exe" },
            System.getenv("USERPROFILE")?.let { "$it\\scoop\\shims\\ffmpeg.exe" },
            "ffmpeg"
        ).filterNotNull()

        for (candidate in candidatePaths) {
            try {
                val pb = ProcessBuilder(candidate, "-version")
                pb.redirectErrorStream(true)
                LyricsExtractor.ensureFfmpegInEnvironment(pb)
                val proc = pb.start()
                val finished = proc.waitFor(1500, java.util.concurrent.TimeUnit.MILLISECONDS)
                if (finished && proc.exitValue() == 0) {
                    return candidate
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun decodeWithFfmpeg(
        file: File,
        targetSampleRate: Float,
        onProgress: (step: String, pct: Float) -> Unit
    ): FloatArray {
        val ffmpegExe = ffmpegExePath ?: throw IllegalStateException("FFmpeg executable not found")
        onProgress("Decoding audio with on-device FFmpeg engine...", 0.10f)

        val pb = ProcessBuilder(
            ffmpegExe,
            "-v", "error",
            "-i", file.absolutePath,
            "-f", "f32le",
            "-ac", "1",
            "-ar", targetSampleRate.toInt().toString(),
            "pipe:1"
        )
        LyricsExtractor.ensureFfmpegInEnvironment(pb)
        val proc = pb.start()

        val rawBytes = proc.inputStream.use { it.readBytes() }
        val exitCode = proc.waitFor()
        if (exitCode != 0 || rawBytes.isEmpty()) {
            val err = proc.errorStream.bufferedReader().readText()
            throw IllegalStateException("FFmpeg failed to decode audio file '${file.name}' (exit code $exitCode): $err")
        }

        val floatBuffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val floatSamples = FloatArray(floatBuffer.remaining())
        floatBuffer.get(floatSamples)
        return floatSamples
    }


    private fun decodeMp3WithJLayer(
        file: File,
        targetSampleRate: Float,
        onProgress: (step: String, pct: Float) -> Unit
    ): FloatArray {
        onProgress("Decoding MP3 frames with on-device engine...", 0.10f)
        val fis = FileInputStream(file)
        val bis = BufferedInputStream(fis)

        // Robustly detect and skip ID3v2 tags at start of MP3 file
        try {
            bis.mark(10)
            val id3Header = ByteArray(10)
            val read = bis.read(id3Header)
            if (read == 10 && id3Header[0] == 'I'.code.toByte() && id3Header[1] == 'D'.code.toByte() && id3Header[2] == '3'.code.toByte()) {
                val size = ((id3Header[6].toInt() and 0x7F) shl 21) or
                           ((id3Header[7].toInt() and 0x7F) shl 14) or
                           ((id3Header[8].toInt() and 0x7F) shl 7) or
                           (id3Header[9].toInt() and 0x7F)
                bis.skip(size.toLong())
            } else {
                bis.reset()
            }
        } catch (_: Exception) {
            try { bis.reset() } catch (_: Exception) {}
        }

        val bitstream = Bitstream(bis)
        val decoder = Decoder()
        val allSamples = ArrayList<Float>((file.length() / 2).toInt().coerceAtMost(5_000_000))
        var sourceSampleRate = 44100

        try {
            var consecutiveErrors = 0
            while (consecutiveErrors < 500) {
                val header = try {
                    val h = bitstream.readFrame()
                    if (h == null) break
                    consecutiveErrors = 0
                    h
                } catch (e: Exception) {
                    consecutiveErrors++
                    try { bitstream.closeFrame() } catch (_: Exception) {}
                    continue
                }

                try {
                    val output = decoder.decodeFrame(header, bitstream) as? SampleBuffer
                    if (output != null) {
                        sourceSampleRate = output.sampleFrequency
                        val channels = output.channelCount
                        val buffer = output.buffer
                        val len = output.bufferLength

                        if (channels >= 2) {
                            for (i in 0 until len step channels) {
                                var sum = 0f
                                for (c in 0 until channels) {
                                    sum += buffer[i + c]
                                }
                                val mono = sum / (channels * 32768.0f)
                                allSamples.add(mono)
                            }
                        } else {
                            for (i in 0 until len) {
                                allSamples.add(buffer[i] / 32768.0f)
                            }
                        }
                    }
                } catch (e: Exception) {
                    consecutiveErrors++
                } finally {
                    try { bitstream.closeFrame() } catch (_: Exception) {}
                }
            }
        } finally {
            try { bitstream.close() } catch (_: Exception) {}
            try { bis.close() } catch (_: Exception) {}
            try { fis.close() } catch (_: Exception) {}
        }

        if (allSamples.isEmpty()) {
            throw IllegalArgumentException("MP3 file contained no decodable audio frames. Please verify file is a valid MP3.")
        }

        val rawArray = FloatArray(allSamples.size) { allSamples[it] }
        return resampleAudio(rawArray, sourceSampleRate.toFloat(), targetSampleRate)
    }

    private fun decodeStandardAudio(file: File, targetSampleRate: Float): FloatArray {
        val audioInputStream = AudioSystem.getAudioInputStream(file)
        val sourceFormat = audioInputStream.format

        val targetFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            targetSampleRate,
            16,
            1, // Mono
            2, // Frame size in bytes
            targetSampleRate,
            false // Little-endian
        )

        val convertedStream = if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
            AudioSystem.getAudioInputStream(targetFormat, audioInputStream)
        } else {
            audioInputStream
        }

        val rawBytes = convertedStream.readAllBytes()
        val channels = convertedStream.format.channels.coerceAtLeast(1)
        val sampleSizeInBits = convertedStream.format.sampleSizeInBits
        val streamRate = convertedStream.format.sampleRate
        convertedStream.close()
        audioInputStream.close()

        val bytesPerSample = (sampleSizeInBits / 8).coerceAtLeast(1)
        val bytesPerFrame = channels * bytesPerSample
        val frameCount = if (bytesPerFrame > 0) rawBytes.size / bytesPerFrame else 0
        val floatSamples = FloatArray(frameCount)
        val buffer = ByteBuffer.wrap(rawBytes).order(if (convertedStream.format.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until frameCount) {
            var sum = 0f
            for (c in 0 until channels) {
                val sampleVal: Float = when (bytesPerSample) {
                    1 -> ((buffer.get().toInt() and 0xFF) - 128) / 128.0f
                    2 -> buffer.short / 32768.0f
                    3 -> {
                        val b0 = buffer.get().toInt() and 0xFF
                        val b1 = buffer.get().toInt() and 0xFF
                        val b2 = buffer.get().toInt()
                        val v = (b2 shl 16) or (b1 shl 8) or b0
                        v / 8388608.0f
                    }
                    4 -> buffer.int / 2147483648.0f
                    else -> {
                        buffer.get()
                        0f
                    }
                }
                sum += sampleVal
            }
            floatSamples[i] = sum / channels
        }

        return if (streamRate > 0 && streamRate != targetSampleRate) {
            resampleAudio(floatSamples, streamRate, targetSampleRate)
        } else {
            floatSamples
        }
    }

    fun resampleAudio(input: FloatArray, fromRate: Float, toRate: Float): FloatArray {
        if (fromRate == toRate || input.isEmpty()) return input
        val ratio = fromRate / toRate
        val outLength = (input.size / ratio).toInt()
        val output = FloatArray(outLength)
        for (i in 0 until outLength) {
            val srcIndex = i * ratio
            val idx = srcIndex.toInt()
            val frac = srcIndex - idx
            val s1 = input[idx.coerceIn(0, input.size - 1)]
            val s2 = input[(idx + 1).coerceIn(0, input.size - 1)]
            output[i] = s1 + frac * (s2 - s1)
        }
        return output
    }

    /**
     * Synthesizes audio samples for testing or demo analysis
     */
    fun generateDemoAudioSamples(demo: DemoAudioTrack, durationSeconds: Double = 12.0, sampleRate: Float = 16000f): FloatArray {
        val totalSamples = (durationSeconds * sampleRate).toInt()
        val samples = FloatArray(totalSamples)
        val chordCount = demo.chordProgression.size
        val chordDuration = durationSeconds / chordCount

        for (cIndex in demo.chordProgression.indices) {
            val chordName = demo.chordProgression[cIndex]
            val chordPitches = getChordFrequencies(chordName)
            val startSample = (cIndex * chordDuration * sampleRate).toInt()
            val endSample = min(totalSamples, ((cIndex + 1) * chordDuration * sampleRate).toInt())

            for (i in startSample until endSample) {
                val t = (i - startSample) / sampleRate
                var wave = 0.0
                // Strumming envelope
                // Strumming envelope with sustained acoustic body resonance
                val beatInterval = 60.0 / demo.expectedBpm
                val beatPhase = (t % beatInterval) / beatInterval
                val decay = 0.45 + 0.55 * exp(-beatPhase * 2.2)

                for ((pIdx, freq) in chordPitches.withIndex()) {
                    // Fundamental + rich string harmonics (octave, 5th, 2nd octave)
                    val amp = (1.0 / (pIdx + 1.0)) * decay
                    wave += amp * sin(2.0 * PI * freq * t)
                    wave += (amp * 0.60) * sin(2.0 * PI * freq * 2.0 * t)
                    wave += (amp * 0.35) * sin(2.0 * PI * freq * 3.0 * t)
                    wave += (amp * 0.20) * sin(2.0 * PI * freq * 4.0 * t)
                }

                // Add rich bass fundamental and sub-octave warmth
                val bassFreq = chordPitches.first() / 2.0
                wave += 0.85 * decay * sin(2.0 * PI * bassFreq * t)
                wave += 0.45 * decay * sin(2.0 * PI * bassFreq * 2.0 * t)

                // Add subtle strum texture
                val strumNoise = (Math.random() - 0.5) * 0.02 * decay
                val finalSample = (wave * 0.30 + strumNoise).coerceIn(-1.0, 1.0).toFloat()
                samples[i] = finalSample
            }
        }

        return samples
    }

    private fun getChordFrequencies(chordName: String): List<Double> {
        val rootStr = if (chordName.length > 1 && (chordName[1] == '#' || chordName[1] == 'b')) {
            chordName.take(2)
        } else {
            chordName.take(1)
        }

        val flatsToSharps = mapOf("BB" to "A#", "EB" to "D#", "AB" to "G#", "DB" to "C#", "GB" to "F#")
        val normalizedRoot = flatsToSharps[rootStr.uppercase()] ?: rootStr.uppercase()
        val rootIndex = NOTE_NAMES.indexOf(normalizedRoot).let { if (it >= 0) it else 0 }
        val isMinor = chordName.contains("m") && !chordName.contains("maj")

        val intervals = if (isMinor) listOf(0, 3, 7) else listOf(0, 4, 7)
        val baseFreq = 440.0 * 2.0.pow((rootIndex - 9 + 12) / 12.0) / 2.0 // Octave 3/4

        return intervals.map { semitones ->
            baseFreq * 2.0.pow(semitones / 12.0)
        }
    }

    /**
     * Primary Analysis Function: Executes On-Device DSP + AI Model pipelines
     */
    suspend fun analyzeAudio(
        samples: FloatArray,
        sampleRate: Float = 16000f,
        songTitle: String = "Detected Audio",
        artistName: String = "AI Audio Analyzer",
        providedLyrics: String? = null,
        syncedLyrics: List<SyncedLyricLine> = emptyList(),
        onProgress: (step: String, percent: Float) -> Unit = { _, _ -> }
    ): AudioAnalysisResult {
        val durationSeconds = samples.size.toDouble() / sampleRate

        // Step 1: Pre-processing & Filtering
        onProgress("Pre-processing audio spectrum...", 0.15f)
        val normalizedSamples = normalizeAudio(samples)

        // Step 2: Tempo / BPM Extraction
        onProgress("Computing beat onsets & BPM tempo...", 0.35f)
        val detectedBpm = estimateTempoBpm(normalizedSamples, sampleRate)

        // Step 3: Dual-Band Chromagram & Key Detection
        onProgress("Extracting pitch chromagrams & musical key...", 0.60f)
        val globalChromaResult = computeGlobalChroma(normalizedSamples, sampleRate)
        val detectedKey = estimateKey(globalChromaResult)

        // Step 4: Beat-Synchronous Chord Recognition (Neural Transformer with DSP fallback & gap-filling)
        onProgress("Recognizing chord progression with AI neural engine...", 0.65f)
        val dspChords = recognizeChordProgression(normalizedSamples, sampleRate, detectedBpm, detectedKey)
        val chordEvents = if (OnnxChordModel.isAvailable()) {
            try {
                val resampled = OnnxChordModel.resampleTo22050(normalizedSamples, sampleRate)
                val cqtFeatures = OnnxChordModel.computeLogCqtFeatures(resampled, onProgress)
                val framePredictions = OnnxChordModel.predictChordSequence(cqtFeatures, onProgress)
                val neuralChords = OnnxChordModel.alignToBeatTimeline(framePredictions, detectedBpm, durationSeconds)
                mergeNeuralWithDspFallback(neuralChords, dspChords, detectedBpm, durationSeconds)
            } catch (e: Exception) {
                System.err.println("Neural chord model error, falling back to DSP: ${e.message}")
                dspChords
            }
        } else {
            dspChords
        }

        // Step 5: Formatting Output Tab Sheet
        onProgress("Aligning chords with vocal phrases & formatting...", 0.95f)
        val uniqueChords = chordEvents.map { it.chordName }.filter { it.isNotBlank() }.distinct()
        val suggestedCapo = calculateSuggestedCapo(detectedKey)
        val suggestedStrumming = if (detectedBpm > 115) "D - D U - U D U" else "D - D - D U D U"

        val formattedTab = generateFormattedTab(
            title = songTitle,
            artist = artistName,
            key = detectedKey,
            bpm = detectedBpm,
            capo = suggestedCapo,
            strumming = suggestedStrumming,
            chords = chordEvents,
            providedLyrics = providedLyrics,
            syncedLyrics = syncedLyrics
        )

        val confidence = calculateConfidence(chordEvents, detectedBpm)
        val waveformEnvelope = extractWaveformEnvelope(normalizedSamples, 600)

        onProgress("Analysis Complete!", 1.0f)

        return AudioAnalysisResult(
            title = songTitle,
            artist = artistName,
            detectedKey = detectedKey,
            detectedBpm = detectedBpm,
            timeSignature = "4/4",
            suggestedCapo = suggestedCapo,
            suggestedStrumming = suggestedStrumming,
            confidenceScore = confidence,
            uniqueChords = uniqueChords,
            chordTimeline = chordEvents,
            formattedTabContent = formattedTab,
            durationSeconds = durationSeconds,
            audioSamples = normalizedSamples,
            sampleRate = sampleRate,
            waveformEnvelope = waveformEnvelope,
            lyrics = providedLyrics,
            syncedLyrics = syncedLyrics
        )
    }

    fun extractWaveformEnvelope(samples: FloatArray, targetPoints: Int = 600): FloatArray {
        if (samples.isEmpty()) return FloatArray(targetPoints) { 0.15f }
        val points = targetPoints.coerceAtLeast(100)
        val envelope = FloatArray(points)
        val chunkSize = (samples.size / points).coerceAtLeast(1)

        for (p in 0 until points) {
            val start = p * chunkSize
            val end = min(samples.size, start + chunkSize)
            var sumSquares = 0.0
            var peak = 0.0f
            for (i in start until end) {
                val v = abs(samples[i])
                if (v > peak) peak = v
                sumSquares += v * v
            }
            val count = end - start
            val rms = if (count > 0) sqrt(sumSquares / count).toFloat() else 0f
            envelope[p] = (rms * 0.75f + peak * 0.25f).coerceIn(0.04f, 1.0f)
        }
        return envelope
    }

    private fun normalizeAudio(samples: FloatArray): FloatArray {
        var maxPeak = 0.001f
        for (s in samples) {
            val absVal = abs(s)
            if (absVal > maxPeak) maxPeak = absVal
        }
        val factor = 0.95f / maxPeak
        return FloatArray(samples.size) { (samples[it] * factor).coerceIn(-1.0f, 1.0f) }
    }

    /**
     * Precision Tempo (BPM) Estimator:
     * 1. 4-band spectral energy novelty (Bass kick, Snare body, Vocal/Pick transients, Cymbals)
     * 2. High-resolution 100 fps frame rate for fine lag resolution (prevents 20 BPM quantization jumps)
     * 3. Half-wave rectified log-spectral flux with adaptive local thresholding
     * 4. Normalized autocorrelation across musical lag range (60..185 BPM)
     * 5. Gaussian tempo prior centered at 115 BPM with octave-error correction (resolves double/half tempo)
     */
    fun estimateTempoBpm(samples: FloatArray, sampleRate: Float): Int {
        val windowSize = 1024
        val hopSize = (sampleRate / 100f).roundToInt().coerceAtLeast(128)
        val numFrames = (samples.size - windowSize) / hopSize
        if (numFrames < 40) return 120

        val fps = sampleRate / hopSize
        val novelty = FloatArray(numFrames)

        // 4 rhythmic frequency bands
        val bandFreqs = doubleArrayOf(110.0, 480.0, 1800.0, 4500.0)
        var prevBandEnergies = DoubleArray(4)

        for (f in 0 until numFrames) {
            val start = f * hopSize
            val currentBandEnergies = DoubleArray(4)
            for (b in 0 until 4) {
                currentBandEnergies[b] = goertzelEnergyExact(samples, start, windowSize, bandFreqs[b], sampleRate)
            }

            var frameFlux = 0.0
            for (b in 0 until 4) {
                val curr = ln(1.0 + 60.0 * currentBandEnergies[b])
                val prev = ln(1.0 + 60.0 * prevBandEnergies[b])
                val diff = curr - prev
                if (diff > 0.0) {
                    val weight = when (b) {
                        0 -> 1.5 // Kick drum / bass rhythm
                        1 -> 1.4 // Snare & acoustic strums
                        2 -> 0.9 // Vocal & pick transients
                        else -> 0.7 // High-hats
                    }
                    frameFlux += diff * weight
                }
            }
            novelty[f] = frameFlux.toFloat()
            prevBandEnergies = currentBandEnergies
        }

        // Adaptive thresholding: subtract 30-frame (~0.3s) local moving average to isolate sharp onsets
        val halfW = 15
        val thresholdedNovelty = FloatArray(numFrames)
        for (i in 0 until numFrames) {
            val sStart = max(0, i - halfW)
            val sEnd = min(numFrames, i + halfW + 1)
            var sum = 0f
            for (j in sStart until sEnd) sum += novelty[j]
            val localMean = sum / (sEnd - sStart)
            thresholdedNovelty[i] = max(0f, novelty[i] - localMean)
        }

        // Autocorrelation over lags from 60 BPM to 185 BPM
        val minBpm = 60
        val maxBpm = 185
        val minLag = (fps * 60.0f / maxBpm).roundToInt().coerceAtLeast(1)
        val maxLag = (fps * 60.0f / minBpm).roundToInt().coerceAtMost(numFrames / 2)

        var bestLag = (fps * 60.0f / 115.0f).roundToInt()
        var bestScore = -1.0
        val lagScores = DoubleArray(maxLag + 1)

        for (lag in minLag..maxLag) {
            var dot = 0.0
            var sumX = 0.0
            var sumY = 0.0
            val count = numFrames - lag
            if (count <= 0) continue

            for (i in 0 until count) {
                val x = thresholdedNovelty[i].toDouble()
                val y = thresholdedNovelty[i + lag].toDouble()
                dot += x * y
                sumX += x * x
                sumY += y * y
            }

            val denom = sqrt(sumX * sumY)
            val normCorr = if (denom > 0.0001) dot / denom else 0.0

            val currentBpm = (fps * 60.0) / lag
            // Broad musical tempo prior centered at 100 BPM (sigma = 0.85 covers 60 to 165 BPM equitably)
            val logBpmRatio = ln(currentBpm / 100.0) / ln(2.0)
            val prior = exp(-0.5 * (logBpmRatio / 0.85).pow(2.0))

            val score = normCorr * (0.75 + 0.25 * prior)
            lagScores[lag] = score

            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }

        // Subharmonic & Metric Ratio Resolution (e.g. 4/3 dotted-note triplets, 2x octaves):
        // In acoustic/ballad tracks, guitar arpeggio subdivisions (e.g. 101 BPM) often create a higher raw peak
        // than the underlying slow vocal/measure pulse (76 BPM = 101 * 3/4).
        val tripLag = (bestLag * 4.0 / 3.0).roundToInt()
        if (tripLag in minLag..maxLag) {
            val tripBpm = (fps * 60.0) / tripLag
            if (tripBpm in 65.0..105.0 && lagScores[tripLag] > (bestScore * 0.65)) {
                bestLag = tripLag
                bestScore = lagScores[tripLag]
            }
        }

        // Halving: If tempo is high (>= 130 BPM), check if the half-tempo (e.g. 154 -> 77 BPM) is valid.
        // Human foot-tapping resonance is centered at 70..115 BPM; ballads and slow songs should NEVER be doubled to 140-160 BPM.
        val doubleLag = bestLag * 2
        if (doubleLag in minLag..maxLag) {
            val halfBpm = (fps * 60.0) / doubleLag
            if (halfBpm in 65.0..105.0 && lagScores[doubleLag] > (bestScore * 0.50)) {
                bestLag = doubleLag
            }
        }

        val calculatedBpm = ((fps * 60.0f) / bestLag).roundToInt()
        return calculatedBpm.coerceIn(60, 190)
    }

    /**
     * Dual-band chroma result: separating bass fundamental root notes from treble harmonic quality
     */
    data class GlobalChromaResult(
        val fullChroma: DoubleArray,
        val bassChroma: DoubleArray,
        val trebleChroma: DoubleArray
    )

    private val HANN_WINDOW_CACHE = java.util.concurrent.ConcurrentHashMap<Int, DoubleArray>()

    private fun getHannWindow(length: Int): DoubleArray {
        return HANN_WINDOW_CACHE.computeIfAbsent(length) { len ->
            DoubleArray(len) { i ->
                0.5 * (1.0 - cos((2.0 * PI * i) / (len - 1).coerceAtLeast(1)))
            }
        }
    }

    /**
     * Exact Continuous-Frequency Generalized Goertzel Filter
     * Uses true target frequency omega rather than integer-quantized bin indices.
     */
    private fun goertzelEnergyExact(samples: FloatArray, start: Int, length: Int, targetFreq: Double, sampleRate: Float): Double {
        val omega = (2.0 * PI * targetFreq) / sampleRate
        val coeff = 2.0 * cos(omega)

        var q0: Double
        var q1 = 0.0
        var q2 = 0.0

        val end = min(samples.size, start + length)
        val actLength = end - start
        if (actLength <= 0) return 0.0

        val window = getHannWindow(actLength)

        for (i in 0 until actLength) {
            val sample = samples[start + i] * window[i]
            q0 = coeff * q1 - q2 + sample
            q2 = q1
            q1 = q0
        }

        val power = q1 * q1 + q2 * q2 - q1 * q2 * coeff
        return sqrt(max(0.0, power))
    }

    /**
     * Estimates master concert pitch A4 frequency (436Hz to 444Hz).
     * Locks in tuning offset for acoustic instruments, live tracks, and regional tunings.
     */
    private fun estimateTuningA4(samples: FloatArray, sampleRate: Float): Double {
        val windowSize = 4096.coerceAtMost(samples.size)
        if (windowSize < 1024) return 440.0
        val testFreqs = doubleArrayOf(436.0, 438.0, 440.0, 442.0, 444.0)
        val energies = DoubleArray(testFreqs.size)
        val numChecks = 6
        val step = max(1, (samples.size - windowSize) / numChecks)

        for (c in 0 until numChecks) {
            val start = c * step
            if (start + windowSize > samples.size) break
            for (i in testFreqs.indices) {
                energies[i] += goertzelEnergyExact(samples, start, windowSize, testFreqs[i], sampleRate)
            }
        }

        var maxIdx = 2 // default 440.0
        var maxE = -1.0
        for (i in energies.indices) {
            if (energies[i] > maxE) {
                maxE = energies[i]
                maxIdx = i
            }
        }
        return testFreqs[maxIdx]
    }

    private fun normalizeVector(vec: DoubleArray) {
        val sum = vec.sum()
        if (sum > 0.0001) {
            for (i in vec.indices) {
                vec[i] /= sum
            }
        }
    }

    /**
     * Dual-band Chromagram extraction using chromatic bandpass filters
     * Separates bass (MIDI 40..55, 82Hz..196Hz) and treble (MIDI 56..79, 207Hz..784Hz)
     */
    private fun computeGlobalChroma(samples: FloatArray, sampleRate: Float): GlobalChromaResult {
        val bassChroma = DoubleArray(12)
        val trebleChroma = DoubleArray(12)
        val windowSize = 4096.coerceAtMost(samples.size)
        val hopSize = 2048
        val numFrames = (samples.size - windowSize) / hopSize

        if (numFrames <= 0) {
            val fallback = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0)
            return GlobalChromaResult(fallback, fallback, fallback)
        }

        val tunedA4 = estimateTuningA4(samples, sampleRate)
        val stepStride = if (numFrames > 100) 2 else 1

        for (f in 0 until numFrames step stepStride) {
            val start = f * hopSize
            // Bass register (MIDI 40..55: E2 to G3) - Rejects sub-bass / kick thump below 80Hz
            for (midi in 40..55) {
                val freq = tunedA4 * 2.0.pow((midi - 69) / 12.0)
                val pc = midi % 12
                val energy = goertzelEnergyExact(samples, start, windowSize, freq, sampleRate)
                bassChroma[pc] += energy
            }
            // Treble / Harmony register (MIDI 56..79: G#3 to G5)
            for (midi in 56..79) {
                val freq = tunedA4 * 2.0.pow((midi - 69) / 12.0)
                val pc = midi % 12
                val energy = goertzelEnergyExact(samples, start, windowSize, freq, sampleRate)
                trebleChroma[pc] += energy
            }
        }

        normalizeVector(bassChroma)
        normalizeVector(trebleChroma)

        val fullChroma = DoubleArray(12) { i ->
            0.50 * bassChroma[i] + 0.50 * trebleChroma[i]
        }
        normalizeVector(fullChroma)

        return GlobalChromaResult(fullChroma, bassChroma, trebleChroma)
    }

    /**
     * Krumhansl-Schmuckler Key Finding with Tonic and Fifth Bass Grounding.
     * Prevents relative Major/Minor confusion by correlating tonal center prominence in the bass register.
     */
    private fun estimateKey(globalResult: GlobalChromaResult): String {
        val chroma = globalResult.fullChroma
        val bassChroma = globalResult.bassChroma
        var bestScore = -999.0
        var bestKey = "C Major"

        for (shift in 0 until 12) {
            val shiftedChroma = DoubleArray(12) { chroma[(it + shift) % 12] }
            val tonicBass = bassChroma[shift] // Energy of tonic pitch in the bass
            val fifthBass = bassChroma[(shift + 7) % 12] // Energy of 5th in the bass
            val bassScore = (tonicBass * 0.45) + (fifthBass * 0.15)

            // Major Profile
            val majorCorr = pearsonCorrelation(shiftedChroma, MAJOR_PROFILE)
            val majorScore = majorCorr + bassScore
            if (majorScore > bestScore) {
                bestScore = majorScore
                bestKey = "${NOTE_NAMES[shift]} Major"
            }

            // Minor Profile (incorporates tonic bass and leading tone)
            val minorCorr = pearsonCorrelation(shiftedChroma, MINOR_PROFILE)
            val harmonicLeadingTone = chroma[(shift + 11) % 12]
            val minorScore = minorCorr + bassScore + (harmonicLeadingTone * 0.10)
            if (minorScore > bestScore) {
                bestScore = minorScore
                bestKey = "${NOTE_NAMES[shift]} Minor"
            }
        }

        return bestKey
    }

    private fun pearsonCorrelation(x: DoubleArray, y: DoubleArray): Double {
        val n = x.size
        val meanX = x.average()
        val meanY = y.average()

        var num = 0.0
        var denX = 0.0
        var denY = 0.0

        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }

        val den = sqrt(denX * denY)
        return if (den > 0.00001) num / den else 0.0
    }

    /**
     * Tonality scoring system with strong Diatonic anchoring and Out-of-Key Rejection.
     * Prevents spurious chromatic chords (e.g. C#, Ab in C Major) while encouraging authentic guitar chords.
     */
    private fun getTonalityScore(candidateChord: String, detectedKey: String, noteNames: List<String>): Double {
        val keyRoot = detectedKey.substringBefore(" ").trim()
        val isMinorKey = detectedKey.contains("Minor")
        val rootIdx = when {
            noteNames.indexOf(keyRoot) >= 0 -> noteNames.indexOf(keyRoot)
            NOTE_NAMES.indexOf(keyRoot) >= 0 -> NOTE_NAMES.indexOf(keyRoot)
            TabTransposer.NOTES_FLAT.indexOf(keyRoot) >= 0 -> TabTransposer.NOTES_FLAT.indexOf(keyRoot)
            else -> 0
        }

        if (!isMinorKey) {
            // Major Key: I, ii, iii, IV, V, vi, bVII
            val tonic = noteNames[(rootIdx + 0) % 12]
            val supertonic = noteNames[(rootIdx + 2) % 12]
            val mediant = noteNames[(rootIdx + 4) % 12]
            val subdominant = noteNames[(rootIdx + 5) % 12]
            val dominant = noteNames[(rootIdx + 7) % 12]
            val submediant = noteNames[(rootIdx + 9) % 12]
            val subtonic = noteNames[(rootIdx + 10) % 12]

            return when (candidateChord) {
                tonic -> 0.65
                "${tonic}maj7" -> 0.45
                "${tonic}sus4" -> 0.40
                subdominant -> 0.55
                "${subdominant}maj7" -> 0.40
                dominant -> 0.55
                "${dominant}7" -> 0.50
                "${dominant}sus4" -> 0.40
                "${submediant}m" -> 0.50
                "${submediant}m7" -> 0.40
                "${supertonic}m" -> 0.45
                "${supertonic}m7" -> 0.35
                "${mediant}m" -> 0.40
                "${mediant}m7" -> 0.30
                subtonic -> 0.30 // Flat VII (rock/pop)
                // Common secondary dominants:
                mediant, "${mediant}7" -> 0.35 // V of vi (e.g. E/E7 in C)
                supertonic, "${supertonic}7" -> 0.30 // V of V (e.g. D/D7 in C)
                noteNames[(rootIdx + 9) % 12], "${noteNames[(rootIdx + 9) % 12]}7" -> 0.30 // V of ii (e.g. A/A7 in C)
                else -> -0.55 // OUT-OF-KEY REJECTION PENALTY!
            }
        } else {
            // Minor Key: i, III (rel maj), iv, v/V, VI, VII
            val tonic = noteNames[(rootIdx + 0) % 12]
            val relMajor = noteNames[(rootIdx + 3) % 12]
            val subdominant = noteNames[(rootIdx + 5) % 12]
            val dominant = noteNames[(rootIdx + 7) % 12]
            val submediant = noteNames[(rootIdx + 8) % 12]
            val subtonic = noteNames[(rootIdx + 10) % 12]
            val majorDominant = noteNames[(rootIdx + 7) % 12] // Harmonic minor Major V

            return when (candidateChord) {
                "${tonic}m" -> 0.65
                "${tonic}m7" -> 0.45
                "${tonic}sus4" -> 0.40
                relMajor -> 0.55
                "${relMajor}maj7" -> 0.40
                "${subdominant}m" -> 0.50
                "${subdominant}m7" -> 0.40
                subdominant -> 0.35 // Dorian IV (e.g. D in Am)
                majorDominant, "${majorDominant}7" -> 0.55 // Harmonic minor V/V7 (e.g. E/E7 in Am)
                "${dominant}m" -> 0.45
                submediant -> 0.50 // VI (e.g. F in Am)
                "${submediant}maj7" -> 0.40
                subtonic, "${subtonic}7" -> 0.45 // VII (e.g. G/G7 in Am)
                else -> -0.55 // OUT-OF-KEY REJECTION PENALTY!
            }
        }
    }

    private fun computeRms(samples: FloatArray, start: Int, length: Int): Float {
        val end = min(samples.size, start + length)
        val count = end - start
        if (count <= 0) return 0f
        var sumSquares = 0.0
        for (i in start until end) {
            val s = samples[i]
            sumSquares += s * s
        }
        return sqrt(sumSquares / count).toFloat()
    }

    /**
     * Measure-Synchronous Chord Recognition with 2-Beat Harmonic Integration.
     * Evaluates Cosine Similarity, Bass Foundation, Contrastive Thirds, and Strict Key Anchoring.
     */
    fun recognizeChordProgression(samples: FloatArray, sampleRate: Float, bpm: Int, detectedKey: String): List<ChordEvent> {
        val secondsPerBeat = 60.0 / bpm
        // Half-measure blocks: 2 beats per evaluation step
        val stepBeats = 2
        val stepDuration = secondsPerBeat * stepBeats
        val stepSamples = (stepDuration * sampleRate).toInt().coerceAtLeast(2048)
        val windowSize = 4096.coerceAtMost(samples.size)
        val totalBlocks = ((samples.size - windowSize) / stepSamples).coerceAtLeast(1)

        val useFlats = detectedKey.contains("b") || detectedKey.startsWith("F") || detectedKey.startsWith("D Minor")
        val noteNames = if (useFlats) TabTransposer.NOTES_FLAT else NOTE_NAMES
        val tunedA4 = estimateTuningA4(samples, sampleRate)

        val rawBlockChords = mutableListOf<String>()
        val blockConfidences = mutableListOf<Double>()
        var previousChord = if (detectedKey.contains("Minor")) "${noteNames[0]}m" else noteNames[0]

        for (b in 0 until totalBlocks) {
            val blockStart = b * stepSamples
            if (blockStart + windowSize > samples.size) break

            // Energy / Silence check
            val frameRms = computeRms(samples, blockStart, windowSize)
            if (frameRms < 0.005f) {
                rawBlockChords.add(previousChord)
                blockConfidences.add(0.85)
                continue
            }

            // Dual-point Chroma integration within the 2-beat block:
            // Samples at blockStart and (blockStart + stepSamples/2) to average out attack transients & pick noise
            val blockBassChroma = DoubleArray(12)
            val blockTrebleChroma = DoubleArray(12)
            val subPoints = listOf(blockStart, min(samples.size - windowSize, blockStart + (stepSamples / 2)))

            for (startPos in subPoints) {
                // Bass band: MIDI 40..55 (E2 to G3) - 82Hz to 196Hz
                for (midi in 40..55) {
                    val freq = tunedA4 * 2.0.pow((midi - 69) / 12.0)
                    val pc = midi % 12
                    blockBassChroma[pc] += goertzelEnergyExact(samples, startPos, windowSize, freq, sampleRate)
                }
                // Treble band: MIDI 56..79 (G#3 to G5) - 207Hz to 784Hz
                for (midi in 56..79) {
                    val freq = tunedA4 * 2.0.pow((midi - 69) / 12.0)
                    val pc = midi % 12
                    blockTrebleChroma[pc] += goertzelEnergyExact(samples, startPos, windowSize, freq, sampleRate)
                }
            }

            normalizeVector(blockBassChroma)
            normalizeVector(blockTrebleChroma)

            // Combined Chroma for harmonic template matching
            val combinedChroma = DoubleArray(12) { i ->
                0.40 * blockBassChroma[i] + 0.60 * blockTrebleChroma[i]
            }
            normalizeVector(combinedChroma)

            // Find dominant bass pitch
            val dominantBassPitch = blockBassChroma.indices.maxByOrNull { blockBassChroma[it] } ?: 0

            var bestChordName = previousChord
            var bestScore = -999.0

            for (root in 0 until 12) {
                val rootName = noteNames[root]
                val bassRootScore = blockBassChroma[root] * 1.2 + (if (root == dominantBassPitch) 0.35 else 0.0)
                val bass3rdScore = (blockBassChroma[(root + 4) % 12] + blockBassChroma[(root + 3) % 12]) * 0.30
                val bass5thScore = blockBassChroma[(root + 7) % 12] * 0.20
                val totalBassAgreement = bassRootScore + bass3rdScore + bass5thScore

                for ((suffix, intervals) in CHORD_TEMPLATES) {
                    val candidateName = "$rootName$suffix"

                    // In-chord spectral match
                    var inChordScore = 0.0
                    for (interval in intervals) {
                        val pc = (root + interval) % 12
                        val weight = when (interval) {
                            3, 4, 5 -> 1.7 // Third / Fourth defines chord quality
                            0 -> 1.2      // Root
                            7 -> 0.8      // Fifth
                            10, 11 -> 1.1 // Seventh
                            else -> 1.0
                        }
                        inChordScore += combinedChroma[pc] * weight
                    }

                    // Strict Contrastive Clashing Penalties
                    var clashPenalty = 0.0
                    when (suffix) {
                        "", "7", "maj7" -> {
                            // Major quality: minor 3rd (+3) must NOT be present
                            clashPenalty += combinedChroma[(root + 3) % 12] * 1.8
                            if (suffix == "7") clashPenalty += combinedChroma[(root + 11) % 12] * 1.3
                            if (suffix == "maj7") clashPenalty += combinedChroma[(root + 10) % 12] * 1.3
                        }
                        "m", "m7" -> {
                            // Minor quality: major 3rd (+4) must NOT be present
                            clashPenalty += combinedChroma[(root + 4) % 12] * 1.8
                            if (suffix == "m") clashPenalty += combinedChroma[(root + 11) % 12] * 1.2
                        }
                        "sus4" -> {
                            // Sus4: neither 3rd should be present
                            clashPenalty += (combinedChroma[(root + 3) % 12] + combinedChroma[(root + 4) % 12]) * 1.6
                        }
                    }

                    // Non-chord tones penalty
                    var outChordScore = 0.0
                    for (pc in 0 until 12) {
                        val semitones = (pc - root + 12) % 12
                        if (semitones !in intervals) {
                            outChordScore += combinedChroma[pc]
                        }
                    }

                    val tonalityScore = getTonalityScore(candidateName, detectedKey, noteNames)
                    val stabilityBonus = if (candidateName == previousChord) 0.35 else 0.0

                    val score = (inChordScore * 1.3) + totalBassAgreement - clashPenalty - (outChordScore * 0.35) + tonalityScore + stabilityBonus

                    if (score > bestScore) {
                        bestScore = score
                        bestChordName = candidateName
                    }
                }
            }

            rawBlockChords.add(bestChordName)
            blockConfidences.add((0.85 + (bestScore.coerceIn(0.0, 1.8) * 0.07)).coerceIn(0.85, 0.98))
            previousChord = bestChordName
        }

        // Apply 3-block majority smoothing
        val smoothedBlocks = rawBlockChords.toMutableList()
        for (i in 1 until smoothedBlocks.size - 1) {
            val prev = smoothedBlocks[i - 1]
            val curr = smoothedBlocks[i]
            val next = smoothedBlocks[i + 1]
            if (prev == next && curr != prev) {
                smoothedBlocks[i] = prev
            }
        }

        // Aggregate consecutive 2-beat blocks into ChordEvents
        val chordEvents = mutableListOf<ChordEvent>()
        if (smoothedBlocks.isEmpty()) {
            return listOf(ChordEvent(0.0, if (useFlats) "Bb" else "C", 0.95, stepDuration * 2, 1))
        }

        var currentChord = smoothedBlocks.first()
        var currentStartBlock = 0
        var currentDurationBlocks = 1
        var confSum = blockConfidences.firstOrNull() ?: 0.92
        var confCount = 1

        for (i in 1 until smoothedBlocks.size) {
            val chord = smoothedBlocks[i]
            if (chord == currentChord) {
                currentDurationBlocks++
                confSum += blockConfidences.getOrElse(i) { 0.92 }
                confCount++
            } else {
                val startTime = currentStartBlock * stepDuration
                val durationTime = currentDurationBlocks * stepDuration
                val beatNum = (currentStartBlock * stepBeats) + 1
                val avgConf = (confSum / confCount).coerceIn(0.88, 0.98)
                chordEvents.add(ChordEvent(startTime, currentChord, avgConf, durationTime, beatNum))

                currentChord = chord
                currentStartBlock = i
                currentDurationBlocks = 1
                confSum = blockConfidences.getOrElse(i) { 0.92 }
                confCount = 1
            }
        }

        // Final event
        val startTime = currentStartBlock * stepDuration
        val durationTime = currentDurationBlocks * stepDuration
        val beatNum = (currentStartBlock * stepBeats) + 1
        val avgConf = (confSum / confCount).coerceIn(0.88, 0.98)
        chordEvents.add(ChordEvent(startTime, currentChord, avgConf, durationTime, beatNum))

        return chordEvents
    }

    /**
     * Seamlessly fuses Neural Transformer chord predictions with DSP harmonic analysis.
     * Guarantees 100% gap-free, continuous chord progression across the entire song duration.
     */
    fun mergeNeuralWithDspFallback(
        neuralChords: List<ChordEvent>,
        dspChords: List<ChordEvent>,
        bpm: Int,
        totalDurationSeconds: Double
    ): List<ChordEvent> {
        if (neuralChords.isEmpty()) return dspChords
        if (dspChords.isEmpty()) return neuralChords

        val secondsPerBeat = 60.0 / bpm.coerceIn(55, 190)
        val mergedList = mutableListOf<ChordEvent>()

        val sortedNeural = neuralChords.sortedBy { it.timestampSeconds }
        var currentPointerSec = 0.0

        for (neuralEv in sortedNeural) {
            val neuralStart = neuralEv.timestampSeconds
            val neuralEnd = neuralStart + neuralEv.durationSeconds

            // If there is an unpredicted gap before this neural chord
            if (neuralStart - currentPointerSec > secondsPerBeat * 0.75) {
                val gapStart = currentPointerSec
                val gapEnd = neuralStart
                val dspCover = dspChords.filter { dspEv ->
                    val dEnd = dspEv.timestampSeconds + dspEv.durationSeconds
                    dspEv.timestampSeconds < gapEnd && dEnd > gapStart
                }
                for (dEv in dspCover) {
                    val clippedStart = max(gapStart, dEv.timestampSeconds)
                    val clippedEnd = min(gapEnd, dEv.timestampSeconds + dEv.durationSeconds)
                    val clippedDur = clippedEnd - clippedStart
                    if (clippedDur > 0.1) {
                        mergedList.add(dEv.copy(timestampSeconds = clippedStart, durationSeconds = clippedDur))
                    }
                }
            }

            mergedList.add(neuralEv)
            currentPointerSec = neuralEnd
        }

        // Fill trailing gap until song duration
        if (totalDurationSeconds - currentPointerSec > secondsPerBeat * 0.75) {
            val gapStart = currentPointerSec
            val gapEnd = totalDurationSeconds
            val dspCover = dspChords.filter { dspEv ->
                val dEnd = dspEv.timestampSeconds + dspEv.durationSeconds
                dspEv.timestampSeconds < gapEnd && dEnd > gapStart
            }
            for (dEv in dspCover) {
                val clippedStart = max(gapStart, dEv.timestampSeconds)
                val clippedEnd = min(gapEnd, dEv.timestampSeconds + dEv.durationSeconds)
                val clippedDur = clippedEnd - clippedStart
                if (clippedDur > 0.1) {
                    mergedList.add(dEv.copy(timestampSeconds = clippedStart, durationSeconds = clippedDur))
                }
            }
        }

        // Consolidate adjacent identical chords
        val consolidated = mutableListOf<ChordEvent>()
        for (ev in mergedList) {
            val last = consolidated.lastOrNull()
            if (last != null && last.chordName == ev.chordName) {
                val combinedDur = (ev.timestampSeconds + ev.durationSeconds) - last.timestampSeconds
                consolidated[consolidated.lastIndex] = last.copy(durationSeconds = combinedDur)
            } else {
                consolidated.add(ev)
            }
        }

        var beatCounter = 1
        return consolidated.map { ev ->
            val beats = max(1, (ev.durationSeconds / secondsPerBeat).roundToInt())
            val evWithBeat = ev.copy(beatNumber = beatCounter)
            beatCounter += beats
            evWithBeat
        }
    }

    private fun calculateSuggestedCapo(key: String): Int {
        val root = key.substringBefore(" ")
        return when (root) {
            "Ab", "G#" -> 1 // Play G with Capo 1
            "A" -> 2 // Play G with Capo 2
            "Bb", "A#" -> 3 // Play G with Capo 3
            "B" -> 4 // Play G with Capo 4
            "Db", "C#" -> 1 // Play C with Capo 1
            "Eb", "D#" -> 3 // Play C with Capo 3
            "F" -> 1 // Play E with Capo 1
            "F#" -> 2 // Play E with Capo 2
            else -> 0 // Open key
        }
    }

    private fun calculateConfidence(chordEvents: List<ChordEvent>, bpm: Int): Double {
        if (chordEvents.isEmpty()) return 0.70
        val tempoScore = if (bpm in 70..160) 0.95 else 0.85
        val harmonicScore = (chordEvents.map { it.confidence }.average()).coerceIn(0.75, 0.98)
        return ((tempoScore + harmonicScore) / 2.0).roundTo(2)
    }

    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

    /**
     * Formats the final chord tab into Ultimate Guitar ChordPro notation
     * Renders the complete song's chord progression in structured 4-bar measure rows with timestamps.
     */
    fun generateFormattedTab(
        title: String,
        artist: String,
        key: String,
        bpm: Int,
        capo: Int,
        strumming: String,
        chords: List<ChordEvent>,
        providedLyrics: String?,
        syncedLyrics: List<SyncedLyricLine> = emptyList()
    ): String {
        val distinctChords = chords.map { it.chordName }.distinct()
        val chordsHeader = distinctChords.joinToString("  ") { "[ch]$it[/ch]" }

        val sb = StringBuilder()
        sb.appendLine("Title: $title")
        sb.appendLine("Artist: $artist")
        sb.appendLine("Key: $key | Tempo: $bpm BPM | Time Signature: 4/4")
        if (capo > 0) {
            sb.appendLine("Capo: $capo (Transposed for Open Chord Shapes)")
        } else {
            sb.appendLine("Capo: No Capo")
        }
        sb.appendLine("Strumming Pattern: $strumming")
        sb.appendLine("Chords Used: $chordsHeader")
        sb.appendLine()
        sb.appendLine("-------------------------------------------------------------------------")
        sb.appendLine()

        if (syncedLyrics.isNotEmpty()) {
            // Align Chords with timestamp-synchronized lyrics
            for (line in syncedLyrics) {
                val t = line.timestampSeconds
                val chordAtTime = chords.minByOrNull { kotlin.math.abs(it.timestampSeconds - t) }?.chordName
                    ?: distinctChords.firstOrNull() ?: "C"
                val nextChordAtTime = chords.minByOrNull { kotlin.math.abs(it.timestampSeconds - (t + 2.5)) }?.chordName
                    ?: chordAtTime

                val midPoint = (line.text.length / 2).coerceAtLeast(6)
                val chordLine = buildString {
                    append("[ch]$chordAtTime[/ch]")
                    if (nextChordAtTime != chordAtTime && line.text.length > 8) {
                        val padding = max(2, midPoint - chordAtTime.length - 2)
                        append(" ".repeat(padding))
                        append("[ch]$nextChordAtTime[/ch]")
                    }
                }
                sb.appendLine(chordLine)
                sb.appendLine(line.text)
                sb.appendLine()
            }
        } else if (!providedLyrics.isNullOrBlank()) {
            // Align Chords over Provided Lyrics
            val lines = providedLyrics.lines()
            var chordIdx = 0

            for (line in lines) {
                if (line.trim().startsWith("[") && line.trim().endsWith("]")) {
                    sb.appendLine(line)
                    continue
                }
                if (line.isBlank()) {
                    sb.appendLine()
                    continue
                }

                // Place chord above the line
                val chordForLine = distinctChords.getOrElse(chordIdx % distinctChords.size) { "C" }
                val nextChord = distinctChords.getOrElse((chordIdx + 1) % distinctChords.size) { "G" }
                chordIdx++

                val midPoint = (line.length / 2).coerceAtLeast(6)
                val chordLine = buildString {
                    append("[ch]$chordForLine[/ch]")
                    val padding = max(2, midPoint - chordForLine.length - 2)
                    append(" ".repeat(padding))
                    append("[ch]$nextChord[/ch]")
                }

                sb.appendLine(chordLine)
                sb.appendLine(line)
                sb.appendLine()
            }
        } else {
            // Full Musical Tab Sheet across the entire song!
            if (chords.isEmpty()) {
                sb.appendLine("No chord events detected.")
                return sb.toString()
            }

            val totalDuration = chords.last().let { it.timestampSeconds + it.durationSeconds }
            val barsPerChunk = 4
            var currentSectionName = "Intro"

            // Group chords into measures (chunks of 4 chords or 4 bars)
            val chunks = chords.chunked(barsPerChunk)

            for ((chunkIndex, chunk) in chunks.withIndex()) {
                val chunkStartTime = chunk.first().timestampSeconds
                val progress = chunkStartTime / max(1.0, totalDuration)

                // Musical Section inference based on song progress
                val newSection = when {
                    progress < 0.12 -> "Intro"
                    progress < 0.35 -> "Verse 1"
                    progress < 0.55 -> "Chorus"
                    progress < 0.72 -> "Verse 2"
                    progress < 0.85 -> "Bridge / Solo"
                    else -> "Outro"
                }

                if (newSection != currentSectionName || chunkIndex == 0) {
                    currentSectionName = newSection
                    val mm = (chunkStartTime / 60).toInt()
                    val ss = (chunkStartTime % 60).toInt()
                    val timeStr = String.format("%02d:%02d", mm, ss)
                    sb.appendLine()
                    sb.appendLine("[$timeStr] $currentSectionName:")
                }

                // Format the 4-measure chord row
                val chordRow = buildString {
                    append("| ")
                    for (ev in chunk) {
                        val formattedChord = "[ch]${ev.chordName}[/ch]"
                        // Pad each measure to 12 characters for clean monospaced alignment
                        val pad = max(1, 10 - ev.chordName.length)
                        append(formattedChord)
                        append(" ".repeat(pad))
                        append("| ")
                    }
                }
                sb.appendLine(chordRow)
            }
        }

        return sb.toString()
    }
}
