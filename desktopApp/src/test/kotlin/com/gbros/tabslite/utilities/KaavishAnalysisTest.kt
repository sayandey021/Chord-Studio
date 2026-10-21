package com.gbros.tabslite.utilities

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class KaavishAnalysisTest {

    @Test
    fun testKaavishSongAnalysis() = runBlocking {
        val file = File("""C:\Users\sayan\Downloads\Kaavish Tairay Naam.mp3""")
        if (!file.exists()) {
            println("File not found, skipping.")
            return@runBlocking
        }

        println("Loading ${file.name}...")
        val samples = AudioAnalysisEngine.loadAudioFile(file, 16000f)
        val duration = samples.size / 16000.0
        println("Loaded audio: duration=${duration}s, samples=${samples.size}")

        // Test BPM estimation
        val detectedBpm = AudioAnalysisEngine.estimateTempoBpm(samples, 16000f)
        println("Detected BPM: $detectedBpm (Chord AI detected 76 BPM)")

        val result = AudioAnalysisEngine.analyzeAudio(
            samples = samples,
            sampleRate = 16000f,
            songTitle = "Tairay Naam",
            artistName = "Kaavish"
        )
        println("Detected Key: ${result.detectedKey}")
        println("Detected BPM: ${result.detectedBpm}")
        println("Total Chords: ${result.chordTimeline.size}")
        println("First 20 Chord Events:")
        result.chordTimeline.take(20).forEach { ev ->
            println("  [${"%.2f".format(ev.timestampSeconds)}s - ${"%.2f".format(ev.timestampSeconds + ev.durationSeconds)}s] ${ev.chordName} (dur=${"%.2f".format(ev.durationSeconds)}s, beat=${ev.beatNumber})")
        }
    }

    @Test
    fun testLyricsExtraction() = runBlocking {
        val file = File("""C:\Users\sayan\Downloads\Kaavish Tairay Naam.mp3""")
        if (!file.exists()) {
            println("File not found, skipping lyrics test.")
            return@runBlocking
        }
        val lyricsRes = LyricsExtractor.extractLyrics(file)
        println("Extracted lyrics result: source=${lyricsRes?.source}, title=${lyricsRes?.detectedTitle}, artist=${lyricsRes?.detectedArtist}")
        println("Synced lines: ${lyricsRes?.syncedLyrics?.size}")
        println("Plain lyrics preview:\n${lyricsRes?.plainLyrics?.take(200)}...")
        kotlin.test.assertNotNull(lyricsRes)
        kotlin.test.assertTrue(lyricsRes.plainLyrics.isNotBlank())
    }

    @Test
    fun testWhisperCppTranscription() = runBlocking {
        val file = File("""C:\Users\sayan\Downloads\Kaavish Tairay Naam.mp3""")
        if (!file.exists()) {
            println("File not found, skipping whisper.cpp test.")
            return@runBlocking
        }
        val cli = LyricsExtractor.findWhisperCliExecutable()
        println("Found whisper-cli: $cli")
        kotlin.test.assertNotNull(cli, "whisper-cli.exe must be found")

        val model = LyricsExtractor.findWhisperModelFile("base", fallbackToInstalled = true)
        println("Found model: $model")
        kotlin.test.assertNotNull(model, "Whisper model file must be found")

        val samples = AudioAnalysisEngine.loadAudioFile(file, 16000f)
        val vocalSamples = samples.copyOfRange((20 * 16000).coerceAtMost(samples.size), (50 * 16000).coerceAtMost(samples.size))
        val res = LyricsExtractor.transcribeWithWhisperCpp(
            audioSamples = vocalSamples,
            modelName = "base",
            language = "auto",
            onProgress = { println("Progress: $it") }
        )
        println("Whisper.cpp Result: source=${res?.source}, lines=${res?.syncedLyrics?.size}")
        res?.syncedLyrics?.forEach { line ->
            println("  [${line.timestampSeconds}s] ${line.text}")
        }
        kotlin.test.assertNotNull(res)
        kotlin.test.assertTrue(res.syncedLyrics.isNotEmpty())
    }

    @Test
    fun testCleanAndDeduplicateLyrics() {
        val repetitiveHallucination = buildString {
            append("[SUSH BROKE]\n")
            repeat(50) {
                append("<i>Judhao kemi</i>\n")
            }
            append("[MUSIC]\n")
            append("Juda hoke bhi tu mujhme kahi baaki hai\n")
            append("Juda hoke bhi tu mujhme kahi baaki hai\n")
            append("Palkon pe ban ke aansu tu chali aati hai\n")
        }

        val cleaned = LyricsExtractor.cleanAndDeduplicateLyrics(repetitiveHallucination)
        println("Cleaned Lyrics:\n$cleaned")

        kotlin.test.assertFalse(cleaned.contains("[SUSH BROKE]"), "Sound effect brackets must be stripped")
        kotlin.test.assertFalse(cleaned.contains("<i>"), "HTML tags must be stripped")
        kotlin.test.assertFalse(cleaned.contains("</i>"), "HTML tags must be stripped")
        kotlin.test.assertFalse(cleaned.contains("[MUSIC]"), "Non-speech brackets must be stripped")

        val occurrences = cleaned.lines().count { it.contains("Judhao kemi") }
        kotlin.test.assertTrue(occurrences <= 2, "Hallucinated repetition must be capped to at most 2, found $occurrences")
        kotlin.test.assertTrue(cleaned.contains("Juda hoke bhi tu mujhme kahi baaki hai"))
    }
}
