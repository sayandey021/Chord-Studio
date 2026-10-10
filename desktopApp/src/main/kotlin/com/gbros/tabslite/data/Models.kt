package com.gbros.tabslite.data

import kotlinx.serialization.Serializable

enum class Instrument {
    Guitar,
    Ukulele
}

enum class ThemeSelection {
    System,
    Dark,
    Light
}

enum class FontStyle {
    Default,
    Monospace,
    Serif
}

enum class SortBy {
    Popularity,
    Name,
    Artist,
    DateAdded
}

enum class PlaylistsSortBy {
    DateModified,
    Name,
    Custom
}

enum class LoadingState {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}

@Serializable
data class StrummingPattern(
    val part: String = "Whole song",
    val bpm: Int = 96,
    val denuminator: Int = 16,
    val isTriplet: Boolean = false,
    val measures: List<Int> = emptyList()
)

@Serializable
data class TabDataType(
    var id: String = "",
    var song_id: String = "",
    var song_name: String = "",
    var artist_name: String = "",
    var artist_id: String = "",
    var type: String = "Chords",
    var version: Int = 1,
    var votes: Int = 0,
    var rating: Double = 0.0,
    var date: Long = 0L,
    var tonality_name: String = "",
    var difficulty: String = "novice",
    var tuning: String = "E A D G B E",
    var capo: Int = 0,
    var content: String = "",
    var transpose: Int? = 0,
    var strumming: List<StrummingPattern> = emptyList()
)

@Serializable
data class Tab(
    var tabId: String = "",
    var songId: String = "",
    var songName: String = "",
    var songGenre: String = "",
    var artistName: String = "",
    var artistId: String = "",
    var type: String = "Chords",
    var version: Int = 1,
    var votes: Int = 0,
    var rating: Double = 0.0,
    var date: Long = 0L,
    var tonalityName: String = "",
    var difficulty: String = "novice",
    var tuning: String = "E A D G B E",
    var capo: Int = 0,
    var contributorUserName: String = "Community",
    var content: String = "",
    var transpose: Int? = 0,
    var strumming: List<StrummingPattern> = emptyList()
) {
    constructor(data: TabDataType) : this(
        tabId = data.id,
        songId = data.song_id,
        songName = data.song_name,
        artistName = data.artist_name,
        artistId = data.artist_id,
        type = data.type,
        version = data.version,
        votes = data.votes,
        rating = data.rating,
        date = data.date,
        tonalityName = data.tonality_name,
        difficulty = data.difficulty,
        tuning = data.tuning,
        capo = data.capo,
        content = data.content,
        transpose = data.transpose,
        strumming = data.strumming
    )
}

@Serializable
data class Playlist(
    val id: Int = 0,
    val userCreated: Boolean = true,
    val title: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val description: String = ""
) {
    companion object {
        const val FAVORITES_PLAYLIST_ID = 1
        const val TOP_TABS_PLAYLIST_ID = 2
    }
}

@Serializable
data class DataPlaylistEntry(
    val entryId: Int = 0,
    val playlistId: Int = 0,
    val tabId: String = "",
    val nextEntryId: Int? = null,
    val prevEntryId: Int? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val transpose: Int = 0
)

data class TabWithDataPlaylistEntry(
    val tab: Tab,
    val entry: DataPlaylistEntry
)

@Serializable
data class ChordVariation(
    val id: String = "",
    val chordId: String = "",
    val chordMarkers: String = "",
    val instrument: String = "Guitar"
)

@Serializable
data class SearchSuggestions(
    val query: String = "",
    val suggestions: List<String> = emptyList()
)

@Serializable
data class HistoryEntry(
    val tabId: String = "",
    val lastViewed: Long = System.currentTimeMillis()
)

