package com.gbros.tabslite.utilities

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class OnnxChordModelTest {

    @Test
    fun testModelInitialization() {
        println("Status: ${OnnxChordModel.getInitializationStatus()}")
        assertTrue(OnnxChordModel.isAvailable(), "OnnxChordModel should be initialized and ready")
    }

    @Test
    fun testChordFormatting() {
        assertEquals("C", OnnxChordModel.formatChordName("C"))
        assertEquals("Cm", OnnxChordModel.formatChordName("C:min"))
        assertEquals("G7", OnnxChordModel.formatChordName("G:7"))
        assertEquals("Fmaj7", OnnxChordModel.formatChordName("F:maj7"))
        assertEquals("Am7", OnnxChordModel.formatChordName("A:min7"))
        assertEquals("Dm", OnnxChordModel.formatChordName("D:min"))
        assertEquals("", OnnxChordModel.formatChordName("N"))
        assertEquals("", OnnxChordModel.formatChordName(""))
    }

    @Test
    fun testSilenceGatingProducesNoChords() = runBlocking {
        assertTrue(OnnxChordModel.isAvailable())

        // 3 seconds of near-silent background audio
        val silentAudio = FloatArray(3 * 22050) { ((Math.random() - 0.5) * 0.002).toFloat() }
        val cqtAnalysis = OnnxChordModel.computeLogCqtFeatures(silentAudio)
        val predictions = OnnxChordModel.predictChordSequence(cqtAnalysis)

        // All frames in silent audio must be marked silent
        assertTrue(predictions.all { it.chordName.isEmpty() }, "Silent audio should produce empty chord names")

        val timeline = OnnxChordModel.alignToBeatTimeline(predictions, 120, 3.0)
        assertTrue(timeline.isEmpty(), "Silent audio timeline should not produce phantom chord events")
    }

    @Test
    fun testPrecisionBpmEstimation() {
        // Test BPM estimation across demo tracks
        val popDemo = AudioAnalysisEngine.DEMO_TRACKS.first { it.id == "demo_pop" }
        val samples = AudioAnalysisEngine.generateDemoAudioSamples(popDemo, durationSeconds = 8.0, sampleRate = 16000f)
        val detectedBpm = AudioAnalysisEngine.estimateTempoBpm(samples, 16000f)

        println("Expected BPM: ${popDemo.expectedBpm}, Detected BPM: $detectedBpm")
        // BPM should be close to expected (within 10%)
        assertTrue(kotlin.math.abs(detectedBpm - popDemo.expectedBpm) <= 12, "BPM should be accurately estimated without octave errors")
    }

    @Test
    fun testEndToEndInferenceAndPadStabilization() = runBlocking {
        assertTrue(OnnxChordModel.isAvailable(), "OnnxChordModel should be available")

        val demo = AudioAnalysisEngine.DEMO_TRACKS.first { it.id == "demo_pop" }
        val samples = AudioAnalysisEngine.generateDemoAudioSamples(demo, durationSeconds = 8.0, sampleRate = 16000f)

        val result = AudioAnalysisEngine.analyzeAudio(samples, 16000f, demo.name, demo.artist)
        assertTrue(result.chordTimeline.isNotEmpty(), "Aligned chord timeline should not be empty")

        // 8 seconds with 4 chords should produce around 4 major chord spans, NOT dozens of fragmented chords!
        println("Recognized timeline chords: ${result.chordTimeline.map { "${it.chordName} (${it.durationSeconds}s)" }}")
        assertTrue(result.chordTimeline.size <= 8, "Held chords should be stabilized and merged, not fragmented into dozens of micro-chords")
    }
}
