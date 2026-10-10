package com.gbros.tabslite.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AppSettings(
    val themeMode: String = "OBSIDIAN_DARK", // OBSIDIAN_DARK, MIDNIGHT_BLUE, AMOLED_BLACK, NORD_SLATE, WARM_EMBER, LIGHT_SLATE
    val accentColor: String = "BLUE", // BLUE, GREEN, PURPLE, AMBER, ROSE
    val defaultInstrument: String = "Guitar (Standard E)",
    val defaultScrollSpeed: Float = 20f,
    val defaultFontSize: Float = 15f,
    val twoColumnDefault: Boolean = false,
    val showChordPopups: Boolean = true,
    val smoothScroll: Boolean = true,
    val audioInputDevice: String = "Default System Microphone",
    val audioOutputDevice: String = "Default System Speakers",
    val aiDeviceMode: String = "Auto (CPU / DirectML)",
    val audioBufferSize: Int = 512,
    val autoFetchLyrics: Boolean = true,
    val highPrecisionDsp: Boolean = true,
    val keepScreenAwake: Boolean = true
)

@Serializable
data class DatabaseStorage(
    val tabs: MutableMap<String, Tab> = mutableMapOf(),
    val playlists: MutableList<Playlist> = mutableListOf(
        Playlist(
            id = Playlist.FAVORITES_PLAYLIST_ID,
            userCreated = false,
            title = "Favorites",
            description = "Your favorite guitar tabs"
        )
    ),
    val playlistEntries: MutableList<DataPlaylistEntry> = mutableListOf(),
    val history: MutableList<HistoryEntry> = mutableListOf(),
    var settings: AppSettings = AppSettings()
)

class DesktopDatabase private constructor() {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val dbFile: File
    private val mutex = Mutex()
    private var storage = DatabaseStorage()

    private val _playlistsFlow = MutableStateFlow<List<Playlist>>(emptyList())
    val playlistsFlow: StateFlow<List<Playlist>> = _playlistsFlow.asStateFlow()

    private val _favoritesFlow = MutableStateFlow<List<Tab>>(emptyList())
    val favoritesFlow: StateFlow<List<Tab>> = _favoritesFlow.asStateFlow()

    private val _historyFlow = MutableStateFlow<List<Tab>>(emptyList())
    val historyFlow: StateFlow<List<Tab>> = _historyFlow.asStateFlow()

    private val _settingsFlow = MutableStateFlow(AppSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()


    init {
        val userHome = System.getProperty("user.home") ?: "."
        val appDir = File(userHome, ".tabslite")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        dbFile = File(appDir, "data.json")
        loadData()
    }

    private fun loadData() {
        try {
            if (dbFile.exists()) {
                val content = dbFile.readText()
                if (content.isNotBlank()) {
                    storage = json.decodeFromString(content)
                }
            }
            // Ensure Favorites playlist exists
            if (storage.playlists.none { it.id == Playlist.FAVORITES_PLAYLIST_ID }) {
                storage.playlists.add(
                    0,
                    Playlist(
                        id = Playlist.FAVORITES_PLAYLIST_ID,
                        userCreated = false,
                        title = "Favorites",
                        description = "Your favorite guitar tabs"
                    )
                )
            }
        } catch (e: Exception) {
            println("Storage load warning: ${e.message}")
            storage = DatabaseStorage()
        }
        publishState()
    }

    private fun saveData() {
        try {
            val content = json.encodeToString(storage)
            dbFile.writeText(content)
        } catch (e: Exception) {
            println("Storage save error: ${e.message}")
        }
        publishState()
    }

    private fun publishState() {
        _playlistsFlow.value = storage.playlists.toList()
        val favEntries = storage.playlistEntries.filter { it.playlistId == Playlist.FAVORITES_PLAYLIST_ID }
        _favoritesFlow.value = favEntries.mapNotNull { entry -> storage.tabs[entry.tabId] }

        // Sort history by last viewed descending
        val sortedHistory = storage.history.sortedByDescending { it.lastViewed }
        _historyFlow.value = sortedHistory.mapNotNull { entry -> storage.tabs[entry.tabId] }
        _settingsFlow.value = storage.settings
    }

    suspend fun addToHistory(tab: Tab) = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.tabs[tab.tabId] = tab
            // Remove existing entry for this tab if present
            storage.history.removeAll { it.tabId == tab.tabId }
            // Add new entry at top
            storage.history.add(0, HistoryEntry(tabId = tab.tabId, lastViewed = System.currentTimeMillis()))
            // Keep maximum 100 entries in history
            if (storage.history.size > 100) {
                storage.history.removeLast()
            }
            saveData()
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.history.clear()
            saveData()
        }
    }

    suspend fun removeFromHistory(tabId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.history.removeAll { it.tabId == tabId }
            saveData()
        }
    }

    suspend fun saveTab(tab: Tab) = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.tabs[tab.tabId] = tab
            saveData()
        }
    }


    suspend fun getTab(tabId: String): Tab? = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.tabs[tabId]
        }
    }

    suspend fun isFavorite(tabId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.playlistEntries.any { it.playlistId == Playlist.FAVORITES_PLAYLIST_ID && it.tabId == tabId }
        }
    }

    suspend fun toggleFavorite(tab: Tab): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.tabs[tab.tabId] = tab
            val exists = storage.playlistEntries.any { it.playlistId == Playlist.FAVORITES_PLAYLIST_ID && it.tabId == tab.tabId }
            if (exists) {
                storage.playlistEntries.removeAll { it.playlistId == Playlist.FAVORITES_PLAYLIST_ID && it.tabId == tab.tabId }
            } else {
                val nextEntryId = (storage.playlistEntries.maxOfOrNull { it.entryId } ?: 0) + 1
                storage.playlistEntries.add(
                    DataPlaylistEntry(
                        entryId = nextEntryId,
                        playlistId = Playlist.FAVORITES_PLAYLIST_ID,
                        tabId = tab.tabId,
                        dateAdded = System.currentTimeMillis(),
                        transpose = tab.transpose ?: 0
                    )
                )
            }
            saveData()
            !exists
        }
    }

    suspend fun createPlaylist(title: String, description: String = ""): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val newId = (storage.playlists.maxOfOrNull { it.id } ?: 0) + 1
            val playlist = Playlist(
                id = newId,
                userCreated = true,
                title = title,
                dateCreated = System.currentTimeMillis(),
                dateModified = System.currentTimeMillis(),
                description = description
            )
            storage.playlists.add(playlist)
            saveData()
            newId
        }
    }

    suspend fun addTabToPlaylist(playlistId: Int, tab: Tab) = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.tabs[tab.tabId] = tab
            val nextEntryId = (storage.playlistEntries.maxOfOrNull { it.entryId } ?: 0) + 1
            storage.playlistEntries.add(
                DataPlaylistEntry(
                    entryId = nextEntryId,
                    playlistId = playlistId,
                    tabId = tab.tabId,
                    dateAdded = System.currentTimeMillis(),
                    transpose = tab.transpose ?: 0
                )
            )
            saveData()
        }
    }

    suspend fun removeTabFromPlaylist(playlistId: Int, tabId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.playlistEntries.removeAll { it.playlistId == playlistId && it.tabId == tabId }
            saveData()
        }
    }

    suspend fun getPlaylistTabs(playlistId: Int): List<Tab> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entries = storage.playlistEntries.filter { it.playlistId == playlistId }
            entries.mapNotNull { entry -> storage.tabs[entry.tabId] }
        }
    }

    suspend fun updateSettings(update: (AppSettings) -> AppSettings) = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.settings = update(storage.settings)
            saveData()
        }
    }

    suspend fun clearNonLibraryCache() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val libraryTabIds = storage.playlistEntries.map { it.tabId }.toSet()
            val historyTabIds = storage.history.map { it.tabId }.toSet()
            val keepIds = libraryTabIds + historyTabIds
            storage.tabs.keys.retainAll(keepIds)
            saveData()
        }
    }

    suspend fun exportDatabaseJson(): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            json.encodeToString(storage)
        }
    }

    suspend fun importDatabaseJson(jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val decoded = json.decodeFromString<DatabaseStorage>(jsonContent)
                storage = decoded
                saveData()
                true
            } catch (e: Exception) {
                println("Failed to import database: ${e.message}")
                false
            }
        }
    }

    suspend fun getStats(): Map<String, Int> = withContext(Dispatchers.IO) {
        mutex.withLock {
            mapOf(
                "tabs" to storage.tabs.size,
                "playlists" to storage.playlists.size,
                "favorites" to storage.playlistEntries.count { it.playlistId == Playlist.FAVORITES_PLAYLIST_ID },
                "history" to storage.history.size
            )
        }
    }

    companion object {
        val instance: DesktopDatabase by lazy { DesktopDatabase() }
    }
}
