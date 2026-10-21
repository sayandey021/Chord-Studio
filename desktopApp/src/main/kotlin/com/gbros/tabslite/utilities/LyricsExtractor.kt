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
    val detectedArtist: String? = null,
    val detectedLanguage: String? = null
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

    data class SupportedLanguage(
        val code: String,
        val displayName: String,
        val nativeName: String,
        val category: String,
        val isHybrid: Boolean = false,
        val promptHint: String = ""
    )

    val SUPPORTED_LANGUAGES = listOf(
        SupportedLanguage("auto", "Auto Detect", "⚡ Auto", "Auto", false),

        // Indian & South Asian
        SupportedLanguage("hi", "Hindi", "हिन्दी (Devanagari)", "Indian & South Asian", false, "Hindi song lyrics, Bollywood vocal music, classical and pop"),
        SupportedLanguage("bn", "Bengali", "বাংলা (Bangla)", "Indian & South Asian", false, "Bengali song lyrics, Rabindra Sangeet, Bangla rock and pop vocal music"),
        SupportedLanguage("pa", "Punjabi", "ਪੰਜਾਬੀ (Gurmukhi)", "Indian & South Asian", false, "Punjabi song lyrics, Bhangra and Sufi vocal music"),
        SupportedLanguage("ur", "Urdu", "اردو (Ghazal / Sufi)", "Indian & South Asian", false, "Urdu song lyrics, Ghazal, Coke Studio Pakistan vocal music"),
        SupportedLanguage("ta", "Tamil", "தமிழ்", "Indian & South Asian", false, "Tamil song lyrics, Kollywood vocal music"),
        SupportedLanguage("te", "Telugu", "తెలుగు", "Indian & South Asian", false, "Telugu song lyrics, Tollywood vocal music"),
        SupportedLanguage("mr", "Marathi", "मराठी", "Indian & South Asian", false, "Marathi song lyrics, Bhavgeet and Natyasangeet vocal music"),
        SupportedLanguage("gu", "Gujarati", "ગુજરાતી", "Indian & South Asian", false, "Gujarati song lyrics, Garba and Sugam Sangeet vocal music"),
        SupportedLanguage("kn", "Kannada", "ಕನ್ನಡ", "Indian & South Asian", false, "Kannada song lyrics, Bhavageethe vocal music"),
        SupportedLanguage("ml", "Malayalam", "മലയാളം", "Indian & South Asian", false, "Malayalam song lyrics, Mollywood vocal music"),
        SupportedLanguage("ne", "Nepali", "नेपाली", "Indian & South Asian", false, "Nepali song lyrics, Nepali folk and pop vocal music"),

        // Popular Global
        SupportedLanguage("en", "English", "English", "Popular Global", false, "English song lyrics, pop rock acoustic vocal music"),
        SupportedLanguage("es", "Spanish", "Español", "Popular Global", false, "Letra de canción en español, pop latino, reggaeton, flamenco vocal"),

        // European
        SupportedLanguage("fr", "French", "Français", "European", false, "Paroles de chanson en français, pop française, chanson vocale"),
        SupportedLanguage("de", "German", "Deutsch", "European", false, "Songtext auf Deutsch, Pop Rock Vokalmusik"),
        SupportedLanguage("it", "Italian", "Italiano", "European", false, "Testo della canzone in italiano, opera e pop musica vocale"),
        SupportedLanguage("pt", "Portuguese", "Português", "European", false, "Letra de música em português, Bossa Nova, pop brasileiro"),
        SupportedLanguage("ru", "Russian", "Русский", "European", false, "Текст песни на русском языке, русский рок и поп вокал"),
        SupportedLanguage("nl", "Dutch", "Nederlands", "European", false, "Songtekst in het Nederlands, akoestische vocale muziek"),
        SupportedLanguage("pl", "Polish", "Polski", "European", false, "Tekst piosenki po polsku, muzyka wokalna"),
        SupportedLanguage("sv", "Swedish", "Svenska", "European", false, "Låttext på svenska, vokalmusik"),
        SupportedLanguage("el", "Greek", "Ελληνικά", "European", false, "Στίχοι τραγουδιού στα ελληνικά, φωνητική μουσική"),

        // East & Southeast Asian
        SupportedLanguage("ja", "Japanese", "日本語 (J-Pop)", "East Asian", false, "日本語の歌詞、J-Pop、アニソン、ボーカル音楽"),
        SupportedLanguage("ko", "Korean", "한국어 (K-Pop)", "East Asian", false, "한국어 노래 가사, K-Pop, 아이돌 발라드 보컬"),
        SupportedLanguage("zh", "Chinese", "中文 (Mandopop)", "East Asian", false, "中文歌词，华语流行音乐，流行歌手演唱"),
        SupportedLanguage("id", "Indonesian", "Bahasa Indonesia", "East Asian", false, "Lirik lagu bahasa Indonesia, pop akustik vokal"),
        SupportedLanguage("tl", "Tagalog", "Filipino / Tagalog (OPM)", "East Asian", false, "Titik ng kanta sa Tagalog, Original Pilipino Music"),
        SupportedLanguage("vi", "Vietnamese", "Tiếng Việt (V-Pop)", "East Asian", false, "Lời bài hát tiếng Việt, nhạc trẻ V-Pop trữ tình"),
        SupportedLanguage("th", "Thai", "ไทย (T-Pop)", "East Asian", false, "เนื้อเพลงภาษาไทย, ดนตรีสากล เพลงป็อปไทย"),

        // Middle Eastern
        SupportedLanguage("ar", "Arabic", "العربية", "Middle Eastern", false, "كلمات أغنية عربية، طرب، موسيقى غنائية"),
        SupportedLanguage("tr", "Turkish", "Türkçe", "Middle Eastern", false, "Türkçe şarkı sözleri, akustik pop rock vokal"),
        SupportedLanguage("fa", "Persian", "فارسی (Farsi)", "Middle Eastern", false, "متن آهنگ به زبان فارسی، موسیقی سنتی و پاپ"),
        SupportedLanguage("he", "Hebrew", "עברית", "Middle Eastern", false, "מילות שירים בעברית, מוזיקה ווקאלית")
    )

    fun getLanguageDisplayName(code: String): String {
        val entry = SUPPORTED_LANGUAGES.firstOrNull { it.code.equals(code, ignoreCase = true) }
        if (entry != null) {
            return if (entry.isHybrid) "${entry.displayName} (${entry.nativeName})" else "${entry.displayName} (${entry.code.uppercase()})"
        }
        return java.util.Locale.forLanguageTag(code).getDisplayLanguage(java.util.Locale.ENGLISH).ifBlank { code.uppercase() }
    }

    /**
     * Accurately detects the vocal language of an audio track.
     * Unlike naive Whisper detection on 0..30s (where songs typically have instrumental
     * intros that cause Whisper to hallucinate or default to 'en'), this algorithm:
     * 1. Inspects metadata/filenames for non-Latin scripts (Devanagari, Bengali, Urdu/Arabic, Gurmukhi, etc.).
     * 2. If Romanized, samples 20s windows across vocal-active sections (bypassing the intro)
     *    and picks the highest-confidence language from whisper-cli's language detection head.
     */
    suspend fun detectAudioLanguage(
        audioSamples: FloatArray?,
        audioFile: File?,
        modelName: String = "base",
        hintTitle: String? = null,
        hintArtist: String? = null,
        onProgress: ((String) -> Unit)? = null
    ): Pair<String, Float>? = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("PROGRESS:0.04:Analyzing vocal sound wave to detect singing language...")
            // 1. Check if metadata / filename / user hints contain non-Latin scripts
            val metadataText = buildString {
                if (!hintTitle.isNullOrBlank()) append(hintTitle).append(" ")
                if (!hintArtist.isNullOrBlank()) append(hintArtist).append(" ")
                if (audioFile != null) {
                    append(audioFile.nameWithoutExtension).append(" ")
                    try {
                        val meta = readAudioMetadata(audioFile)
                        if (!meta.title.isNullOrBlank()) append(meta.title).append(" ")
                        if (!meta.artist.isNullOrBlank()) append(meta.artist).append(" ")
                    } catch (_: Exception) {}
                }
            }

            if (metadataText.any { it in '\u0980'..'\u09FF' }) return@withContext Pair("bn", 0.99f)
            if (metadataText.any { it in '\u0900'..'\u097F' }) {
                if (PhoneticTransliterator.isBengaliInDevanagari(metadataText)) {
                    return@withContext Pair("bn", 0.99f)
                }
                return@withContext Pair("hi", 0.99f)
            }
            if (metadataText.any { it in '\u0600'..'\u06FF' }) return@withContext Pair("ur", 0.99f)
            if (metadataText.any { it in '\u0A00'..'\u0A7F' }) return@withContext Pair("pa", 0.99f)
            if (metadataText.any { it in '\u0B80'..'\u0BFF' }) return@withContext Pair("ta", 0.99f)
            if (metadataText.any { it in '\u0C00'..'\u0C7F' }) return@withContext Pair("te", 0.99f)
            if (metadataText.any { it in '\u0A80'..'\u0AFF' }) return@withContext Pair("gu", 0.99f)
            if (metadataText.any { it in '\u0400'..'\u04FF' }) return@withContext Pair("ru", 0.99f)
            if (metadataText.any { it in '\uAC00'..'\uD7AF' }) return@withContext Pair("ko", 0.99f)
            if (metadataText.any { it in '\u3040'..'\u30FF' }) return@withContext Pair("ja", 0.99f)

            // Inspect Romanized keywords in filename / ID3 metadata
            val lowerMeta = metadataText.lowercase()
            val bengaliKeywords = listOf(
                "dugga", "asche", "amar", "amare", "sonar", "shonar", "bangla", "banglay", "bengali", "ami", "tumi",
                "amra", "tomra", "bhalobashi", "bhalobasa", "bhalobeshe", "valobashi", "valobasa", "valobeshe",
                "tomake", "amake", "tui", "tor", "amay", "amaye", "tomay", "tomaye", "ekla", "cholo", "gaan",
                "geeti", "sangeet", "rabindra", "rabindrasangeet", "rabindranath", "nazrul", "anupam roy", "anupam",
                "rupam islam", "rupam", "fossils", "cactus", "bhoomi", "chandrabindoo", "nachiketa", "kabir suman", "silajit",
                "anjan dutt", "rupankar", "srikanto", "lopamudra", "somlata", "lagnajita", "iman chakraborty",
                "jeet gannguli", "manna dey", "hemanta", "sandhya mukherjee", "mon majhi", "bojhena", "thakte dao",
                "sedin", "shedin", "bondhu", "kothay", "khub", "dekho", "shono", "kotha", "kintu", "chilo",
                "hobe", "ebong", "karon", "shei", "keno", "pujo", "puja", "kolkata", "dhaka", "bangladesh",
                "boishakh", "bristi", "brishti", "megh", "pakhi", "nodi", "chokh", "hridoy", "majhe", "paglu",
                "poran", "ashbe", "asbe", "hoyechilo", "obosheshe", "prithibi", "shur", "sur", "bhenge", "mor",
                "gaicher", "shune", "ghore", "phire", "alo", "andhar", "akash", "batash", "shondha", "bhor", "raat",
                "preme", "moner", "apni", "apnara", "tahole", "jodi", "tobe", "kichu", "kono", "sobai", "shobai",
                "jibon", "shob", "sob", "bhalo", "valo", "shundor", "sundor", "tollywood", "svf", "hoichoi"
            )
            val punjabiKeywords = listOf("punjabi", "bhangra", "diljit", "dosanjh", "sidhu", "moosewala", "ap dhillon", "karan aujla", "shubh", "jatt", "pind", "munde", "kudiye", "billo", "sohniye", "gabru")
            val urduKeywords = listOf("ghazal", "qawwali", "coke studio", "nusrat", "fateh", "ali khan", "rahat", "atif aslam", "kaavish", "mehdi hassan", "ghulam ali", "faiz", "iqbal")
            val spanishKeywords = listOf("cancion", "letra", "espanol", "reggaeton", "despacito", "flamenco")
            val frenchKeywords = listOf("chanson", "paroles", "francais")
            val germanKeywords = listOf("songtext", "deutsch")
            val japaneseKeywords = listOf("anime", "j-pop", "jpop", "vocaloid", "opening", "ending", "ost")
            val koreanKeywords = listOf("k-pop", "kpop", "bts", "blackpink", "kdrama")

            if (bengaliKeywords.any { lowerMeta.contains(it) }) return@withContext Pair("bn", 0.99f)
            if (punjabiKeywords.any { lowerMeta.contains(it) }) return@withContext Pair("pa", 0.99f)
            if (urduKeywords.any { lowerMeta.contains(it) }) return@withContext Pair("ur", 0.99f)
            if (spanishKeywords.any { lowerMeta.contains(it) }) return@withContext Pair("es", 0.99f)
            if (frenchKeywords.any { lowerMeta.contains(it) }) return@withContext Pair("fr", 0.99f)
            if (germanKeywords.any { lowerMeta.contains(it) }) return@withContext Pair("de", 0.99f)
            if (japaneseKeywords.any { lowerMeta.contains(it) }) return@withContext Pair("ja", 0.99f)
            if (koreanKeywords.any { lowerMeta.contains(it) }) return@withContext Pair("ko", 0.99f)

            // 2. Prepare samples
            val samples = if (audioSamples != null && audioSamples.isNotEmpty()) {
                audioSamples
            } else if (audioFile != null && audioFile.exists()) {
                try {
                    AudioAnalysisEngine.loadAudioFile(audioFile, 16000f)
                } catch (_: Exception) { null }
            } else {
                null
            } ?: return@withContext null

            val sampleRate = 16000
            val totalSeconds = samples.size / sampleRate
            if (totalSeconds < 5) return@withContext null

            val cliExe = findWhisperCliExecutable() ?: return@withContext null
            val modelFile = findWhisperModelFile(modelName, fallbackToInstalled = true) ?: return@withContext null

            // 3. Find candidate vocal windows (after intro, e.g. 20s, 40s, 65s, 95s)
            val candidateOffsets = mutableListOf<Int>()
            if (totalSeconds >= 30) candidateOffsets.add(20)
            if (totalSeconds >= 50) candidateOffsets.add(40)
            if (totalSeconds >= 75) candidateOffsets.add(65)
            if (totalSeconds >= 105) candidateOffsets.add(95)
            if (candidateOffsets.isEmpty()) candidateOffsets.add(0)

            // Rank candidate offsets by RMS energy so we prioritize loud vocal sections over quiet breaks
            val rankedOffsets = candidateOffsets.sortedByDescending { startSec ->
                val startIdx = (startSec * sampleRate).coerceIn(0, samples.size - 1)
                val endIdx = ((startSec + 20) * sampleRate).coerceIn(startIdx, samples.size)
                var sumSq = 0.0
                val count = endIdx - startIdx
                if (count > 0) {
                    val step = (count / 1000).coerceAtLeast(1)
                    var measured = 0
                    for (i in startIdx until endIdx step step) {
                        val s = samples[i]
                        sumSq += (s * s)
                        measured++
                    }
                    if (measured > 0) sumSq / measured else 0.0
                } else 0.0
            }

            val langVotes = mutableMapOf<String, Float>()
            var sampleStep = 0

            for (startSec in rankedOffsets.take(3)) {
                sampleStep++
                val pFrac = 0.05f + (sampleStep * 0.035f)
                onProgress?.invoke("PROGRESS:$pFrac:Analyzing vocal sound wave to detect singing language...")
                val startIdx = (startSec * sampleRate).coerceIn(0, samples.size - 1)
                val endIdx = ((startSec + 16) * sampleRate).coerceIn(startIdx, samples.size)
                if (endIdx - startIdx < sampleRate * 4) continue

                val chunkSamples = samples.copyOfRange(startIdx, endIdx)
                val tempChunkFile = File.createTempFile("whisper_lang_detect_", ".wav")
                try {
                    saveSamplesToWav(chunkSamples, sampleRate, tempChunkFile)

                    val detectCmd = mutableListOf(
                        cliExe.absolutePath,
                        "-m", modelFile.absolutePath,
                        "-f", tempChunkFile.absolutePath,
                        "-dl"
                    )
                    detectCmd.add("-ng")
                    val c = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
                    detectCmd.add("-t")
                    detectCmd.add(c.toString())

                    val pb = ProcessBuilder(detectCmd)
                    pb.directory(cliExe.parentFile ?: File("."))
                    pb.redirectErrorStream(true)
                    val proc = pb.start()
                    val output = proc.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
                    proc.waitFor()

                    val regex = Regex("""auto-detected language:\s*([a-zA-Z]{2,3})\s*\(p\s*=\s*([0-9.]+)\)""")
                    val match = regex.find(output)
                    if (match != null) {
                        val lang = match.groupValues[1].lowercase()
                        val prob = match.groupValues[2].toFloatOrNull() ?: 0.5f

                        // Boost Bengali over Hindi if detected: Whisper's base prior distribution is 50x biased to Hindi.
                        val effectiveProb = if (lang == "bn") prob * 1.30f else prob
                        langVotes[lang] = (langVotes[lang] ?: 0f) + effectiveProb

                        if (effectiveProb >= 0.70f) {
                            break
                        }
                    }
                } finally {
                    tempChunkFile.delete()
                }
            }

            val bestEntry = langVotes.maxByOrNull { it.value }
            val bestLang = bestEntry?.key
            val bestProb = if (sampleStep > 0 && bestEntry != null) bestEntry.value / sampleStep else 0f

            if (bestLang != null && bestProb >= 0.28f) {
                val langName = getLanguageDisplayName(bestLang)
                val conf = (bestProb.coerceAtMost(1f) * 100).toInt()
                onProgress?.invoke("PROGRESS:0.16:Detected singing language: $langName ($bestLang, $conf% confidence). Initializing AI...")
                Pair(bestLang, bestProb)
            } else {
                onProgress?.invoke("PROGRESS:0.16:Language auto-detection complete. Initializing AI...")
                null
            }
        } catch (e: Exception) {
            System.err.println("Language detection error: ${e.message}")
            null
        }
    }

    /**
     * Transcribes vocals directly from an audio file or raw PCM float samples using local OpenAI Whisper.
     * Prioritizes ultra-lightweight, standalone whisper.cpp (~80 MB, 0 Python/PyTorch dependencies).
     * Falls back to Python Whisper if whisper.cpp is not available.
     */
    suspend fun transcribeWithAi(
        audioFile: File? = null,
        audioSamples: FloatArray? = null,
        modelName: String = "base",
        language: String? = null,
        hintTitle: String? = null,
        hintArtist: String? = null,
        onProgress: ((String) -> Unit)? = null
    ): ExtractedLyricsResult? = withContext(Dispatchers.IO) {
        lastError = null

        val langNorm = language?.lowercase()?.trim() ?: "auto"
        var resolvedLanguage = langNorm
        val isHybrid = langNorm in listOf("hinglish", "benglish", "tanglish", "pinglish")

        if ((resolvedLanguage == "auto" || resolvedLanguage.isBlank()) && !isHybrid) {
            val detected = detectAudioLanguage(
                audioSamples = audioSamples,
                audioFile = audioFile,
                modelName = modelName,
                hintTitle = hintTitle,
                hintArtist = hintArtist,
                onProgress = onProgress
            )
            if (detected != null) {
                // If detected language is Hindi with moderate acoustic confidence (< 0.80) and no explicit Hindi metadata,
                // keep resolvedLanguage = "auto" so Whisper C++ transcribes dynamically rather than locking with a Bollywood Hindi prompt.
                val isAmbiguousHindi = detected.first == "hi" && detected.second < 0.80f
                if (!isAmbiguousHindi) {
                    resolvedLanguage = detected.first
                }
            }
        } else {
            val langName = getLanguageDisplayName(resolvedLanguage)
            onProgress?.invoke("PROGRESS:0.10:Preparing vocal audio for $langName transcription...")
        }

        // 1. Primary: Ultra-fast standalone C++ Whisper (whisper.cpp) - Zero Python, Zero PyTorch, Zero DLL conflicts
        val cppResult = transcribeWithWhisperCpp(
            audioFile = audioFile,
            audioSamples = audioSamples,
            modelName = modelName,
            language = resolvedLanguage,
            onProgress = onProgress
        )
        if (cppResult != null) {
            val isBengali = PhoneticTransliterator.isBengaliInDevanagari(cppResult.plainLyrics)
            val adjusted = if (isBengali) {
                val convertedPlain = PhoneticTransliterator.devanagariToBengali(cppResult.plainLyrics)
                val convertedSynced = cppResult.syncedLyrics.map {
                    it.copy(text = PhoneticTransliterator.devanagariToBengali(it.text))
                }
                cppResult.copy(
                    plainLyrics = convertedPlain,
                    syncedLyrics = convertedSynced,
                    detectedLanguage = "bn"
                )
            } else if (cppResult.plainLyrics.any { it in '\u0980'..'\u09FF' }) {
                cppResult.copy(detectedLanguage = "bn")
            } else if (cppResult.detectedLanguage == null && resolvedLanguage != "auto") {
                cppResult.copy(detectedLanguage = resolvedLanguage)
            } else cppResult

            return@withContext if (isHybrid) {
                adjusted.copy(
                    plainLyrics = PhoneticTransliterator.processHybridText(adjusted.plainLyrics, langNorm),
                    syncedLyrics = adjusted.syncedLyrics.map {
                        it.copy(text = PhoneticTransliterator.processHybridText(it.text, langNorm))
                    }
                )
            } else adjusted
        }
        if (lastError?.contains("No vocals detected", ignoreCase = true) == true) {
            return@withContext null
        }

        // 2. Secondary fallback: Python Whisper script
        val pyResult = transcribeWithPythonWhisper(
            audioFile = audioFile,
            audioSamples = audioSamples,
            modelName = modelName,
            language = resolvedLanguage,
            onProgress = onProgress
        )
        if (pyResult != null) {
            val isBengali = PhoneticTransliterator.isBengaliInDevanagari(pyResult.plainLyrics)
            val adjusted = if (isBengali) {
                val convertedPlain = PhoneticTransliterator.devanagariToBengali(pyResult.plainLyrics)
                val convertedSynced = pyResult.syncedLyrics.map {
                    it.copy(text = PhoneticTransliterator.devanagariToBengali(it.text))
                }
                pyResult.copy(
                    plainLyrics = convertedPlain,
                    syncedLyrics = convertedSynced,
                    detectedLanguage = "bn"
                )
            } else if (pyResult.plainLyrics.any { it in '\u0980'..'\u09FF' }) {
                pyResult.copy(detectedLanguage = "bn")
            } else if (pyResult.detectedLanguage == null && resolvedLanguage != "auto") {
                pyResult.copy(detectedLanguage = resolvedLanguage)
            } else pyResult

            if (isHybrid) {
                adjusted.copy(
                    plainLyrics = PhoneticTransliterator.processHybridText(adjusted.plainLyrics, langNorm),
                    syncedLyrics = adjusted.syncedLyrics.map {
                        it.copy(text = PhoneticTransliterator.processHybridText(it.text, langNorm))
                    },
                    detectedLanguage = langNorm
                )
            } else adjusted
        } else null
    }

    /**
     * Standalone whisper.cpp inference (no Python, no PyTorch).
     */
    suspend fun transcribeWithWhisperCpp(
        audioFile: File? = null,
        audioSamples: FloatArray? = null,
        modelName: String = "base",
        language: String? = null,
        onProgress: ((String) -> Unit)? = null
    ): ExtractedLyricsResult? = withContext(Dispatchers.IO) {
        val cliExe = findWhisperCliExecutable() ?: return@withContext null
        val modelFile = findWhisperModelFile(modelName, fallbackToInstalled = true) ?: return@withContext null

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
            onProgress?.invoke("PROGRESS:0.18:Loading AI model (${modelFile.nameWithoutExtension})...")

            val command = mutableListOf<String>()
            command.add(cliExe.absolutePath)
            command.add("-m")
            command.add(modelFile.absolutePath)
            command.add("-f")
            command.add(targetAudioFile.absolutePath)
            command.add("-ojf")
            command.add("-of")
            command.add(tempOutputPrefix.absolutePath)
            command.add("-pp")
            // Segment splitting for lyric line synchronization (crucial for large & turbo models):
            // Prevents Whisper from emitting 20-30s monolithic paragraph blocks.
            command.add("-ml")
            command.add("40") // Max ~40 characters per segment (1 lyric line)
            command.add("-sow") // Split strictly on word boundaries
            // Anti-hallucination and anti-repetition parameters:
            command.add("-sns") // Suppress non-speech tokens
            command.add("-nth")
            command.add("0.65") // Higher threshold so guitar/drums aren't hallucinated as singing

            // Pure CPU multi-core execution (AVX2 optimized)
            command.add("-ng")

            val cores = Runtime.getRuntime().availableProcessors().coerceIn(4, 12)
            command.add("-t")
            command.add(cores.toString())

            val langNorm = language?.lowercase()?.trim() ?: "auto"
            val resolvedLang = langNorm
            val isHybrid = langNorm in listOf("hinglish", "benglish", "tanglish", "pinglish")

            // For hybrid languages, transcribe using the native speech model (hi for Hinglish, bn for Benglish, etc.)
            // and then phonetically transliterate into Roman sing-along text.
            val whisperLang = when (langNorm) {
                "hinglish" -> "hi"
                "benglish" -> "bn"
                "tanglish" -> "ta"
                "pinglish" -> "pa"
                else -> resolvedLang
            }

            if (whisperLang != "auto" && whisperLang.isNotBlank()) {
                command.add("-l")
                command.add(whisperLang)
            } else {
                command.add("-l")
                command.add("auto")
            }

            // Language acoustic and script prompts to condition Whisper against hallucinating wrong Indic scripts
            val promptText = when (whisperLang) {
                "bn" -> "বাংলা গান, রবীন্দ্রসঙ্গীত, আধুনিক বাংলা গানের লিরিক্স"
                "hi" -> "हिन्दी गाने के बोल, बॉलीवुड संगीत"
                "pa" -> "ਪੰਜਾਬੀ ਗੀਤਾਂ ਦੇ ਬੋਲ, ਭੰਗੜਾ ਸੰਗੀਤ"
                "ur" -> "اردو گانے، غزل اور صوفی موسیقی کے بول"
                "ta" -> "தமிழ் பாடல் வரிகள்"
                "te" -> "తెలుగు పాట సాహిత్యం"
                else -> null
            }
            if (!promptText.isNullOrBlank()) {
                command.add("--prompt")
                command.add(promptText)
            }

            val pb = ProcessBuilder(command)
            pb.directory(cliExe.parentFile ?: File("."))
            pb.redirectErrorStream(true)
            ensureFfmpegInEnvironment(pb)

            val process = pb.start()
            val reader = process.inputStream.bufferedReader(StandardCharsets.UTF_8)
            val fullOutput = StringBuilder()
            var totalAudioDurationSec = if (audioSamples != null && audioSamples.isNotEmpty()) {
                audioSamples.size / 16000f
            } else 0f

            var line: String?
            var latestEndSec = 0f
            var whisperInternalPct = 0f
            var lastVocalText = ""

            // Parse a whisper timestamp string like "00:04.320" or "00:00:04.320" into seconds
            fun parseTimestampSec(ts: String): Float {
                val parts = ts.trim().split(":")
                return when (parts.size) {
                    2 -> (parts[0].toFloatOrNull() ?: 0f) * 60f + (parts[1].toFloatOrNull() ?: 0f)
                    3 -> (parts[0].toFloatOrNull() ?: 0f) * 3600f +
                         (parts[1].toFloatOrNull() ?: 0f) * 60f +
                         (parts[2].toFloatOrNull() ?: 0f)
                    else -> 0f
                }
            }

            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                fullOutput.append(l).append("\n")

                // 1. Parse audio duration from whisper init line if not known
                // e.g. "main: processing 'file.wav' (320000 samples, 20.0 sec)..."
                if (totalAudioDurationSec <= 0f && l.contains("main: processing") && l.contains("sec)")) {
                    val secMatch = Regex("""\(([0-9.]+)\s*sec\)""").find(l)
                    if (secMatch != null) {
                        totalAudioDurationSec = secMatch.groupValues[1].toFloatOrNull() ?: 0f
                    }
                }

                // 2. Parse whisper internal progress callback (-pp flag):
                // "whisper_print_progress_callback: progress = 42%"
                if (l.contains("whisper_print_progress_callback") && l.contains("%")) {
                    val pctMatch = Regex("""progress\s*=\s*(\d+)%""").find(l)
                    if (pctMatch != null) {
                        val pInt = pctMatch.groupValues[1].toIntOrNull() ?: 0
                        whisperInternalPct = pInt / 100f
                        val effectiveFrac = 0.20f + (whisperInternalPct * 0.78f)
                        val textDisplay = if (lastVocalText.isNotBlank()) "Transcribing: $lastVocalText" else "Transcribing vocals (${pInt}%)..."
                        onProgress?.invoke("PROGRESS:$effectiveFrac:$textDisplay")
                    }
                }

                // 3. Segment lines:
                // Whisper.cpp segment lines look like: [00:00.000 --> 00:04.320]  text
                // or with hours:                       [00:00:00.000 --> 00:00:04.320]  text
                if (l.startsWith("[") && l.contains("-->")) {
                    val arrowIdx   = l.indexOf("-->")
                    val closeBracket = l.indexOf("]", arrowIdx)
                    // Extract the end timestamp (between "-->" and "]")
                    val endTsRaw = if (closeBracket > arrowIdx + 3)
                        l.substring(arrowIdx + 3, closeBracket).trim()
                    else ""
                    val segEnd = if (endTsRaw.isNotEmpty()) parseTimestampSec(endTsRaw) else 0f

                    // Text follows the closing bracket
                    val textPart = if (closeBracket in 0 until l.length)
                        l.substring(closeBracket + 1).trim()
                    else l.substringAfter("]").trim()

                    if (segEnd > latestEndSec) latestEndSec = segEnd

                    val isCleanText = textPart.isNotBlank() &&
                        !textPart.contains("BLANK_AUDIO", ignoreCase = true) &&
                        !isSoundEffectTag(textPart)

                    if (isCleanText) {
                        lastVocalText = textPart
                    }

                    val timeProgress = if (totalAudioDurationSec > 0f) {
                        (latestEndSec / totalAudioDurationSec).coerceIn(0f, 1f)
                    } else 0f

                    val combinedProgress = maxOf(whisperInternalPct, timeProgress)
                    val overallFrac = 0.20f + (combinedProgress * 0.78f)
                    val textDisplay = if (lastVocalText.isNotBlank()) "Transcribing: $lastVocalText" else "Transcribing vocals (${(combinedProgress * 100).toInt()}%)..."

                    onProgress?.invoke("PROGRESS:$overallFrac:$textDisplay")
                } else if (l.contains("processing", ignoreCase = true) || l.contains("loading model", ignoreCase = true)) {
                    onProgress?.invoke("PROGRESS:0.20:AI processing vocal audio...")
                }
            }

            val exitCode = process.waitFor()

            if (expectedJsonFile.exists() && expectedJsonFile.length() > 0) {
                val jsonText = expectedJsonFile.readText(StandardCharsets.UTF_8)
                val parsed = parseWhisperCppJson(jsonText, if (resolvedLang != "auto") resolvedLang else null)
                if (parsed != null) {
                    val finalResult = if (isHybrid) {
                        parsed.copy(
                            plainLyrics = PhoneticTransliterator.processHybridText(parsed.plainLyrics, langNorm),
                            syncedLyrics = parsed.syncedLyrics.map {
                                it.copy(text = PhoneticTransliterator.processHybridText(it.text, langNorm))
                            }
                        )
                    } else parsed
                    onProgress?.invoke("PROGRESS:1.0:Finalizing lyrics & timestamps...")
                    lastError = null
                    return@withContext finalResult
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

            if (line.isBlank() || line.none { it.isLetter() }) {
                if (line.isBlank() && cleanedLines.isNotEmpty() && cleanedLines.last().isNotBlank()) {
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

            val normalized = line.lowercase().replace(Regex("""[^\p{L}\p{Nd}\s]"""), "").trim()

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
            val norm = l.lowercase().replace(Regex("""[^\p{L}\p{Nd}\s]"""), "").trim()
            if (norm.length in 3..25) {
                frequencyMap[norm] = (frequencyMap[norm] ?: 0) + 1
            }
        }

        val overRepeatedPhrases = frequencyMap.filter { it.value >= 4 }.keys
        if (overRepeatedPhrases.isNotEmpty()) {
            val finalLines = mutableListOf<String>()
            val phraseSeenCount = mutableMapOf<String, Int>()
            for (l in cleanedLines) {
                val norm = l.lowercase().replace(Regex("""[^\p{L}\p{Nd}\s]"""), "").trim()
                if (norm in overRepeatedPhrases) {
                    val count = (phraseSeenCount[norm] ?: 0) + 1
                    phraseSeenCount[norm] = count
                    if (count > 2) {
                        continue
                    }
                }
                finalLines.add(l)
            }
            return PhoneticTransliterator.sanitizeIndicScript(finalLines.joinToString("\n").trim())
        }

        return PhoneticTransliterator.sanitizeIndicScript(cleanedLines.joinToString("\n").trim())
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

            if (text.isBlank() || text.none { it.isLetter() }) continue
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

            val norm = text.lowercase().replace(Regex("""[^\p{L}\p{Nd}\s]"""), "").trim()
            if (norm == lastNorm && norm.isNotBlank()) {
                consecutiveCount++
                if (consecutiveCount >= 2) continue
            } else {
                lastNorm = norm
                consecutiveCount = 0
            }

            val sanitizedText = PhoneticTransliterator.sanitizeIndicScript(text)
            cleaned.add(SyncedLyricLine(seg.timestampSeconds, sanitizedText))
        }

        return cleaned
    }

    /**
     * Splits Whisper token stream into natural, synchronized lyric lines.
     * Uses silence gaps (>= 0.85s), punctuation, and phrase length constraints
     * to ensure large/turbo models produce accurately timed lines rather than 20-30s blocks.
     */
    fun extractLyricLinesFromTokens(tokensArr: JsonArray): List<SyncedLyricLine> {
        val result = mutableListOf<SyncedLyricLine>()
        val currentLineWords = StringBuilder()
        var lineStartSec: Double? = null
        var prevTokenEndSec = 0.0

        for (i in 0 until tokensArr.size()) {
            val tok = tokensArr.get(i).asJsonObject
            val text = tok.get("text")?.asString ?: ""
            if (text.startsWith("[_") || text.startsWith("<|") || text.endsWith("_]")) continue

            val offsets = if (tok.has("offsets")) tok.getAsJsonObject("offsets") else null
            val tokStart = if (offsets != null && offsets.has("from")) offsets.get("from").asLong / 1000.0 else prevTokenEndSec
            val tokEnd = if (offsets != null && offsets.has("to")) offsets.get("to").asLong / 1000.0 else tokStart

            if (lineStartSec == null) {
                lineStartSec = tokStart
            }

            val gap = tokStart - prevTokenEndSec
            val isPunctuationBreak = text.endsWith(".") || text.endsWith("?") || text.endsWith("!") || text.endsWith("|") || text.contains("\n")
            val isCommaBreak = (text.endsWith(",") || text.endsWith("，")) && currentLineWords.length >= 20
            val isGapBreak = gap >= 0.85 && currentLineWords.isNotBlank()
            val isLengthBreak = currentLineWords.length >= 45 && text.startsWith(" ")

            currentLineWords.append(text)
            prevTokenEndSec = tokEnd

            if (isPunctuationBreak || isCommaBreak || isGapBreak || isLengthBreak) {
                val cleaned = currentLineWords.toString().trim()
                if (cleaned.isNotBlank()) {
                    result.add(SyncedLyricLine(lineStartSec, cleaned))
                }
                currentLineWords.clear()
                lineStartSec = null
            }
        }

        val remaining = currentLineWords.toString().trim()
        if (remaining.isNotBlank()) {
            result.add(SyncedLyricLine(lineStartSec ?: prevTokenEndSec, remaining))
        }

        return result
    }

    /**
     * Parses output JSON generated by whisper-cli (-oj or -ojf).
     */
    fun parseWhisperCppJson(json: String, detectedLanguage: String? = null): ExtractedLyricsResult? {
        try {
            val root = JsonParser.parseString(json).asJsonObject
            val transArray = root.getAsJsonArray("transcription") ?: return null
            val detectedFromRoot = if (root.has("result")) {
                root.getAsJsonObject("result")?.get("language")?.asString
            } else null
            val finalLanguage = detectedLanguage ?: detectedFromRoot
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

                // If full token-level timestamps are present (-ojf), extract refined lyric lines!
                var addedFromTokens = false
                if (segObj.has("tokens")) {
                    val tokensArr = segObj.getAsJsonArray("tokens")
                    if (tokensArr != null && tokensArr.size() > 1) {
                        val subLines = extractLyricLinesFromTokens(tokensArr)
                        if (subLines.isNotEmpty()) {
                            rawSegments.addAll(subLines)
                            addedFromTokens = true
                        }
                    }
                }

                if (!addedFromTokens) {
                    if (rawText.contains("\n")) {
                        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
                        lines.forEachIndexed { i, l ->
                            rawSegments.add(SyncedLyricLine(finalStart + (i * 2.5), l))
                        }
                    } else {
                        rawSegments.add(SyncedLyricLine(finalStart, rawText))
                    }
                }
            }

            val cleanedSegments = cleanAndDeduplicateSegments(rawSegments)
            if (cleanedSegments.isEmpty()) return null

            val plain = cleanAndDeduplicateLyrics(cleanedSegments.joinToString("\n") { it.text })
            if (plain.isBlank()) return null

            return ExtractedLyricsResult(
                plainLyrics = plain,
                syncedLyrics = cleanedSegments,
                source = "ai_whisper",
                detectedLanguage = finalLanguage
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
     * Finds GGML model file (e.g. ggml-base.bin, ggml-large-v3-turbo.bin, ggml-large-v3.bin) for whisper.cpp.
     */
    fun findWhisperModelFile(modelName: String = "base", fallbackToInstalled: Boolean = false): File? {
        val projectRoot = findProjectRoot()
        val userHome = System.getProperty("user.home") ?: ""
        val userDir = System.getProperty("user.dir") ?: "."
        val codeLocation = try {
            LyricsExtractor::class.java.protectionDomain?.codeSource?.location?.toURI()?.let { File(it) }
        } catch (_: Exception) { null }
        val appDir = if (codeLocation != null && !codeLocation.isDirectory) codeLocation.parentFile else codeLocation

        val raw = if (modelName.isBlank()) "base" else modelName.trim().lowercase()
        val candidateNames = when (raw) {
            "turbo", "large-turbo", "large-v3-turbo", "turbo-large" -> listOf("ggml-large-v3-turbo.bin", "ggml-turbo.bin")
            "large", "large-v3" -> listOf("ggml-large-v3.bin", "ggml-large.bin")
            else -> listOf("ggml-$raw.bin")
        }

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
            File(userHome, ".cache/whisper"),
            File(userHome, ".chordstudio/models")
        ).filterNotNull()

        for (dir in searchDirs) {
            for (candidate in candidateNames) {
                val f = File(dir, candidate)
                if (f.exists() && f.length() > 1000) return f
            }
        }

        if (fallbackToInstalled) {
            val fallbackPriorities = listOf(
                "ggml-base.bin",
                "ggml-large-v3-turbo.bin",
                "ggml-small.bin",
                "ggml-large-v3.bin",
                "ggml-medium.bin"
            )
            for (modelFileName in fallbackPriorities) {
                for (dir in searchDirs) {
                    val f = File(dir, modelFileName)
                    if (f.exists() && f.length() > 1000) return f
                }
            }

            for (dir in searchDirs) {
                if (dir.isDirectory) {
                    val anyBin = dir.listFiles { file -> file.isFile && file.name.endsWith(".bin") }?.firstOrNull()
                    if (anyBin != null && anyBin.length() > 1000) return anyBin
                }
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
        modelName: String = "base",
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
            onProgress?.invoke("PROGRESS:0.18:Loading AI ($modelName model)...")

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
            pb.environment()["CHORD_STUDIO_DEVICE"] = "cpu"
            ensureFfmpegInEnvironment(pb)

            val process = pb.start()
            val reader = process.inputStream.bufferedReader(StandardCharsets.UTF_8)
            val fullOutput = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                fullOutput.append(l).append("\n")
                if (l.contains("%|") || l.contains("frames/s")) {
                    val pctMatch = Regex("""(\d+)%""").find(l)
                    val pInt = pctMatch?.groupValues?.get(1)?.toIntOrNull()
                    if (pInt != null) {
                        val overallFrac = 0.20f + ((pInt / 100f) * 0.78f)
                        onProgress?.invoke("PROGRESS:$overallFrac:Transcribing vocals ($pInt%)...")
                    } else {
                        onProgress?.invoke("PROGRESS:0.35:Transcribing vocals...")
                    }
                } else if (l.contains("Detected language:")) {
                    onProgress?.invoke("PROGRESS:0.20:$l")
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
                    val langNorm = language?.lowercase()?.trim() ?: "auto"
                    val isHybrid = langNorm in listOf("hinglish", "benglish", "tanglish", "pinglish")
                    val finalResult = if (isHybrid) {
                        result.copy(
                            plainLyrics = PhoneticTransliterator.processHybridText(result.plainLyrics, langNorm),
                            syncedLyrics = result.syncedLyrics.map {
                                it.copy(text = PhoneticTransliterator.processHybridText(it.text, langNorm))
                            }
                        )
                    } else result
                    onProgress?.invoke("PROGRESS:1.0:Finalizing lyrics & timestamps...")
                    lastError = null
                    return@withContext finalResult
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
    fun findProjectRoot(): File? {
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
        val detectedLang = extractJsonField(json, "language")
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
                            if (text.contains("\n")) {
                                val sub = text.lines().map { it.trim() }.filter { it.isNotBlank() }
                                sub.forEachIndexed { i, l ->
                                    segments.add(SyncedLyricLine(startSec + (i * 2.5), l))
                                }
                            } else {
                                segments.add(SyncedLyricLine(startSec, text))
                            }
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
            source = "ai_whisper",
            detectedLanguage = detectedLang
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

def transcribe(audio_path, model_name="base", output_file=None, language=None):
    if not os.path.exists(audio_path):
        return {"error": f"Audio file not found: {audio_path}"}
    if model_name in ("large-v3-turbo", "turbo-large", "large_turbo"):
        resolved_model = "large-v3-turbo"
    elif model_name in ("large", "large-v3"):
        resolved_model = "large-v3"
    elif not model_name or model_name.lower() in ("default", "tiny"):
        resolved_model = "base"
    else:
        resolved_model = model_name
    print(f"Loading Whisper model: {resolved_model}", flush=True)
    model = whisper.load_model(resolved_model)
    print("Model loaded. Starting transcription...", flush=True)
    kwargs = {"verbose": True, "fp16": False, "condition_on_previous_text": False, "word_timestamps": True}
    if language and language.lower() not in ("auto", "none", ""):
        kwargs["language"] = language.lower()
    else:
        try:
            audio_data = whisper.load_audio(audio_path)
            if len(audio_data) >= 16000 * 35:
                vocal_chunk = audio_data[16000 * 20 : 16000 * 50]
                mel = whisper.log_mel_spectrogram(whisper.pad_or_trim(vocal_chunk))
                _, probs = model.detect_language(mel)
                best_lang = max(probs, key=probs.get)
                if probs[best_lang] >= 0.40:
                    kwargs["language"] = best_lang
                    print(f"Detected vocal language: {best_lang} ({probs[best_lang]:.2f})", flush=True)
        except Exception as e:
            print(f"Vocal detection note: {e}", flush=True)
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
        words = s.get("words", [])
        if words and (float(s["end"]) - float(s["start"]) > 5.0 or len(words) > 7):
            cur_line_words = []
            cur_start = None
            prev_end = float(s["start"])
            for w in words:
                w_text = w.get("word", "").strip()
                if not w_text:
                    continue
                w_start = round(float(w.get("start", prev_end)), 2)
                w_end = round(float(w.get("end", w_start)), 2)
                if cur_start is None:
                    cur_start = w_start
                gap = w_start - prev_end
                cur_line_words.append(w_text)
                prev_end = w_end

                is_punct = any(w_text.endswith(p) for p in (".", "?", "!", "|", "\n"))
                is_comma = (w_text.endswith(",") or w_text.endswith("，")) and len(" ".join(cur_line_words)) >= 20
                is_gap = gap >= 0.85 and len(cur_line_words) > 1
                is_len = len(" ".join(cur_line_words)) >= 45

                if is_punct or is_comma or is_gap or is_len:
                    line_text = " ".join(cur_line_words).strip()
                    if line_text:
                        segments.append({"start": cur_start, "end": prev_end, "text": line_text})
                    cur_line_words = []
                    cur_start = None
            if cur_line_words:
                line_text = " ".join(cur_line_words).strip()
                if line_text:
                    segments.append({"start": cur_start if cur_start is not None else round(float(s["start"]), 2), "end": prev_end, "text": line_text})
        else:
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
    model_name = sys.argv[2] if len(sys.argv) > 2 else "base"
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


