package com.gbros.tabslite.utilities

import com.gbros.tabslite.data.Tab
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

object BackendConnection {
    private var apiKey: String? = null
    private val apiKeyMutex = Mutex()
    private var deviceId: String = generateDeviceId()

    private fun generateDeviceId(): String {
        val chars = "0123456789abcdef"
        return (1..16).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private suspend fun getApiKey(forceRefresh: Boolean = false): String = withContext(Dispatchers.IO) {
        if (!forceRefresh && apiKey != null) return@withContext apiKey!!
        apiKeyMutex.lock()
        try {
            if (!forceRefresh && apiKey != null) return@withContext apiKey!!
            val serverTime = fetchServerTime()
            val md5Input = "$deviceId$serverTime" + "createLog()"
            apiKey = getMd5(md5Input)
            println("Generated Ultimate Guitar API Key: $apiKey (serverTime: $serverTime)")
            return@withContext apiKey!!
        } finally {
            apiKeyMutex.unlock()
        }
    }

    private fun fetchServerTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd:H", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        return try {
            val url = URL("https://api.ultimate-guitar.com/api/v1/common/hello")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "UGT_ANDROID/5.10.12 (")
            conn.setRequestProperty("x-ug-client-id", deviceId)
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val json = conn.inputStream.use { stream ->
                InputStreamReader(stream).use { reader ->
                    JsonParser.parseReader(reader).asJsonObject
                }
            }
            val timestamp = when {
                json.has("timestamp") -> json.get("timestamp").asLong
                json.has("results") && json.get("results").isJsonObject && json.getAsJsonObject("results").has("server_time") ->
                    json.getAsJsonObject("results").get("server_time").asLong
                else -> System.currentTimeMillis() / 1000L
            }
            sdf.format(timestamp * 1000L)
        } catch (e: Exception) {
            println("Server time fetch fallback: ${e.message}")
            sdf.format(System.currentTimeMillis())
        }
    }

    private fun getMd5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        var hex = BigInteger(1, digest).toString(16)
        while (hex.length < 32) {
            hex = "0$hex"
        }
        return hex
    }

    private suspend fun openAuthenticatedConnection(urlString: String): HttpURLConnection = withContext(Dispatchers.IO) {
        var key = getApiKey()
        var conn = URL(urlString).openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "UGT_ANDROID/5.10.12 (")
        conn.setRequestProperty("x-ug-client-id", deviceId)
        conn.setRequestProperty("x-ug-api-key", key)
        conn.connectTimeout = 8000
        conn.readTimeout = 8000

        var responseCode = try { conn.responseCode } catch (e: Exception) { 0 }
        if (responseCode == 498) {
            // Bad token, refresh api key and retry
            println("498 Bad token, refreshing api key...")
            conn.disconnect()
            key = getApiKey(forceRefresh = true)
            conn = URL(urlString).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "UGT_ANDROID/5.10.12 (")
            conn.setRequestProperty("x-ug-client-id", deviceId)
            conn.setRequestProperty("x-ug-api-key", key)
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
        }
        return@withContext conn
    }

    suspend fun searchTabs(query: String, page: Int = 1): List<Tab> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val urlString = "https://api.ultimate-guitar.com/api/v1/tab/search?title=$encoded&page=$page&type[]=300&type[]=400&type[]=200&official[]=0"

        val resultList = mutableListOf<Tab>()
        try {
            val conn = openAuthenticatedConnection(urlString)
            conn.inputStream.use { stream ->
                InputStreamReader(stream).use { reader ->
                    val root = JsonParser.parseReader(reader)
                    val tabsArray: JsonArray? = when {
                        root.isJsonObject && root.asJsonObject.has("tabs") -> root.asJsonObject.getAsJsonArray("tabs")
                        root.isJsonArray -> root.asJsonArray
                        else -> null
                    }

                    if (tabsArray != null) {
                        for (elem in tabsArray) {
                            if (!elem.isJsonObject) continue
                            val obj = elem.asJsonObject
                            val tabId = obj.get("id")?.asString ?: continue
                            val songName = obj.get("song_name")?.asString ?: "Untitled"
                            val artistName = obj.get("artist_name")?.asString ?: "Unknown"

                            val tab = Tab(
                                tabId = tabId,
                                songId = obj.get("song_id")?.asString ?: "",
                                songName = songName,
                                artistName = artistName,
                                artistId = obj.get("artist_id")?.asString ?: "",
                                type = obj.get("type_name")?.asString ?: obj.get("type")?.asString ?: "Chords",
                                version = obj.get("version")?.asInt ?: 1,
                                votes = obj.get("votes")?.asInt ?: 0,
                                rating = obj.get("rating")?.asDouble ?: 0.0,
                                tonalityName = obj.get("tonality_name")?.asString ?: "",
                                difficulty = obj.get("difficulty")?.asString ?: "novice",
                                tuning = obj.get("tuning")?.asString ?: "E A D G B E",
                                capo = obj.get("capo")?.asInt ?: 0
                            )
                            resultList.add(tab)
                        }
                    }
                }
            }
            println("Search for '$query' returned ${resultList.size} tabs")
        } catch (e: Exception) {
            println("Search exception for '$query': ${e.message}")
        }
        return@withContext resultList
    }

    private fun findStringInJson(element: com.google.gson.JsonElement, key: String): String {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has(key) && obj.get(key).isJsonPrimitive) {
                val str = obj.get(key).asString
                if (str.isNotBlank()) return str
            }
            for (entry in obj.entrySet()) {
                val res = findStringInJson(entry.value, key)
                if (res.isNotBlank()) return res
            }
        }
        return ""
    }

    private fun findIntInJson(element: com.google.gson.JsonElement, key: String): Int? {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has(key) && obj.get(key).isJsonPrimitive) {
                return try { obj.get(key).asInt } catch (e: Exception) { null }
            }
            for (entry in obj.entrySet()) {
                val res = findIntInJson(entry.value, key)
                if (res != null) return res
            }
        }
        return null
    }

    suspend fun fetchTabContent(tabId: String): Tab = withContext(Dispatchers.IO) {
        val cleanId = tabId.replace("7567-", "")
        val urlString = "https://api.ultimate-guitar.com/api/v1/tab/info?tab_id=$cleanId&tab_access_type=public"
        val conn = openAuthenticatedConnection(urlString)

        val stream = try {

            conn.inputStream
        } catch (e: Exception) {
            val errorText = conn.errorStream?.reader()?.readText() ?: "No error body"
            println("Fetch tab error response (Code ${conn.responseCode}): $errorText")
            throw e
        }

        stream.use { inStream ->

            InputStreamReader(inStream).use { reader ->

                val root = JsonParser.parseReader(reader)
                val content = findStringInJson(root, "content")
                val songName = findStringInJson(root, "song_name")
                val artistName = findStringInJson(root, "artist_name")
                val typeName = findStringInJson(root, "type_name").ifBlank { findStringInJson(root, "type") }.ifBlank { "Chords" }
                val tonalityName = findStringInJson(root, "tonality_name")
                val difficulty = findStringInJson(root, "difficulty").ifBlank { "novice" }
                val tuning = findStringInJson(root, "tuning").ifBlank { "E A D G B E" }
                val capo = findIntInJson(root, "capo") ?: 0
                val version = findIntInJson(root, "version") ?: 1
                val votes = findIntInJson(root, "votes") ?: 0

                val strummingList = mutableListOf<com.gbros.tabslite.data.StrummingPattern>()
                val tabObj = when {
                    root.isJsonObject && root.asJsonObject.has("tab") && root.asJsonObject.get("tab").isJsonObject -> root.asJsonObject.getAsJsonObject("tab")
                    root.isJsonObject && root.asJsonObject.has("tab_view") && root.asJsonObject.get("tab_view").isJsonObject -> root.asJsonObject.getAsJsonObject("tab_view")
                    root.isJsonObject -> root.asJsonObject
                    else -> null
                }

                val strummingArr = when {
                    tabObj != null && tabObj.has("strumming") && tabObj.get("strumming").isJsonArray -> tabObj.getAsJsonArray("strumming")
                    tabObj != null && tabObj.has("strummings") && tabObj.get("strummings").isJsonArray -> tabObj.getAsJsonArray("strummings")
                    root.isJsonObject && root.asJsonObject.has("strumming") && root.asJsonObject.get("strumming").isJsonArray -> root.asJsonObject.getAsJsonArray("strumming")
                    root.isJsonObject && root.asJsonObject.has("strummings") && root.asJsonObject.get("strummings").isJsonArray -> root.asJsonObject.getAsJsonArray("strummings")
                    else -> null
                }

                if (strummingArr != null) {
                    for (elem in strummingArr) {
                        if (!elem.isJsonObject) continue
                        val obj = elem.asJsonObject
                        val part = (obj.get("part")?.asString ?: "").ifBlank { "Whole song" }
                        val bpm = obj.get("bpm")?.asInt ?: 96
                        val denuminator = obj.get("denuminator")?.asInt ?: 16
                        val isTriplet = (obj.get("is_triplet")?.asInt ?: 0) != 0
                        val measures = mutableListOf<Int>()
                        if (obj.has("measures") && obj.get("measures").isJsonArray) {
                            for (mElem in obj.getAsJsonArray("measures")) {
                                if (mElem.isJsonObject && mElem.asJsonObject.has("measure")) {
                                    measures.add(mElem.asJsonObject.get("measure").asInt)
                                } else if (mElem.isJsonPrimitive) {
                                    measures.add(mElem.asInt)
                                }
                            }
                        }
                        if (measures.isNotEmpty()) {
                            strummingList.add(
                                com.gbros.tabslite.data.StrummingPattern(
                                    part = part,
                                    bpm = bpm,
                                    denuminator = denuminator,
                                    isTriplet = isTriplet,
                                    measures = measures
                                )
                            )
                        }
                    }
                }

                println("Loaded tab $tabId content length: ${content.length}, song: $songName by $artistName, strumming patterns: ${strummingList.size}")

                Tab(
                    tabId = tabId,
                    songId = cleanId,
                    songName = songName,
                    artistName = artistName,
                    type = typeName,
                    version = version,
                    votes = votes,
                    tonalityName = tonalityName,
                    difficulty = difficulty,
                    tuning = tuning,
                    capo = capo,
                    content = content,
                    strumming = strummingList
                )
            }
        }
    }


    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val urlString = "https://api.ultimate-guitar.com/api/v1/tab/suggestion?q=$encoded"

        val list = mutableListOf<String>()
        try {
            val conn = openAuthenticatedConnection(urlString)
            conn.inputStream.use { stream ->
                InputStreamReader(stream).use { reader ->
                    val root = JsonParser.parseReader(reader)
                    if (root.isJsonObject && root.asJsonObject.has("suggestions")) {
                        val arr = root.asJsonObject.getAsJsonArray("suggestions")
                        for (item in arr) {
                            list.add(item.asString)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore suggestion errors
        }
        return@withContext list
    }
}
