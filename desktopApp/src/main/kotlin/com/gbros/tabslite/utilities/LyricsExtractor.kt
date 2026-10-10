package com.gbros.tabslite.utilities

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import com.google.gson.JsonArray

data class SyncedLyricLine(
    val timestampSeconds: Double,
    val text: String
)

data class ExtractedLyricsResult(
    val plainLyrics: String,
    val syncedLyrics: List<SyncedLyricLine> = emptyList(),
    val source: String = "online", // "embedded", "sidecar", "online", "manual"
    val detectedTitle: String? = null,
    val detectedArtist: String? = null
) {
    fun toLrcOrPlainText(): String {
        if (syncedLyrics.isNotEmpty()) {
            return syncedLyrics.joinToString("\n") { line ->
                val totalSec = line.timestampSeconds
                val mm = (totalSec / 60).toInt()
                val ss = totalSec % 60
                "[%02d:%05.2f] %s".format(java.util.Locale.US, mm, ss, line.text)
            }
        }
        return plainLyrics
    }
}

data class AudioFileMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val embeddedLyrics: String? = null
)

object LyricsExtractor {

    private const val LRCLIB_BASE_URL = "https://lrclib.net/api"
    private const val USER_AGENT = "TabsLite-PC/1.0 (https://github.com/gbros/Tabs-Lite)"

    /**
     * Complete pipeline to extract lyrics from:
     * 1. Embedded ID3 tags in the audio file (USLT / SYLT)
     * 2. Sidecar .lrc or .txt files in the same folder
     * 3. Online LRCLIB database using ID3 metadata or filename
     */
    suspend fun extractLyrics(
        file: File?,
        durationSeconds: Double? = null,
        fallbackTitle: String? = null,
        fallbackArtist: String? = null
    ): ExtractedLyricsResult? = withContext(Dispatchers.IO) {
        if (file == null && fallbackTitle.isNullOrBlank()) return@withContext null

        var id3Metadata: AudioFileMetadata? = null
        if (file != null && file.exists()) {
            try {
                id3Metadata = readAudioMetadata(file)
            } catch (e: Exception) {
                System.err.println("Error reading ID3 tags: ${e.message}")
            }

            // 1. Check embedded lyrics inside ID3 tags
            val embedded = id3Metadata?.embeddedLyrics
            if (!embedded.isNullOrBlank()) {
                val synced = parseLrc(embedded)
                val plain = if (synced.isNotEmpty()) synced.joinToString("\n") { it.text } else embedded
                return@withContext ExtractedLyricsResult(
                    plainLyrics = plain.trim(),
                    syncedLyrics = synced,
                    source = "embedded",
                    detectedTitle = id3Metadata.title,
                    detectedArtist = id3Metadata.artist
                )
            }

            // 2. Check local sidecar files (e.g. song.lrc or song.txt in same directory)
            val sidecarLyrics = findSidecarLyrics(file)
            if (sidecarLyrics != null) {
                return@withContext sidecarLyrics
            }
        }

        // 3. Resolve best Title and Artist for online lookup
        val resolvedTitle: String
        val resolvedArtist: String

        val id3Title = id3Metadata?.title?.trim()
        val id3Artist = id3Metadata?.artist?.trim()

        if (!id3Title.isNullOrBlank()) {
            if (id3Title.contains(" - ")) {
                val parts = id3Title.split(" - ", limit = 2)
                resolvedArtist = parts[0].trim()
                resolvedTitle = cleanTitle(parts[1])
            } else {
                resolvedTitle = cleanTitle(id3Title)
                resolvedArtist = id3Artist?.takeIf { it.isNotBlank() } ?: (fallbackArtist ?: "")
            }
        } else if (file != null) {
            val (fileArtist, fileTitle) = parseArtistAndTitleFromFilename(file.nameWithoutExtension)
            resolvedTitle = cleanTitle(fileTitle)
            resolvedArtist = if (fileArtist.isNotBlank()) fileArtist else (fallbackArtist ?: "")
        } else {
            resolvedTitle = cleanTitle(fallbackTitle ?: "")
            resolvedArtist = fallbackArtist ?: ""
        }

        if (resolvedTitle.isBlank()) return@withContext null

        // 4. Fetch online lyrics via LRCLIB
        try {
            val onlineResult = fetchOnlineLyrics(
                title = resolvedTitle,
                artist = resolvedArtist,
                durationSeconds = durationSeconds
            )
            if (onlineResult != null) {
                return@withContext onlineResult
            }
        } catch (e: Exception) {
            System.err.println("Failed to fetch online lyrics: ${e.message}")
        }

        null
    }

    /**
     * Returns a list of potential matching lyrics from LRCLIB for the given title and artist.
     */
    fun searchOnlineLyricsList(
        title: String,
        artist: String? = null
    ): List<ExtractedLyricsResult> {
        val cleanT = cleanTitle(title)
        val cleanA = artist?.let { cleanTitle(it) }?.takeIf { it.isNotBlank() && it != "Local Audio File" && it != "Detected Audio" }
        if (cleanT.isBlank()) return emptyList()

        val results = mutableListOf<ExtractedLyricsResult>()
        val seenKeys = mutableSetOf<String>()

        fun addAllUnique(list: List<ExtractedLyricsResult>) {
            for (item in list) {
                val key = "${item.detectedTitle?.lowercase()?.trim()} - ${item.detectedArtist?.lowercase()?.trim()} - ${item.syncedLyrics.isNotEmpty()}"
                if (seenKeys.add(key)) {
                    results.add(item)
                }
            }
        }

        // 1. Exact get query if artist is known
        if (!cleanA.isNullOrBlank()) {
            val getUrl = buildString {
                append("$LRCLIB_BASE_URL/get?")
                append("track_name=").append(URLEncoder.encode(cleanT, "UTF-8"))
                append("&artist_name=").append(URLEncoder.encode(cleanA, "UTF-8"))
            }
            val res = httpGet(getUrl)
            if (res != null) {
                val parsed = parseLrclibJson(res)
                if (parsed != null) addAllUnique(listOf(parsed))
            }
        }

        // 2. Search endpoint with artist + title
        if (!cleanA.isNullOrBlank()) {
            val q = URLEncoder.encode("$cleanA $cleanT", "UTF-8")
            val res = httpGet("$LRCLIB_BASE_URL/search?q=$q")
            if (res != null) {
                addAllUnique(parseLrclibSearchJson(res))
            }
        }

        // 3. Search endpoint with title only
        val qTitle = URLEncoder.encode(cleanT, "UTF-8")
        val resTitle = httpGet("$LRCLIB_BASE_URL/search?q=$qTitle")
        if (resTitle != null) {
            addAllUnique(parseLrclibSearchJson(resTitle))
        }

        // 4. If title has multiple words (e.g. "Kaavish Tairay Naam"), search by substrings
        if (results.isEmpty() && cleanT.contains(" ")) {
            val words = cleanT.split(Regex("\\s+"))
            if (words.size >= 2) {
                val lastWord = words.last()
                val subRes = httpGet("$LRCLIB_BASE_URL/search?q=" + URLEncoder.encode(lastWord, "UTF-8"))
                if (subRes != null) {
                    addAllUnique(parseLrclibSearchJson(subRes))
                }
            }
        }

        return results
    }

    /**
     * Searches LRCLIB for lyrics using get or search endpoints.
     */
    fun fetchOnlineLyrics(
        title: String,
        artist: String? = null,
        durationSeconds: Double? = null
    ): ExtractedLyricsResult? {
        val list = searchOnlineLyricsList(title, artist)
        if (list.isEmpty()) return null

        return list.firstOrNull { it.syncedLyrics.isNotEmpty() } ?: list.first()
    }

    /**
     * Parses synchronized LRC text (e.g. [01:23.45] lyric text) into SyncedLyricLine list.
     */
    fun parseLrc(lrcContent: String): List<SyncedLyricLine> {
        val result = mutableListOf<SyncedLyricLine>()
        val lrcPattern = Pattern.compile("\\[(\\d{1,3}):(\\d{2}(?:[\\.,]\\d{1,3})?)\\](.*)")

        for (line in lrcContent.lines()) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") || trimmed.startsWith("[al:") || trimmed.startsWith("[by:") || trimmed.startsWith("[offset:")) {
                continue
            }
            val matcher = lrcPattern.matcher(trimmed)
            if (matcher.matches()) {
                val minutes = matcher.group(1).toDoubleOrNull() ?: 0.0
                val secStr = matcher.group(2).replace(',', '.')
                val seconds = secStr.toDoubleOrNull() ?: 0.0
                val text = matcher.group(3).trim()
                if (text.isNotBlank()) {
                    val timestamp = minutes * 60.0 + seconds
                    result.add(SyncedLyricLine(timestamp, text))
                }
            }
        }

        return result.sortedBy { it.timestampSeconds }
    }

    /**
     * Looks for sidecar .lrc or .txt in the directory of the audio file.
     */
    private fun findSidecarLyrics(audioFile: File): ExtractedLyricsResult? {
        val parent = audioFile.parentFile ?: return null
        val baseName = audioFile.nameWithoutExtension

        val lrcCandidate = File(parent, "$baseName.lrc")
        if (lrcCandidate.exists() && lrcCandidate.length() > 0) {
            val content = lrcCandidate.readText(StandardCharsets.UTF_8)
            val synced = parseLrc(content)
            val plain = if (synced.isNotEmpty()) synced.joinToString("\n") { it.text } else content
            return ExtractedLyricsResult(plain.trim(), synced, source = "sidecar")
        }

        val txtCandidate = File(parent, "$baseName.txt")
        if (txtCandidate.exists() && txtCandidate.length() > 0) {
            val content = txtCandidate.readText(StandardCharsets.UTF_8)
            val synced = parseLrc(content)
            val plain = if (synced.isNotEmpty()) synced.joinToString("\n") { it.text } else content
            return ExtractedLyricsResult(plain.trim(), synced, source = "sidecar")
        }

        return null
    }

    /**
     * Reads ID3v2 metadata (Title, Artist, Album, USLT Lyrics) from audio files.
     */
    fun readAudioMetadata(file: File): AudioFileMetadata {
        if (!file.exists() || file.length() < 10) return AudioFileMetadata()

        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(10)
            raf.readFully(header)

            // Must start with 'ID3'
            if (header[0] != 0x49.toByte() || header[1] != 0x44.toByte() || header[2] != 0x33.toByte()) {
                return AudioFileMetadata()
            }

            val majorVersion = header[3].toInt() and 0xFF
            val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                    ((header[7].toInt() and 0x7F) shl 14) or
                    ((header[8].toInt() and 0x7F) shl 7) or
                    (header[9].toInt() and 0x7F)

            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var embeddedLyrics: String? = null

            var currentOffset = 10L
            val maxOffset = (10 + tagSize).toLong().coerceAtMost(raf.length())

            while (currentOffset + 10 <= maxOffset) {
                raf.seek(currentOffset)
                val frameHeader = ByteArray(10)
                raf.readFully(frameHeader)

                val frameId = String(frameHeader, 0, 4, StandardCharsets.ISO_8859_1)
                if (frameId.any { it.code == 0 } || !frameId.all { it in 'A'..'Z' || it in '0'..'9' }) {
                    break // End of frames / padding
                }

                val frameSize = if (majorVersion == 4) {
                    ((frameHeader[4].toInt() and 0x7F) shl 21) or
                            ((frameHeader[5].toInt() and 0x7F) shl 14) or
                            ((frameHeader[6].toInt() and 0x7F) shl 7) or
                            (frameHeader[7].toInt() and 0x7F)
                } else {
                    ((frameHeader[4].toInt() and 0xFF) shl 24) or
                            ((frameHeader[5].toInt() and 0xFF) shl 16) or
                            ((frameHeader[6].toInt() and 0xFF) shl 8) or
                            (frameHeader[7].toInt() and 0xFF)
                }

                if (frameSize <= 0 || currentOffset + 10 + frameSize > maxOffset) {
                    break
                }

                val frameData = ByteArray(frameSize)
                raf.readFully(frameData)
                currentOffset += 10 + frameSize

                when (frameId) {
                    "TIT2" -> title = decodeId3Text(frameData)
                    "TPE1" -> artist = decodeId3Text(frameData)
                    "TALB" -> album = decodeId3Text(frameData)
                    "USLT" -> {
                        // USLT frame: encoding (1 byte) + language (3 bytes) + content descriptor (null-terminated) + lyrics
                        val decoded = decodeUsltLyrics(frameData)
                        if (!decoded.isNullOrBlank()) {
                            embeddedLyrics = decoded
                        }
                    }
                }
            }

            return AudioFileMetadata(title = title, artist = artist, album = album, embeddedLyrics = embeddedLyrics)
        }
    }

    private fun decodeId3Text(data: ByteArray): String? {
        if (data.isEmpty()) return null
        val encodingByte = data[0].toInt() and 0xFF
        val charset = when (encodingByte) {
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            3 -> StandardCharsets.UTF_8
            else -> StandardCharsets.ISO_8859_1
        }
        val textBytes = data.copyOfRange(1, data.size)
        return String(textBytes, charset).trim { it <= ' ' || it.code == 0 }
    }

    private fun decodeUsltLyrics(data: ByteArray): String? {
        if (data.size < 5) return null
        val encodingByte = data[0].toInt() and 0xFF
        val charset = when (encodingByte) {
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            3 -> StandardCharsets.UTF_8
            else -> StandardCharsets.ISO_8859_1
        }
        // Skip encoding (1) + language (3)
        var offset = 4
        // Find null terminator for content descriptor
        if (encodingByte == 1 || encodingByte == 2) {
            while (offset + 1 < data.size && !(data[offset] == 0.toByte() && data[offset + 1] == 0.toByte())) {
                offset += 2
            }
            offset += 2
        } else {
            while (offset < data.size && data[offset] != 0.toByte()) {
                offset++
            }
            offset++
        }

        if (offset >= data.size) return null
        val lyricsBytes = data.copyOfRange(offset, data.size)
        return String(lyricsBytes, charset).trim { it <= ' ' || it.code == 0 }
    }

    fun parseArtistAndTitleFromFilename(filename: String): Pair<String, String> {
        val clean = cleanTitle(filename)
        if (clean.contains(" - ")) {
            val parts = clean.split(" - ", limit = 2)
            val p0 = cleanTitle(parts[0])
            val p1 = cleanTitle(parts[1])
            if (p0.isNotBlank() && p1.isNotBlank()) {
                return Pair(p0, p1)
            }
        }
        return Pair("", clean)
    }

    fun cleanTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\.(mp3|wav|flac|m4a|aac|ogg|wma|aif|aiff|au)$"), "")
            .replace(Regex("(?i)\\[(official|video|audio|lyrics|hd|4k|hq|visualizer|remastered|cover|lyrics video|audio)[^\\]]*\\]"), "")
            .replace(Regex("(?i)\\((official|video|audio|lyrics|hd|4k|hq|visualizer|remastered|cover|lyrics video|audio)[^\\)]*\\)"), "")
            .replace(Regex("(?i)\\((320kbps|128kbps|256kbps|flac|mp3|webrip|cdrip)[^\\)]*\\)"), "")
            .replace(Regex("(?i)\\[(320kbps|128kbps|256kbps|flac|mp3|webrip|cdrip)[^\\]]*\\]"), "")
            .replace(Regex("^\\d{1,2}[\\s._-]+"), "") // strip track numbers like "01 - " or "01. "
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseLrclibJson(json: String): ExtractedLyricsResult? {
        val plainLyrics = extractJsonField(json, "plainLyrics") ?: extractJsonField(json, "plain")
        val syncedLyricsRaw = extractJsonField(json, "syncedLyrics")
        val trackName = extractJsonField(json, "trackName") ?: extractJsonField(json, "name")
        val artistName = extractJsonField(json, "artistName")

        if (plainLyrics.isNullOrBlank() && syncedLyricsRaw.isNullOrBlank()) {
            return null
        }

        val synced = if (!syncedLyricsRaw.isNullOrBlank()) parseLrc(syncedLyricsRaw) else emptyList()
        val plain = plainLyrics?.takeIf { it.isNotBlank() } ?: synced.joinToString("\n") { it.text }

        return ExtractedLyricsResult(
            plainLyrics = plain.trim(),
            syncedLyrics = synced,
            source = "online",
            detectedTitle = trackName,
            detectedArtist = artistName
        )
    }

    private fun parseLrclibSearchJson(json: String): List<ExtractedLyricsResult> {
        val results = mutableListOf<ExtractedLyricsResult>()
        try {
            val root = JsonParser.parseString(json)
            if (root.isJsonArray) {
                for (elem in root.asJsonArray) {
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val plainLyrics = obj.get("plainLyrics")?.takeIf { !it.isJsonNull }?.asString
                        val syncedLyricsRaw = obj.get("syncedLyrics")?.takeIf { !it.isJsonNull }?.asString
                        val trackName = obj.get("trackName")?.takeIf { !it.isJsonNull }?.asString
                        val artistName = obj.get("artistName")?.takeIf { !it.isJsonNull }?.asString

                        if (!plainLyrics.isNullOrBlank() || !syncedLyricsRaw.isNullOrBlank()) {
                            val synced = if (!syncedLyricsRaw.isNullOrBlank()) parseLrc(syncedLyricsRaw) else emptyList()
                            val plain = plainLyrics?.takeIf { it.isNotBlank() } ?: synced.joinToString("\n") { it.text }
                            results.add(
                                ExtractedLyricsResult(
                                    plainLyrics = cleanAndDeduplicateLyrics(plain),
                                    syncedLyrics = cleanAndDeduplicateSegments(synced),
                                    source = "online",
                                    detectedTitle = trackName,
                                    detectedArtist = artistName
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error parsing LRCLIB search JSON with Gson: ${e.message}")
        }
        return results
    }

    private fun extractJsonField(json: String, fieldName: String): String? {
        val pattern = Pattern.compile("\"$fieldName\"\\s*:\\s*(?:\"((?:\\\\\"|[^\"])*)\"|null)")
        val matcher = pattern.matcher(json)
        if (matcher.find()) {
            val value = matcher.group(1) ?: return null
            return unescapeJson(value)
        }
        return null
    }

    private fun unescapeJson(s: String): String {
        return s
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    var lastError: String? = null

    // ==========================================
    // AI AUDIO LYRICS TRANSCRIPTION (WHISPER)
    // ==========================================

    /**
     * Transcribes vocals directly from an audio file or raw PCM float samples using local OpenAI Whisper.
     * Prioritizes ultra-lightweight, standalone whisper.cpp (~80 MB, 0 Python/PyTorch dependencies).
     * Falls back to Python Whisper if whisper.cpp is not available.
     */
    suspend fun transcribeWithAi(
        audioFile: File? = null,
        audioSamples: FloatArray? = null,
        modelName: String = "tiny",
        language: String? = null,
        onProgress: ((String) -> Unit)? = null
    ): ExtractedLyricsResult? = withContext(Dispatchers.IO) {
        lastError = null

        // 1. Primary: Ultra-fast standalone C++ Whisper (whisper.cpp) - Zero Python, Zero PyTorch, Zero DLL conflicts
        val cppResult = transcribeWithWhisperCpp(
            audioFile = audioFile,
            audioSamples = audioSamples,
            modelName = modelName,
            language = language,
            onProgress = onProgress
        )
        if (cppResult != null) {
            return@withContext cppResult
        }
        if (lastError?.contains("No vocals detected", ignoreCase = true) == true) {
            return@withContext null
        }

        // 2. Secondary fallback: Python Whisper script
        transcribeWithPythonWhisper(
            audioFile = audioFile,
            audioSamples = audioSamples,
            modelName = modelName,
            language = language,
            onProgress = onProgress
        )
    }

    /**
     * Standalone whisper.cpp inference (no Python, no PyTorch).
     */
    suspend fun transcribeWithWhisperCpp(
        audioFile: File? = null,
        audioSamples: FloatArray? = null,
        modelName: String = "tiny",
        language: String? = null,
        onProgress: ((String) -> Unit)? = null
    ): ExtractedLyricsResult? = withContext(Dispatchers.IO) {
        val cliExe = findWhisperCliExecutable() ?: return@withContext null
        val modelFile = findWhisperModelFile(modelName) ?: return@withContext null

        var tempAudioFile: File? = null
        val targetAudioFile: File = if (audioSamples != null && audioSamples.isNotEmpty()) {
            val temp = File.createTempFile("whisper_cpp_in_", ".wav")
            saveSamplesToWav(audioSamples, 16000, temp)
            tempAudioFile = temp
            temp
        } else if (audioFile != null && audioFile.exists()) {
            val ext = audioFile.extension.lowercase()
            if (ext in listOf("wav", "mp3", "flac", "ogg")) {
                audioFile
            } else {
                audioFile
            }
        } else {
            return@withContext null
        }

        val tempOutputPrefix = File.createTempFile("whisper_cpp_out_", "")
        val expectedJsonFile = File(tempOutputPrefix.absolutePath + ".json")

        try {
            onProgress?.invoke("Loading whisper.cpp (${modelFile.nameWithoutExtension})...")

            val command = mutableListOf<String>()
            command.add(cliExe.absolutePath)
            command.add("-m")
            command.add(modelFile.absolutePath)
            command.add("-f")
            command.add(targetAudioFile.absolutePath)
            command.add("-oj")
            command.add("-of")
            command.add(tempOutputPrefix.absolutePath)
            command.add("-pp")
            // Anti-hallucination and anti-repetition parameters:
            command.add("-mc")
            command.add("128") // Limit context window to prevent carrying hallucinated loops into next segments
            command.add("-sns") // Suppress non-speech tokens
            command.add("-nth")
            command.add("0.65") // Higher threshold so guitar/drums aren't hallucinated as singing

            val cores = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
            command.add("-t")
            command.add(cores.toString())

            val langNorm = language?.lowercase()?.trim() ?: "auto"
            if (langNorm != "auto" && langNorm.isNotBlank()) {
                command.add("-l")
                command.add(langNorm)
                if (langNorm == "hi" || langNorm == "ur") {
                    command.add("--prompt")
                    command.add("Hindi song lyrics, Hinglish Bollywood vocal music")
                } else if (langNorm == "bn") {
                    command.add("--prompt")
                    command.add("Bengali song lyrics, Bangla vocal music")
                }
            } else {
                command.add("-l")
                command.add("auto")
            }

            val pb = ProcessBuilder(command)
            pb.directory(cliExe.parentFile ?: File("."))
            pb.redirectErrorStream(true)
            ensureFfmpegInEnvironment(pb)

            val process = pb.start()
            val reader = process.inputStream.bufferedReader(StandardCharsets.UTF_8)
            val fullOutput = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                fullOutput.append(l).append("\n")
                if (l.startsWith("[") && l.contains("-->")) {
                    val textPart = l.substringAfter("]").trim()
                    if (textPart.isNotBlank() && !textPart.contains("BLANK_AUDIO", ignoreCase = true)) {
                        onProgress?.invoke("Transcribing: $textPart")
                    }
                } else if (l.contains("processing", ignoreCase = true) || l.contains("loading model", ignoreCase = true)) {
                    onProgress?.invoke("Whisper AI processing audio...")
                }
            }

            val exitCode = process.waitFor()

            if (expectedJsonFile.exists() && expectedJsonFile.length() > 0) {
                val jsonText = expectedJsonFile.readText(StandardCharsets.UTF_8)
                val parsed = parseWhisperCppJson(jsonText)
                if (parsed != null) {
                    onProgress?.invoke("Transcription complete!")
                    lastError = null
                    return@withContext parsed
                }
            }

            if (fullOutput.contains("BLANK_AUDIO", ignoreCase = true) || fullOutput.contains("no speech", ignoreCase = true)) {
                lastError = "No vocals detected in this audio. The track may be instrumental."
                return@withContext null
            }

            if (exitCode != 0) {
                System.err.println("whisper.cpp exited with code $exitCode:\n$fullOutput")
            }
            null
        } catch (e: Exception) {
            System.err.println("whisper.cpp execution error: ${e.message}")
            null
        } finally {
            tempAudioFile?.delete()
            tempOutputPrefix.delete()
            expectedJsonFile.delete()
        }
    }

    /**
     * Sanitizes lyrics by:
     * 1. Stripping HTML tags (<i>, </i>, <b>, etc.) and unescaping entities.
     * 2. Removing non-speech/sound-effect bracket tags ([MUSIC], [SUSH BROKE], [APPLAUSE], etc.).
     * 3. Eliminating Whisper hallucination repetition loops (caps identical consecutive lines to max 2).
     */
    private val SOUND_EFFECT_KEYWORDS = listOf(
        "music", "applause", "laughter", "blank_audio", "cheering",
        "chuckle", "cough", "singing", "instrumental", "foreign language",
        "sush broke", "sound", "noise", "whisper", "outro", "intro",
        "guitar solo", "beat", "drop", "_tt_"
    )

    /**
     * Checks if a line is a non-speech sound-effect or subtitle tag (e.g. [MUSIC], [SUSH BROKE], (Applause)).
     * Non-recursive string inspection prevents java.lang.StackOverflowError in regex engines.
     */
    fun isSoundEffectTag(text: String): Boolean {
        val trimmed = text.trim()
        if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || (trimmed.startsWith("(") && trimmed.endsWith(")"))) {
            // Do not treat LRC timestamp tags like [01:23.45] as sound effects
            if (trimmed.length in 8..12 && trimmed.startsWith("[") && trimmed.getOrNull(1)?.isDigit() == true && trimmed.contains(":")) {
                return false
            }
            val inner = trimmed.substring(1, trimmed.length - 1).trim().lowercase()
            return SOUND_EFFECT_KEYWORDS.any { inner.contains(it) }
        }
        return false
    }

    /**
     * Sanitizes lyrics by:
     * 1. Stripping HTML tags (<i>, </i>, <b>, etc.) and unescaping entities.
     * 2. Removing non-speech/sound-effect bracket tags ([MUSIC], [SUSH BROKE], [APPLAUSE], etc.).
     * 3. Eliminating Whisper hallucination repetition loops (caps identical consecutive lines to max 2).
     * 4. Collapsing excessive global repetition of short phrases.
     */
    fun cleanAndDeduplicateLyrics(text: String): String {
        if (text.isBlank()) return ""
        val lines = text.lines()
        val cleanedLines = mutableListOf<String>()
        var lastNormalized = ""
        var consecutiveCount = 0

        for (rawLine in lines) {
            var line = rawLine
                .replace(Regex("<[^>]+>"), "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim()

            if (line.isBlank()) {
                if (cleanedLines.isNotEmpty() && cleanedLines.last().isNotBlank()) {
                    cleanedLines.add("")
                }
                lastNormalized = ""
                consecutiveCount = 0
                continue
            }

            if (isSoundEffectTag(line)) {
                continue
            }

            if (line.startsWith("[") && line.endsWith("]") && !line.matches(Regex("""^\[\d{2}:\d{2}.*\]$"""))) {
                val inner = line.removeSurrounding("[", "]").trim()
                if (inner.equals("SUSH BROKE", ignoreCase = true) ||
                    inner.equals("BLANK_AUDIO", ignoreCase = true) ||
                    inner.equals("MUSIC", ignoreCase = true) ||
                    inner.startsWith("_TT_", ignoreCase = true)
                ) {
                    continue
                }
            }

            val normalized = line.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()

            if (normalized == lastNormalized && normalized.isNotBlank()) {
                consecutiveCount++
                if (consecutiveCount >= 2) {
                    continue
                }
            } else {
                lastNormalized = normalized
                consecutiveCount = 0
            }

            cleanedLines.add(line)
        }

        // Secondary pass: Detect and collapse global hallucination loops
        val frequencyMap = mutableMapOf<String, Int>()
        for (l in cleanedLines) {
            val norm = l.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
            if (norm.length in 3..25) {
                frequencyMap[norm] = (frequencyMap[norm] ?: 0) + 1
            }
        }

        val overRepeatedPhrases = frequencyMap.filter { it.value >= 4 }.keys
        if (overRepeatedPhrases.isNotEmpty()) {
            val finalLines = mutableListOf<String>()
            val phraseSeenCount = mutableMapOf<String, Int>()
            for (l in cleanedLines) {
                val norm = l.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
                if (norm in overRepeatedPhrases) {
                    val count = (phraseSeenCount[norm] ?: 0) + 1
                    phraseSeenCount[norm] = count
                    if (count > 2) {
                        continue
                    }
                }
                finalLines.add(l)
            }
            return finalLines.joinToString("\n").trim()
        }

        return cleanedLines.joinToString("\n").trim()
    }

    /**
     * Deduplicates and cleans SyncedLyricLine items, eliminating Whisper repetition loops.
     */
    fun cleanAndDeduplicateSegments(rawSegments: List<SyncedLyricLine>): List<SyncedLyricLine> {
        val cleaned = mutableListOf<SyncedLyricLine>()
        var lastNorm = ""
        var consecutiveCount = 0

        for (seg in rawSegments) {
            var text = seg.text
                .replace(Regex("<[^>]+>"), "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim()

            if (text.isBlank()) continue
            if (isSoundEffectTag(text)) continue

            if (text.startsWith("[") && text.endsWith("]")) {
                val inner = text.removeSurrounding("[", "]").trim()
                if (inner.equals("SUSH BROKE", ignoreCase = true) ||
                    inner.equals("BLANK_AUDIO", ignoreCase = true) ||
                    inner.equals("MUSIC", ignoreCase = true) ||
                    inner.startsWith("_TT_", ignoreCase = true)
                ) {
                    continue
                }
            }

            val norm = text.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
            if (norm == lastNorm && norm.isNotBlank()) {
                consecutiveCount++
                if (consecutiveCount >= 2) continue
            } else {
                lastNorm = norm
                consecutiveCount = 0
            }

            cleaned.add(SyncedLyricLine(seg.timestampSeconds, text))
        }

        return cleaned
    }

    /**
     * Parses output JSON generated by whisper-cli (-oj).
     */
    fun parseWhisperCppJson(json: String): ExtractedLyricsResult? {
        try {
            val root = JsonParser.parseString(json).asJsonObject
            val transArray = root.getAsJsonArray("transcription") ?: return null
            val rawSegments = mutableListOf<SyncedLyricLine>()

            for (elem in transArray) {
                val segObj = elem.asJsonObject
                val rawText = segObj.get("text")?.asString?.trim() ?: continue
                if (rawText.isBlank()) continue

                var startSec: Double? = null
                if (segObj.has("offsets")) {
                    val offsets = segObj.getAsJsonObject("offsets")
                    if (offsets.has("from")) {
                        startSec = offsets.get("from").asLong / 1000.0
                    }
                }
                if (startSec == null && segObj.has("timestamps")) {
                    val timestamps = segObj.getAsJsonObject("timestamps")
                    if (timestamps.has("from")) {
                        startSec = parseTimestampToSeconds(timestamps.get("from").asString)
                    }
                }
                val finalStart = startSec ?: 0.0
                rawSegments.add(SyncedLyricLine(finalStart, rawText))
            }

            val cleanedSegments = cleanAndDeduplicateSegments(rawSegments)
            if (cleanedSegments.isEmpty()) return null

            val plain = cleanAndDeduplicateLyrics(cleanedSegments.joinToString("\n") { it.text })
            if (plain.isBlank()) return null

            return ExtractedLyricsResult(
                plainLyrics = plain,
                syncedLyrics = cleanedSegments,
                source = "ai_whisper"
            )
        } catch (e: Exception) {
            System.err.println("Failed to parse whisper.cpp json: ${e.message}")
            return null
        }
    }

    private fun parseTimestampToSeconds(ts: String): Double {
        return try {
            val parts = ts.replace(',', '.').split(':')
            if (parts.size == 3) {
                val h = parts[0].trim().toDoubleOrNull() ?: 0.0
                val m = parts[1].trim().toDoubleOrNull() ?: 0.0
                val s = parts[2].trim().toDoubleOrNull() ?: 0.0
                h * 3600.0 + m * 60.0 + s
            } else if (parts.size == 2) {
                val m = parts[0].trim().toDoubleOrNull() ?: 0.0
                val s = parts[1].trim().toDoubleOrNull() ?: 0.0
                m * 60.0 + s
            } else {
                ts.replace(',', '.').toDoubleOrNull() ?: 0.0
            }
        } catch (_: Exception) {
            0.0
        }
    }

    /**
     * Resolves whisper-cli.exe executable from tools/whisper, Scoop installation, or system PATH.
     */
    fun findWhisperCliExecutable(): File? {
        val projectRoot = findProjectRoot()
        val userHome = System.getProperty("user.home") ?: ""
        val userDir = System.getProperty("user.dir") ?: "."
        val codeLocation = try {
            LyricsExtractor::class.java.protectionDomain?.codeSource?.location?.toURI()?.let { File(it) }
        } catch (_: Exception) { null }
        val appDir = if (codeLocation != null && !codeLocation.isDirectory) codeLocation.parentFile else codeLocation

        val candidates = listOf(
            File("tools/whisper/whisper-cli.exe"),
            File("whisper/whisper-cli.exe"),
            File(userDir, "tools/whisper/whisper-cli.exe"),
            File(userDir, "whisper/whisper-cli.exe"),
            appDir?.let { File(it, "whisper/whisper-cli.exe") },
            appDir?.let { File(it, "../whisper/whisper-cli.exe") },
            appDir?.let { File(it, "../../whisper/whisper-cli.exe") },
            projectRoot?.let { File(it, "tools/whisper/whisper-cli.exe") },
            projectRoot?.let { File(it, "desktopApp/build/compose/binaries/main/app/TabsLite/whisper/whisper-cli.exe") },
            File(userHome, "scoop/apps/whisper-cpp/current/whisper-cli.exe"),
            File(userHome, "scoop/shims/whisper-cli.exe"),
            File("C:/Ai/whisper/whisper-cli.exe")
        ).filterNotNull()

        for (c in candidates) {
            if (c.exists()) return c
        }
        return null
    }

    /**
     * Finds GGML model file (e.g. ggml-tiny.bin, ggml-base.bin) for whisper.cpp.
     */
    fun findWhisperModelFile(modelName: String = "tiny"): File? {
        val projectRoot = findProjectRoot()
        val userHome = System.getProperty("user.home") ?: ""
        val userDir = System.getProperty("user.dir") ?: "."
        val codeLocation = try {
            LyricsExtractor::class.java.protectionDomain?.codeSource?.location?.toURI()?.let { File(it) }
        } catch (_: Exception) { null }
        val appDir = if (codeLocation != null && !codeLocation.isDirectory) codeLocation.parentFile else codeLocation

        val norm = if (modelName.isBlank()) "tiny" else modelName.lowercase()
        val targetFile = "ggml-$norm.bin"

        val searchDirs = listOf(
            File("tools/whisper/models"),
            File("whisper/models"),
            File(userDir, "tools/whisper/models"),
            File(userDir, "whisper/models"),
            appDir?.let { File(it, "whisper/models") },
            appDir?.let { File(it, "../whisper/models") },
            projectRoot?.let { File(it, "tools/whisper/models") },
            projectRoot?.let { File(it, "desktopApp/build/compose/binaries/main/app/TabsLite/whisper/models") },
            File(userHome, "scoop/apps/whisper-cpp/current/models"),
            File(userHome, ".cache/whisper")
        ).filterNotNull()

        for (dir in searchDirs) {
            val f = File(dir, targetFile)
            if (f.exists() && f.length() > 1000) return f
        }

        for (dir in searchDirs) {
            val f = File(dir, "ggml-tiny.bin")
            if (f.exists() && f.length() > 1000) return f
        }

        for (dir in searchDirs) {
            if (dir.isDirectory) {
                val anyBin = dir.listFiles { file -> file.isFile && file.name.endsWith(".bin") }?.firstOrNull()
                if (anyBin != null && anyBin.length() > 1000) return anyBin
            }
        }
        return null
    }

    /**
     * Fallback transcription using Python openai-whisper package.
     */
    suspend fun transcribeWithPythonWhisper(
        audioFile: File? = null,
        audioSamples: FloatArray? = null,
        modelName: String = "tiny",
        language: String? = null,
        onProgress: ((String) -> Unit)? = null
    ): ExtractedLyricsResult? = withContext(Dispatchers.IO) {
        var tempAudioFile: File? = null
        val targetAudioFile = if (audioFile != null && audioFile.exists()) {
            audioFile
        } else if (audioSamples != null && audioSamples.isNotEmpty()) {
            val temp = File.createTempFile("whisper_input_", ".wav")
            saveSamplesToWav(audioSamples, 16000, temp)
            tempAudioFile = temp
            temp
        } else {
            lastError = "No audio file or recorded samples available for transcription"
            return@withContext null
        }

        val tempJsonFile = File.createTempFile("whisper_lyrics_", ".json")

        try {
            val pythonCmd = findPythonExecutable() ?: run {
                lastError = "Python with openai-whisper package not found. Run: pip install openai-whisper"
                System.err.println(lastError)
                return@withContext null
            }

            val scriptFile = resolveScriptFile()
            onProgress?.invoke("Loading Whisper AI ($modelName model)...")

            val command = mutableListOf<String>()
            command.add(pythonCmd)
            command.add(scriptFile.absolutePath)
            command.add(targetAudioFile.absolutePath)
            command.add(modelName)
            command.add(tempJsonFile.absolutePath)
            if (!language.isNullOrBlank()) {
                command.add(language)
            }

            val pb = ProcessBuilder(command)
            pb.redirectErrorStream(true)
            pb.directory(File(System.getProperty("user.home") ?: "."))
            pb.environment()["KMP_DUPLICATE_LIB_OK"] = "TRUE"
            ensureFfmpegInEnvironment(pb)

            val process = pb.start()
            val reader = process.inputStream.bufferedReader(StandardCharsets.UTF_8)
            val fullOutput = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                fullOutput.append(l).append("\n")
                if (l.contains("%|") || l.contains("frames/s")) {
                    onProgress?.invoke("Transcribing vocals: $l")
                } else if (l.contains("Detected language:")) {
                    onProgress?.invoke(l)
                }
            }

            val exitCode = process.waitFor()
            if (exitCode != 0 && (!tempJsonFile.exists() || tempJsonFile.length() == 0L)) {
                val errSummary = fullOutput.toString().lines().lastOrNull { it.isNotBlank() && !it.contains("%|") }
                    ?: "Process exited with code $exitCode"
                lastError = errSummary
                System.err.println("Whisper process error ($exitCode):\n$fullOutput")
                try {
                    File(System.getProperty("user.home") ?: ".", ".tabslite_whisper.log").writeText("Exit code: $exitCode\nOutput:\n$fullOutput")
                } catch (_: Exception) {}
                return@withContext null
            }

            if (tempJsonFile.exists() && tempJsonFile.length() > 0) {
                val jsonContent = tempJsonFile.readText(StandardCharsets.UTF_8)
                if (jsonContent.contains("\"error\":")) {
                    val errMsg = extractJsonField(jsonContent, "error") ?: "Transcription failed"
                    lastError = errMsg
                    return@withContext null
                }
                val result = parseWhisperJson(jsonContent)
                if (result != null) {
                    onProgress?.invoke("Transcription complete!")
                    lastError = null
                    return@withContext result
                }
            }

            lastError = "No valid transcription segments received from Whisper."
            null
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}: ${e.message}"
            System.err.println("Error during Whisper AI transcription: ${e.message}")
            e.printStackTrace()
            null
        } finally {
            tempAudioFile?.delete()
            tempJsonFile.delete()
        }
    }

    /**
     * Searches upward from current working directory and class code source to find project root containing .venv or transcribe_lyrics.py
     */
    private fun findProjectRoot(): File? {
        val startDirs = mutableListOf<File>()
        try {
            startDirs.add(File(System.getProperty("user.dir") ?: "."))
        } catch (_: Exception) {}
        try {
            val codeLocation = LyricsExtractor::class.java.protectionDomain?.codeSource?.location?.toURI()?.let { File(it) }
            if (codeLocation != null) {
                startDirs.add(if (codeLocation.isDirectory) codeLocation else (codeLocation.parentFile ?: codeLocation))
            }
        } catch (_: Exception) {}

        for (start in startDirs) {
            var cur: File? = try { start.canonicalFile } catch (_: Exception) { start }
            var depth = 0
            while (cur != null && depth < 12) {
                if (File(cur, ".venv\\Scripts\\python.exe").exists() || File(cur, "transcribe_lyrics.py").exists()) {
                    return cur
                }
                cur = cur.parentFile
                depth++
            }
        }
        return null
    }

    /**
     * Injects known ffmpeg and torch lib installation paths into ProcessBuilder environment.
     */
    fun ensureFfmpegInEnvironment(pb: ProcessBuilder) {
        val env = pb.environment()
        env["KMP_DUPLICATE_LIB_OK"] = "TRUE"
        val currentPath = env["PATH"] ?: env["Path"] ?: ""
        val projectRoot = findProjectRoot()
        val candidateDirs = listOf(
            projectRoot?.let { File(it, ".venv\\Scripts").takeIf { f -> f.isDirectory }?.absolutePath },
            File(".venv\\Scripts").takeIf { it.isDirectory }?.absolutePath,
            File(System.getProperty("user.dir") ?: ".", ".venv\\Scripts").takeIf { it.isDirectory }?.absolutePath,
            File("../.venv/Scripts").takeIf { it.isDirectory }?.absolutePath,
            File("../../.venv/Scripts").takeIf { it.isDirectory }?.absolutePath,
            "C:\\Users\\sayan\\Documents\\GitHub\\Tabs-Lite-master\\.venv\\Scripts",
            "C:\\Ai\\ffmpeg",
            "C:\\ffmpeg\\bin",
            "C:\\Program Files\\ffmpeg\\bin",
            "C:\\Program Files\\Python312",
            "C:\\Program Files\\Python312\\Scripts",
            System.getenv("USERPROFILE")?.let { "$it\\scoop\\shims" },
            System.getenv("LOCALAPPDATA")?.let { "$it\\Programs\\ffmpeg\\bin" },
            System.getenv("APPDATA")?.let { "$it\\Python\\Python312\\site-packages\\torch\\lib" },
            System.getenv("LOCALAPPDATA")?.let { "$it\\Programs\\Python\\Python312\\Lib\\site-packages\\torch\\lib" },
            "C:\\Program Files\\Python312\\Lib\\site-packages\\torch\\lib"
        ).filterNotNull()

        val validDirs = candidateDirs.filter { dir ->
            File(dir).isDirectory
        }

        if (validDirs.isNotEmpty()) {
            val newPath = validDirs.joinToString(";") + ";" + currentPath
            env["PATH"] = newPath
            env["Path"] = newPath
        }
    }

    /**
     * Parses Whisper JSON output into ExtractedLyricsResult with SyncedLyricLine items.
     */
    fun parseWhisperJson(json: String): ExtractedLyricsResult? {
        val overallText = extractJsonField(json, "text") ?: ""
        val segments = mutableListOf<SyncedLyricLine>()

        val segIdx = json.indexOf("\"segments\"")
        if (segIdx != -1) {
            val arrayStart = json.indexOf('[', segIdx)
            val arrayEnd = json.lastIndexOf(']')
            if (arrayStart != -1 && arrayEnd > arrayStart) {
                val arrayContent = json.substring(arrayStart, arrayEnd)
                val objMatcher = Pattern.compile("\\{([^{}]+)\\}").matcher(arrayContent)
                while (objMatcher.find()) {
                    val obj = objMatcher.group(1)
                    val startMatcher = Pattern.compile("\"start\"\\s*:\\s*([0-9.]+)").matcher(obj)
                    val textMatcher = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"").matcher(obj)
                    if (startMatcher.find() && textMatcher.find()) {
                        val startSec = startMatcher.group(1).toDoubleOrNull() ?: 0.0
                        val text = unescapeJson(textMatcher.group(1)).trim()
                        if (text.isNotBlank()) {
                            segments.add(SyncedLyricLine(startSec, text))
                        }
                    }
                }
            }
        }

        val cleanedSegments = cleanAndDeduplicateSegments(segments)
        val plain = cleanAndDeduplicateLyrics(
            if (cleanedSegments.isNotEmpty()) {
                cleanedSegments.joinToString("\n") { it.text }
            } else {
                overallText.trim()
            }
        )

        if (plain.isBlank()) return null

        return ExtractedLyricsResult(
            plainLyrics = plain,
            syncedLyrics = cleanedSegments,
            source = "ai_whisper"
        )
    }

    /**
     * Finds Python executable on system, prioritizing installations with whisper installed.
     */
    fun findPythonExecutable(): String? {
        val projectRoot = findProjectRoot()
        val candidates = listOf(
            projectRoot?.let { File(it, ".venv\\Scripts\\python.exe").takeIf { f -> f.exists() }?.absolutePath },
            File(".venv\\Scripts\\python.exe").takeIf { it.exists() }?.absolutePath,
            File(System.getProperty("user.dir") ?: ".", ".venv\\Scripts\\python.exe").takeIf { it.exists() }?.absolutePath,
            File("../.venv/Scripts/python.exe").takeIf { it.exists() }?.absolutePath,
            File("../../.venv/Scripts/python.exe").takeIf { it.exists() }?.absolutePath,
            File("../../../.venv/Scripts/python.exe").takeIf { it.exists() }?.absolutePath,
            File("../../../../.venv/Scripts/python.exe").takeIf { it.exists() }?.absolutePath,
            "C:\\Users\\sayan\\Documents\\GitHub\\Tabs-Lite-master\\.venv\\Scripts\\python.exe",
            "C:\\Program Files\\Python312\\python.exe",
            "C:\\Program Files\\Python311\\python.exe",
            "C:\\Program Files\\Python310\\python.exe",
            System.getenv("LOCALAPPDATA")?.let { "$it\\Programs\\Python\\Python312\\python.exe" },
            System.getenv("LOCALAPPDATA")?.let { "$it\\Programs\\Python\\Python311\\python.exe" },
            System.getenv("LOCALAPPDATA")?.let { "$it\\Programs\\Python\\Python310\\python.exe" },
            "python",
            "python3",
            "C:\\WINDOWS\\py.exe",
            "py"
        ).filterNotNull()

        // Pass 1: find one that has whisper package installed (fast 50ms check without DLL overhead)
        for (cmd in candidates) {
            val file = File(cmd)
            if (file.isAbsolute && !file.exists()) continue
            try {
                val pb = ProcessBuilder(cmd, "-c", "import importlib.util, sys; sys.exit(0 if importlib.util.find_spec('whisper') else 1)")
                pb.redirectErrorStream(true)
                pb.environment()["KMP_DUPLICATE_LIB_OK"] = "TRUE"
                val p = pb.start()
                val ok = p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
                if (ok && p.exitValue() == 0) {
                    return cmd
                }
            } catch (_: Exception) {
            }
        }

        // Pass 2: fallback to any working Python executable
        for (cmd in candidates) {
            val file = File(cmd)
            if (file.isAbsolute && !file.exists()) continue
            try {
                val pb = ProcessBuilder(cmd, "--version")
                pb.redirectErrorStream(true)
                val p = pb.start()
                val ok = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
                if (ok && p.exitValue() == 0) {
                    return cmd
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    /**
     * Resolves the transcribe_lyrics.py script file on disk or extracts it from classpath.
     */
    fun resolveScriptFile(): File {
        val projectRoot = findProjectRoot()
        val candidates = mutableListOf<File>()
        if (projectRoot != null) {
            candidates.add(File(projectRoot, "transcribe_lyrics.py"))
        }
        candidates.addAll(listOf(
            File("transcribe_lyrics.py"),
            File(System.getProperty("user.dir") ?: ".", "transcribe_lyrics.py"),
            File("../transcribe_lyrics.py"),
            File("../../transcribe_lyrics.py"),
            File("../../../transcribe_lyrics.py")
        ))
        for (c in candidates) {
            if (c.exists() && c.length() > 50) {
                val content = try { c.readText(StandardCharsets.UTF_8) } catch (_: Exception) { "" }
                if (content.contains("preload_torch_dlls")) {
                    return c
                }
            }
        }

        val tempScript = File.createTempFile("transcribe_lyrics_", ".py")
        tempScript.deleteOnExit()

        val stream = LyricsExtractor::class.java.getResourceAsStream("/scripts/transcribe_lyrics.py")
            ?: LyricsExtractor::class.java.classLoader?.getResourceAsStream("scripts/transcribe_lyrics.py")
            ?: Thread.currentThread().contextClassLoader?.getResourceAsStream("scripts/transcribe_lyrics.py")

        var usedResource = false
        if (stream != null) {
            val streamText = try { stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() } } catch (_: Exception) { "" }
            if (streamText.contains("preload_torch_dlls")) {
                tempScript.writeText(streamText, StandardCharsets.UTF_8)
                usedResource = true
            }
        }
        if (!usedResource) {
            tempScript.writeText(EMBEDDED_TRANSCRIBE_SCRIPT, StandardCharsets.UTF_8)
        }
        return tempScript
    }

    /**
     * Encodes 16kHz float samples to 16-bit mono PCM WAV file for Whisper processing.
     */
    fun saveSamplesToWav(samples: FloatArray, sampleRate: Int = 16000, outputFile: File) {
        val numSamples = samples.size
        val byteRate = sampleRate * 1 * 2
        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize
        outputFile.outputStream().buffered().use { out ->
            out.write("RIFF".toByteArray(StandardCharsets.US_ASCII))
            out.write(intToLittleEndian(totalSize))
            out.write("WAVE".toByteArray(StandardCharsets.US_ASCII))
            out.write("fmt ".toByteArray(StandardCharsets.US_ASCII))
            out.write(intToLittleEndian(16))
            out.write(shortToLittleEndian(1))
            out.write(shortToLittleEndian(1))
            out.write(intToLittleEndian(sampleRate))
            out.write(intToLittleEndian(byteRate))
            out.write(shortToLittleEndian(2))
            out.write(shortToLittleEndian(16))
            out.write("data".toByteArray(StandardCharsets.US_ASCII))
            out.write(intToLittleEndian(dataSize))
            val buf = ByteArray(2)
            for (s in samples) {
                val clamped = s.coerceIn(-1.0f, 1.0f)
                val pcm = (clamped * 32767.0f).toInt().toShort()
                buf[0] = (pcm.toInt() and 0xFF).toByte()
                buf[1] = ((pcm.toInt() shr 8) and 0xFF).toByte()
                out.write(buf)
            }
        }
    }

    private fun intToLittleEndian(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToLittleEndian(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    private const val EMBEDDED_TRANSCRIBE_SCRIPT = """import sys
import os
import io

os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

import ctypes

def preload_torch_dlls():
    try:
        search_dirs = []
        try:
            import site
            us = site.getusersitepackages()
            if isinstance(us, str):
                search_dirs.append(us)
            elif isinstance(us, (list, tuple)):
                search_dirs.extend(us)
            search_dirs.extend(site.getsitepackages())
        except Exception:
            pass

        if hasattr(sys, "prefix"):
            search_dirs.append(os.path.join(sys.prefix, "Lib", "site-packages"))
        if hasattr(sys, "base_prefix"):
            search_dirs.append(os.path.join(sys.base_prefix, "Lib", "site-packages"))

        appdata = os.environ.get("APPDATA")
        if appdata:
            for pyver in ["Python312", "Python311", "Python310", "Python313"]:
                search_dirs.append(os.path.join(appdata, "Python", pyver, "site-packages"))

        localappdata = os.environ.get("LOCALAPPDATA")
        if localappdata:
            for pyver in ["Python312", "Python311", "Python310", "Python313"]:
                search_dirs.append(os.path.join(localappdata, "Programs", "Python", pyver, "Lib", "site-packages"))

        for prog in [r"C:\Program Files\Python312", r"C:\Program Files\Python311", r"C:\Program Files\Python310"]:
            search_dirs.append(os.path.join(prog, "Lib", "site-packages"))

        for base in search_dirs:
            if not base or not os.path.isdir(base):
                continue
            tlib = os.path.join(base, "torch", "lib")
            if os.path.isdir(tlib):
                if hasattr(os, "add_dll_directory"):
                    try:
                        os.add_dll_directory(tlib)
                    except Exception:
                        pass
                os.environ["PATH"] = tlib + os.pathsep + os.environ.get("PATH", "")

                for dll_name in ["libiomp5md.dll", "uv.dll", "c10.dll", "torch_cpu.dll", "torch_global_deps.dll", "torch.dll"]:
                    dll_file = os.path.join(tlib, dll_name)
                    if os.path.exists(dll_file):
                        try:
                            ctypes.CDLL(dll_file)
                        except Exception:
                            pass
    except Exception:
        pass

preload_torch_dlls()

try:
    import torch
except Exception:
    pass

ffmpeg_candidates = [
    r"C:\Ai\ffmpeg",
    r"C:\ffmpeg\bin",
    r"C:\Program Files\ffmpeg\bin",
    os.path.join(os.environ.get("USERPROFILE", ""), "scoop", "shims"),
    os.path.join(os.environ.get("LOCALAPPDATA", ""), "Programs", "ffmpeg", "bin"),
]
for p in ffmpeg_candidates:
    if os.path.exists(p) and p not in os.environ.get("PATH", ""):
        os.environ["PATH"] = p + os.pathsep + os.environ.get("PATH", "")

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

import json
import whisper

def transcribe(audio_path, model_name="tiny", output_file=None, language=None):
    if not os.path.exists(audio_path):
        return {"error": f"Audio file not found: {audio_path}"}
    print(f"Loading Whisper model: {model_name}", flush=True)
    model = whisper.load_model(model_name)
    print("Model loaded. Starting transcription...", flush=True)
    kwargs = {"verbose": True, "fp16": False, "condition_on_previous_text": False}
    if language and language.lower() not in ("auto", "none", ""):
        kwargs["language"] = language.lower()
    try:
        result = model.transcribe(audio_path, **kwargs)
    except Exception as e:
        err = str(e)
        if "model output" in err.lower() or "no speech" in err.lower() or "empty" in err.lower():
            return {"error": "No vocals detected. The track may be instrumental or audio quality is too low."}
        raise
    segments = []
    for s in result.get("segments", []):
        if s.get("no_speech_prob", 0) > 0.85:
            continue
        start = round(float(s["start"]), 2)
        end = round(float(s["end"]), 2)
        text = s["text"].strip()
        if text:
            segments.append({"start": start, "end": end, "text": text})
    plain_text = result.get("text", "").strip()
    if not plain_text and not segments:
        return {"error": "No vocals detected. The track may be instrumental or speech recognition failed on this audio."}
    data = {"text": plain_text, "segments": segments, "language": result.get("language", "en")}
    if output_file:
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    return data

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"error": "Missing audio file path argument"}))
        sys.exit(1)
    audio_path = sys.argv[1]
    model_name = sys.argv[2] if len(sys.argv) > 2 else "tiny"
    out_file = sys.argv[3] if len(sys.argv) > 3 and sys.argv[3] != "none" else None
    language = sys.argv[4] if len(sys.argv) > 4 and sys.argv[4] != "auto" else None
    try:
        data = transcribe(audio_path, model_name, out_file, language)
        if "error" in data:
            err_msg = data["error"]
            if out_file:
                try:
                    with open(out_file, "w", encoding="utf-8") as f:
                        json.dump({"error": err_msg}, f)
                except Exception:
                    pass
            print(json.dumps({"error": err_msg}))
            sys.exit(1)
        print("---TRANSCRIPTION_START---")
        print(json.dumps(data, ensure_ascii=False))
        print("---TRANSCRIPTION_END---")
    except Exception as e:
        import traceback
        traceback.print_exc(file=sys.stderr)
        err_str = str(e)
        if "model output" in err_str.lower():
            err_msg = "No vocals detected in this audio. The track may be instrumental."
        elif "ffmpeg" in err_str.lower() or "FileNotFoundError" in err_str:
            err_msg = "ffmpeg not found. Ensure ffmpeg is installed and in PATH."
        else:
            err_msg = f"{type(e).__name__}: {err_str}"
        if out_file:
            try:
                with open(out_file, "w", encoding="utf-8") as f:
                    json.dump({"error": err_msg}, f)
            except Exception:
                pass
        print(json.dumps({"error": err_msg}))
        sys.exit(1)
"""}


