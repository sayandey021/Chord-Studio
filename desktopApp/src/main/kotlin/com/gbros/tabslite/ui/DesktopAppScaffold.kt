package com.gbros.tabslite.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gbros.tabslite.data.DesktopDatabase
import com.gbros.tabslite.data.Playlist
import com.gbros.tabslite.data.Tab
import com.gbros.tabslite.desktop.DesktopScreen
import com.gbros.tabslite.utilities.BackendConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class NavigationSection(val title: String, val icon: ImageVector, val group: String) {
    SEARCH("Search & Explore", Icons.Default.Search, "BROWSE"),
    LEARN("Learn Hub", Icons.Default.School, "LEARN"),
    FAVORITES("Favorites", Icons.Default.FavoriteBorder, "LIBRARY"),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.List, "LIBRARY"),
    HISTORY("Recently Played", Icons.Default.History, "LIBRARY"),
    AI_STUDIO("AI Audio to Tab", Icons.Default.AutoAwesome, "STUDIO"),
    TUNER("Chromatic Tuner", Icons.Default.GraphicEq, "TOOLS"),
    CREATE_TAB("Create Custom Tab", Icons.Default.Add, "CONTRIBUTE"),
    SETTINGS("Settings", Icons.Default.Settings, "PREFERENCES")
}

@Composable
fun DesktopAppScaffold(
    currentScreen: DesktopScreen = DesktopScreen.Home,
    selectedSection: NavigationSection,
    onSectionSelected: (NavigationSection) -> Unit,
    onSelectTab: (String) -> Unit,
    onSelectPlaylist: (Int) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val favorites by DesktopDatabase.instance.favoritesFlow.collectAsState()
    val playlists by DesktopDatabase.instance.playlistsFlow.collectAsState()
    val history by DesktopDatabase.instance.historyFlow.collectAsState()

    var isSidebarCollapsed by remember { mutableStateOf(false) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarCollapsed) 68.dp else 250.dp,
        animationSpec = tween(180)
    )

    val searchState = remember { SearchState() }
    val aiStudioState = remember { AiAudioStudioState() }

    val colors = AppColors

    Row(modifier = modifier.fillMaxSize().background(colors.bg)) {
        // Persistent Professional Sidebar
        Surface(
            modifier = Modifier.width(sidebarWidth).fillMaxHeight(),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isSidebarCollapsed) 8.dp else 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Header with Branding & Collapse Toggle
                    if (!isSidebarCollapsed) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Image(
                                    painter = painterResource("app_icon_72.png"),
                                    contentDescription = "Chord Studio Logo",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Chord Studio",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        letterSpacing = 0.3.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Guitar & Chord Sheets",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isSidebarCollapsed = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                                    contentDescription = "Collapse sidebar",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Surface(
                                onClick = { isSidebarCollapsed = false },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Image(
                                    painter = painterResource("app_icon_72.png"),
                                    contentDescription = "Expand sidebar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(if (isSidebarCollapsed) 10.dp else 18.dp))

                    // Grouped Navigation Items
                    var currentGroup = ""
                    NavigationSection.entries.filter { it != NavigationSection.SETTINGS }.forEach { section ->
                        if (!isSidebarCollapsed && section.group != currentGroup) {
                            currentGroup = section.group
                            Text(
                                text = currentGroup,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 10.dp, top = 12.dp, bottom = 4.dp)
                            )
                        }

                        val isSelected = selectedSection == section
                        val badgeCount = when (section) {
                            NavigationSection.FAVORITES -> favorites.size
                            NavigationSection.PLAYLISTS -> playlists.size
                            NavigationSection.HISTORY -> history.size
                            else -> 0
                        }

                        SidebarNavItem(
                            title = section.title,
                            icon = section.icon,
                            badgeCount = badgeCount,
                            isSelected = isSelected,
                            isCollapsed = isSidebarCollapsed,
                            onClick = { onSectionSelected(section) }
                        )
                    }
                }

                // Bottom Settings Section
                SidebarNavItem(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    badgeCount = 0,
                    isSelected = selectedSection == NavigationSection.SETTINGS,
                    isCollapsed = isSidebarCollapsed,
                    onClick = { onSectionSelected(NavigationSection.SETTINGS) }
                )
            }
        }

        // Main Content Area
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(colors.bg)) {
            when (currentScreen) {
                is DesktopScreen.Home -> {
                    when (selectedSection) {
                        NavigationSection.SEARCH -> SearchExploreView(searchState = searchState, onSelectTab = onSelectTab)
                        NavigationSection.LEARN -> LearnHubScreen()
                        NavigationSection.FAVORITES -> FavoritesView(onSelectTab = onSelectTab)
                        NavigationSection.PLAYLISTS -> PlaylistsView(onSelectPlaylist = onSelectPlaylist)
                        NavigationSection.HISTORY -> HistoryView(onSelectTab = onSelectTab)
                        NavigationSection.TUNER -> TunerScreen()
                        NavigationSection.AI_STUDIO -> AiAudioTabScreen(onOpenInTabViewer = onSelectTab, state = aiStudioState)
                        NavigationSection.CREATE_TAB -> {
                            CreateTabScreen(
                                onBack = { onSectionSelected(NavigationSection.SEARCH) },
                                onTabSaved = { tabId -> onSelectTab(tabId) }
                            )
                        }
                        NavigationSection.SETTINGS -> SettingsView()
                    }
                }
                is DesktopScreen.TabViewer -> {
                    TabViewerScreen(
                        tabId = currentScreen.tabId,
                        onBack = onNavigateBack
                    )
                }
                is DesktopScreen.PlaylistDetail -> {
                    PlaylistDetailScreen(
                        playlistId = currentScreen.playlistId,
                        onBack = onNavigateBack,
                        onSelectTab = onSelectTab
                    )
                }
            }
        }
    }
}

@Composable
fun SidebarNavItem(
    title: String,
    icon: ImageVector,
    badgeCount: Int,
    isSelected: Boolean,
    isCollapsed: Boolean = false,
    onClick: () -> Unit
) {
    val colors = AppColors
    val backgroundColor = if (isSelected) colors.cardElevated else Color.Transparent
    val contentColor = if (isSelected) colors.textPrimary else colors.textSecondary
    val iconTint = if (isSelected) colors.accent else colors.textMuted

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) BorderStroke(1.dp, colors.borderHover) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(vertical = 1.dp)
    ) {
        if (isCollapsed) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 6.dp)
                            .size(6.dp)
                            .background(colors.accent, CircleShape)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 16.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        ),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = contentColor,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                }

                if (badgeCount > 0) {
                    Surface(
                        color = if (isSelected) colors.card else colors.surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text(
                                text = badgeCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    lineHeight = 12.sp,
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    )
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) colors.accent else colors.textSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.offset(y = (-0.5).dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class PopularTabEntry(
    val song: String,
    val artist: String,
    val type: String,
    val diff: String,
    val rating: Double,
    val key: String
)

data class GenreCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val curatedTabs: List<PopularTabEntry>
)

val EXPLORE_CATEGORIES = listOf(
    GenreCategory(
        title = "Rock Classics",
        subtitle = "Eagles, Queen, Pink Floyd",
        icon = Icons.Default.GraphicEq,
        accentColor = Color(0xFFEF4444),
        curatedTabs = listOf(
            PopularTabEntry("Hotel California", "Eagles", "Chords", "Intermediate", 4.9, "Bm"),
            PopularTabEntry("Bohemian Rhapsody", "Queen", "Chords", "Intermediate", 4.9, "Bb"),
            PopularTabEntry("Wish You Were Here", "Pink Floyd", "Chords", "Novice", 4.9, "G"),
            PopularTabEntry("Sweet Child O' Mine", "Guns N' Roses", "Chords", "Intermediate", 4.8, "D"),
            PopularTabEntry("Comfortably Numb", "Pink Floyd", "Chords", "Intermediate", 4.9, "Bm"),
            PopularTabEntry("Don't Stop Believin'", "Journey", "Chords", "Novice", 4.8, "E"),
            PopularTabEntry("Smoke on the Water", "Deep Purple", "Tabs", "Novice", 4.8, "G"),
            PopularTabEntry("Back in Black", "AC/DC", "Tabs", "Intermediate", 4.9, "E")
        )
    ),
    GenreCategory(
        title = "Acoustic & Folk",
        subtitle = "Oasis, Vance Joy, Denver",
        icon = Icons.Default.MusicNote,
        accentColor = Color(0xFFF59E0B),
        curatedTabs = listOf(
            PopularTabEntry("Wonderwall", "Oasis", "Chords", "Novice", 4.9, "Em"),
            PopularTabEntry("Take Me Home, Country Roads", "John Denver", "Chords", "Novice", 4.9, "G"),
            PopularTabEntry("Fast Car", "Tracy Chapman", "Chords", "Novice", 4.9, "C"),
            PopularTabEntry("Blowin' in the Wind", "Bob Dylan", "Chords", "Novice", 4.8, "D"),
            PopularTabEntry("The Sound of Silence", "Simon & Garfunkel", "Chords", "Novice", 4.9, "Am"),
            PopularTabEntry("Heart of Gold", "Neil Young", "Chords", "Novice", 4.8, "Em"),
            PopularTabEntry("A Horse with No Name", "America", "Chords", "Novice", 4.8, "Em")
        )
    ),
    GenreCategory(
        title = "Pop & Modern",
        subtitle = "Ed Sheeran, Swift, Adele",
        icon = Icons.Default.Radio,
        accentColor = Color(0xFF8B5CF6),
        curatedTabs = listOf(
            PopularTabEntry("Perfect", "Ed Sheeran", "Chords", "Novice", 4.9, "G"),
            PopularTabEntry("Someone Like You", "Adele", "Chords", "Novice", 4.9, "A"),
            PopularTabEntry("Blank Space", "Taylor Swift", "Chords", "Novice", 4.8, "F"),
            PopularTabEntry("Shallow", "Lady Gaga", "Chords", "Novice", 4.9, "G"),
            PopularTabEntry("All of Me", "John Legend", "Chords", "Novice", 4.9, "Em"),
            PopularTabEntry("Stay With Me", "Sam Smith", "Chords", "Novice", 4.8, "Am"),
            PopularTabEntry("Counting Stars", "OneRepublic", "Chords", "Novice", 4.8, "Am")
        )
    ),
    GenreCategory(
        title = "Metal & Solos",
        subtitle = "Metallica, Maiden, Avenged",
        icon = Icons.Default.Bolt,
        accentColor = Color(0xFFDC2626),
        curatedTabs = listOf(
            PopularTabEntry("Nothing Else Matters", "Metallica", "Tabs", "Intermediate", 4.9, "Em"),
            PopularTabEntry("The Trooper", "Iron Maiden", "Tabs", "Advanced", 4.9, "Em"),
            PopularTabEntry("Hail to the King", "Avenged Sevenfold", "Tabs", "Intermediate", 4.8, "Dm"),
            PopularTabEntry("Fade to Black", "Metallica", "Tabs", "Advanced", 4.9, "Bm"),
            PopularTabEntry("Paranoid", "Black Sabbath", "Tabs", "Novice", 4.9, "Em"),
            PopularTabEntry("Master of Puppets", "Metallica", "Tabs", "Advanced", 4.9, "Em"),
            PopularTabEntry("Crazy Train", "Ozzy Osbourne", "Tabs", "Advanced", 4.9, "F#m")
        )
    ),
    GenreCategory(
        title = "Ukulele Essentials",
        subtitle = "Easy strumming arrangements",
        icon = Icons.AutoMirrored.Filled.QueueMusic,
        accentColor = Color(0xFF06B6D4),
        curatedTabs = listOf(
            PopularTabEntry("Riptide", "Vance Joy", "Ukulele", "Novice", 4.8, "Am"),
            PopularTabEntry("Somewhere Over the Rainbow", "Israel K", "Ukulele", "Novice", 4.9, "C"),
            PopularTabEntry("I'm Yours", "Jason Mraz", "Ukulele", "Novice", 4.8, "B"),
            PopularTabEntry("Hey Soul Sister", "Train", "Ukulele", "Novice", 4.8, "E"),
            PopularTabEntry("Count on Me", "Bruno Mars", "Ukulele", "Novice", 4.8, "C"),
            PopularTabEntry("Can't Help Falling in Love", "Elvis Presley", "Ukulele", "Novice", 4.9, "C")
        )
    ),
    GenreCategory(
        title = "Blues & Jazz",
        subtitle = "B.B. King, Clapton, SRV",
        icon = Icons.Default.Nightlife,
        accentColor = Color(0xFF3B82F6),
        curatedTabs = listOf(
            PopularTabEntry("The Thrill Is Gone", "B.B. King", "Chords", "Intermediate", 4.9, "Bm"),
            PopularTabEntry("Layla (Acoustic)", "Eric Clapton", "Chords", "Intermediate", 4.9, "Dm"),
            PopularTabEntry("Pride and Joy", "Stevie Ray Vaughan", "Tabs", "Advanced", 4.9, "Eb"),
            PopularTabEntry("Autumn Leaves", "Jazz Standard", "Chords", "Intermediate", 4.8, "Em"),
            PopularTabEntry("Fly Me to the Moon", "Frank Sinatra", "Chords", "Intermediate", 4.9, "Am"),
            PopularTabEntry("Cross Road Blues", "Robert Johnson", "Tabs", "Intermediate", 4.8, "A")
        )
    ),
    GenreCategory(
        title = "Beginner Essentials",
        subtitle = "Open chords & strumming",
        icon = Icons.Default.School,
        accentColor = Color(0xFF10B981),
        curatedTabs = listOf(
            PopularTabEntry("Let It Be", "The Beatles", "Chords", "Novice", 4.8, "C"),
            PopularTabEntry("Knockin' on Heaven's Door", "Bob Dylan", "Chords", "Novice", 4.9, "G"),
            PopularTabEntry("Stand By Me", "Ben E. King", "Chords", "Novice", 4.9, "A"),
            PopularTabEntry("Brown Eyed Girl", "Van Morrison", "Chords", "Novice", 4.8, "G"),
            PopularTabEntry("Have You Ever Seen the Rain", "CCR", "Chords", "Novice", 4.8, "C"),
            PopularTabEntry("Zombie", "The Cranberries", "Chords", "Novice", 4.9, "Em")
        )
    ),
    GenreCategory(
        title = "Fingerstyle",
        subtitle = "Tabs & solo acoustic patterns",
        icon = Icons.Default.Tune,
        accentColor = Color(0xFFD97706),
        curatedTabs = listOf(
            PopularTabEntry("Stairway to Heaven", "Led Zeppelin", "Tabs", "Advanced", 4.9, "Am"),
            PopularTabEntry("Dust in the Wind", "Kansas", "Tabs", "Intermediate", 4.9, "C"),
            PopularTabEntry("Tears in Heaven", "Eric Clapton", "Chords", "Intermediate", 4.9, "A"),
            PopularTabEntry("Blackbird", "The Beatles", "Tabs", "Intermediate", 4.9, "G"),
            PopularTabEntry("Classical Gas", "Mason Williams", "Tabs", "Advanced", 4.9, "Am"),
            PopularTabEntry("Shape of My Heart", "Sting", "Tabs", "Intermediate", 4.9, "F#m")
        )
    )
)

val QUICK_SEARCH_TAGS = listOf(
    "Hotel California",
    "Wonderwall",
    "Bohemian Rhapsody",
    "Let It Be",
    "Riptide",
    "Nothing Else Matters",
    "Autumn Leaves",
    "Hallelujah",
    "Wish You Were Here"
)

val POPULAR_JAM_TABS = listOf(
    PopularTabEntry("Wonderwall", "Oasis", "Chords", "Easy", 4.9, "Em"),
    PopularTabEntry("Hotel California", "Eagles", "Chords", "Intermediate", 4.9, "Bm"),
    PopularTabEntry("Nothing Else Matters", "Metallica", "Tabs", "Advanced", 4.9, "Em"),
    PopularTabEntry("Let It Be", "The Beatles", "Chords", "Novice", 4.8, "C"),
    PopularTabEntry("Hallelujah", "Leonard Cohen", "Chords", "Easy", 4.9, "C"),
    PopularTabEntry("Riptide", "Vance Joy", "Ukulele", "Novice", 4.8, "Am"),
    PopularTabEntry("Perfect", "Ed Sheeran", "Chords", "Easy", 4.9, "Ab"),
    PopularTabEntry("Stairway to Heaven", "Led Zeppelin", "Tabs", "Advanced", 4.9, "Am")
)

class SearchState {
    var searchQuery by mutableStateOf("")
    var searchSuggestions by mutableStateOf<List<String>>(emptyList())
    var searchResults by mutableStateOf<List<Tab>>(emptyList())
    var isSearching by mutableStateOf(false)
    var selectedFilterType by mutableStateOf("All")
    var selectedCategory by mutableStateOf<GenreCategory?>(null)
}

@Composable
fun SearchExploreView(
    searchState: SearchState = remember { SearchState() },
    onSelectTab: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        searchState.searchQuery = query
        searchState.searchSuggestions = emptyList()
        searchState.selectedCategory = null
        searchState.isSearching = true
        coroutineScope.launch {
            try {
                searchState.searchResults = BackendConnection.searchTabs(query)
            } finally {
                searchState.isSearching = false
            }
        }
    }

    LaunchedEffect(searchState.searchQuery) {
        if (searchState.searchQuery.length >= 2 && searchState.searchResults.isEmpty()) {
            searchJob?.cancel()
            searchJob = launch {
                delay(200L)
                searchState.searchSuggestions = BackendConnection.getSearchSuggestions(searchState.searchQuery)
            }
        } else {
            searchState.searchSuggestions = emptyList()
        }
    }

    val filteredResults = remember(searchState.searchResults, searchState.selectedFilterType) {
        if (searchState.selectedFilterType == "All") searchState.searchResults
        else searchState.searchResults.filter { it.type.contains(searchState.selectedFilterType, ignoreCase = true) }
    }

    val categories = EXPLORE_CATEGORIES
    val quickSearches = QUICK_SEARCH_TAGS

    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Clean Studio Header & Search Bar (Pinned at top, doesn't scroll)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(top = 24.dp, bottom = 12.dp)
        ) {
            Text(
                text = "Search & Explore",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary
            )
            Text(
                text = "Find tabs, chords, and lyrics across guitar, ukulele, and bass",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorTextMuted
            )

            Spacer(Modifier.height(16.dp))

            // Unified Modern Studio Search Input
            BasicTextField(
                value = searchState.searchQuery,
                onValueChange = {
                    searchState.searchQuery = it
                    if (it.isBlank()) searchState.searchResults = emptyList()
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = ColorTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(ColorAccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                            performSearch(searchState.searchQuery)
                            true
                        } else false
                    },
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(ColorSurfaceCard, RoundedCornerShape(10.dp))
                            .border(1.dp, ColorCardBorder, RoundedCornerShape(10.dp))
                            .padding(start = 14.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = ColorTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchState.searchQuery.isEmpty()) {
                                Text(
                                    "Search songs, artists, chords, or tabs...",
                                    color = ColorTextMuted,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                        if (searchState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchState.searchQuery = ""
                                    searchState.searchResults = emptyList()
                                    searchState.searchSuggestions = emptyList()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = ColorTextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        Surface(
                            color = ColorSurfaceDark,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, ColorCardBorder),
                            onClick = { performSearch(searchState.searchQuery) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Search",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ColorAccentBlue
                                    )
                                )
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = ColorAccentBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            // Quick Search Tags (Wrapping FlowRow, No Sideways Scroll)
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Trending:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextMuted,
                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp)
                )
                quickSearches.forEach { pick ->
                    Surface(
                        onClick = { performSearch(pick) },
                        shape = RoundedCornerShape(6.dp),
                        color = ColorSurfaceCard,
                        border = BorderStroke(1.dp, ColorCardBorder)
                    ) {
                        Text(
                            text = pick,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            color = ColorTextSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Scrollable Body (Starts AFTER title header, with non-overlapping scrollbar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 32.dp, end = 20.dp, bottom = 16.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

        // Search Suggestions Overlay
        if (searchState.searchSuggestions.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                    border = BorderStroke(1.dp, ColorCardBorder)
                ) {
                    Column {
                        searchState.searchSuggestions.take(6).forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { performSearch(suggestion) }
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = ColorTextMuted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    suggestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ColorTextPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Search Results vs Browse Hub
        if (searchState.isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = ColorAccentBlue,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Searching catalog...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTextSecondary
                        )
                    }
                }
            }
        } else if (searchState.searchResults.isNotEmpty()) {
            // Results Bar & Filter Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Results for \"${searchState.searchQuery}\" (${filteredResults.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "Chords", "Tabs", "Ukulele", "Bass").forEach { filter ->
                            val isSelected = searchState.selectedFilterType == filter
                            Surface(
                                onClick = { searchState.selectedFilterType = filter },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ColorAccentBlue else ColorSurfaceCardElevated,
                                border = BorderStroke(1.dp, if (isSelected) ColorAccentBlue else ColorCardBorder)
                            ) {
                                Text(
                                    text = filter,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else ColorTextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Results List
            items(filteredResults) { tab ->
                TabListItem(
                    tab = tab,
                    onClick = { onSelectTab(tab.tabId) }
                )
            }
        } else if (searchState.selectedCategory != null) {
            // CATEGORY DETAIL VIEW
            val cat = searchState.selectedCategory!!
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Back button row
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { searchState.selectedCategory = null }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ColorAccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Back to All Categories",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorAccentBlue
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Hero Banner Card for Selected Category
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorSurfaceDark),
                        border = BorderStroke(1.dp, ColorCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ColorSurfaceCardElevated)
                                        .border(1.dp, ColorCardBorder, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = cat.accentColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorTextPrimary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = cat.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ColorTextSecondary
                                    )
                                }
                            }

                            Surface(
                                color = ColorSurfaceCardElevated,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, ColorCardBorder)
                            ) {
                                Text(
                                    text = "${cat.curatedTabs.size} Curated Tracks",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorAccentBlue,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Curated Song Catalog",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                }
            }

            // List of songs in the category
            items(cat.curatedTabs) { entry ->
                CuratedTabItem(
                    entry = entry,
                    onClick = { performSearch("${entry.song} ${entry.artist}") }
                )
            }
        } else {
            // BROWSE HUB (Idle State)

            // 1. Curated Categories (Professional Studio Grid)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Curated Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                        Text(
                            text = "Click to browse collection",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorTextMuted
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    val row1 = categories.take(4)
                    val row2 = categories.drop(4)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row1.forEach { cat ->
                                CategoryCard(
                                    category = cat,
                                    onClick = { searchState.selectedCategory = cat },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row2.forEach { cat ->
                                CategoryCard(
                                    category = cat,
                                    onClick = { searchState.selectedCategory = cat },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Popular Jamming Essentials (Clean 4-Column Grid, No Sideways Scroll)
            item {
                Column {
                    Text(
                        text = "Popular Jamming Tabs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    Spacer(Modifier.height(12.dp))

                    val popRow1 = POPULAR_JAM_TABS.take(4)
                    val popRow2 = POPULAR_JAM_TABS.drop(4)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            popRow1.forEach { item ->
                                PopularTabCard(
                                    item = item,
                                    onClick = { performSearch(item.song) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            popRow2.forEach { item ->
                                PopularTabCard(
                                    item = item,
                                    onClick = { performSearch(item.song) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            style = defaultScrollbarStyle().copy(
                unhoverColor = Color.White.copy(alpha = 0.18f),
                hoverColor = ColorAccentBlue,
                thickness = 8.dp,
                shape = RoundedCornerShape(4.dp)
            )
        )
    }
}
}

@Composable
fun PopularTabCard(
    item: PopularTabEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
        border = BorderStroke(1.dp, ColorCardBorder),
        modifier = modifier.height(104.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = item.song,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    color = ColorTextPrimary,
                    maxLines = 1
                )
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorTextSecondary,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ColorSurfaceCardElevated,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, ColorCardBorder),
                    modifier = Modifier.height(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                        Text(
                            text = item.type,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorAccentBlue
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = item.rating.toString(),
                        fontSize = 11.5.sp,
                        lineHeight = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: GenreCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
        border = BorderStroke(1.dp, ColorCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ColorSurfaceCardElevated)
                        .border(1.dp, ColorCardBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = category.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = ColorTextPrimary,
                        maxLines = 1,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = category.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTextSecondary,
                        maxLines = 1,
                        fontSize = 11.5.sp
                    )
                }
            }

            Surface(
                color = ColorSurfaceDark,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, ColorCardBorder),
                modifier = Modifier.height(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        text = "${category.curatedTabs.size} tabs",
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun CuratedTabItem(
    entry: PopularTabEntry,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
        border = BorderStroke(1.dp, ColorCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ColorSurfaceCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = ColorAccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = entry.song,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorTextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = entry.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Key Badge
                Surface(
                    color = ColorSurfaceDark,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, ColorCardBorder),
                    modifier = Modifier.height(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = "Key: ${entry.key}",
                            fontSize = 11.5.sp,
                            lineHeight = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorTextSecondary
                        )
                    }
                }

                // Difficulty Badge
                val (diffBg, diffColor) = when (entry.diff.lowercase()) {
                    "novice", "easy" -> Pair(Color(0xFF064E3B).copy(alpha = 0.5f), Color(0xFF34D399))
                    "intermediate" -> Pair(Color(0xFF78350F).copy(alpha = 0.5f), Color(0xFFFBBF24))
                    else -> Pair(Color(0xFF7F1D1D).copy(alpha = 0.5f), Color(0xFFF87171))
                }
                Surface(
                    color = diffBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, diffColor.copy(alpha = 0.3f)),
                    modifier = Modifier.height(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = entry.diff,
                            fontSize = 11.5.sp,
                            lineHeight = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = diffColor
                        )
                    }
                }

                // Type Badge
                Surface(
                    color = ColorSurfaceCardElevated,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, ColorCardBorder),
                    modifier = Modifier.height(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = entry.type,
                            fontSize = 11.5.sp,
                            lineHeight = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorAccentBlue
                        )
                    }
                }

                // Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = entry.rating.toString(),
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Find Tabs Button
                Surface(
                    onClick = onClick,
                    color = ColorAccentBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ColorAccentBlue.copy(alpha = 0.4f)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 11.dp)
                    ) {
                        Text(
                            text = "Find Tab",
                            fontSize = 12.5.sp,
                            lineHeight = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorAccentBlue
                        )
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = ColorAccentBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesView(onSelectTab: (String) -> Unit) {
    val favorites by DesktopDatabase.instance.favoritesFlow.collectAsState()
    var filterQuery by remember { mutableStateOf("") }

    val filtered = remember(favorites, filterQuery) {
        if (filterQuery.isBlank()) favorites
        else favorites.filter { it.songName.contains(filterQuery, ignoreCase = true) || it.artistName.contains(filterQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Pinned Header (Does not scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Favorites", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                Text("Saved chord sheets and tablatures for offline playback", style = MaterialTheme.typography.bodyMedium, color = ColorTextMuted)
            }
            if (favorites.isNotEmpty()) {
                BasicTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = ColorTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(ColorAccentBlue),
                    modifier = Modifier.width(260.dp),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .background(ColorSurfaceDark, RoundedCornerShape(8.dp))
                                .border(1.dp, ColorCardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = ColorTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (filterQuery.isEmpty()) {
                                    Text(
                                        text = "Filter favorites...",
                                        color = ColorTextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                            if (filterQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { filterQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = ColorTextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = ColorSurfaceCard,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ColorCardBorder),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(32.dp), tint = ColorTextMuted)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("No Favorites Saved", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ColorTextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("Search for songs and click the heart icon to save them for offline access.", style = MaterialTheme.typography.bodySmall, color = ColorTextMuted)
                }
            }
        } else {
            val listState = rememberLazyListState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 32.dp, end = 20.dp, bottom = 16.dp)
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 12.dp)
                ) {
                    items(filtered) { tab ->
                        TabListItem(tab = tab, onClick = { onSelectTab(tab.tabId) })
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    style = defaultScrollbarStyle().copy(
                        unhoverColor = Color.White.copy(alpha = 0.18f),
                        hoverColor = ColorAccentBlue,
                        thickness = 8.dp,
                        shape = RoundedCornerShape(4.dp)
                    )
                )
            }
        }
    }
}

@Composable
fun PlaylistsView(onSelectPlaylist: (Int) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val playlists by DesktopDatabase.instance.playlistsFlow.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Pinned Header (Does not scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Playlists & Setlists", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                Text("Organize song lists for rehearsals and jam sessions", style = MaterialTheme.typography.bodyMedium, color = ColorTextMuted)
            }
            Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorAccentBlue)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Playlist", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        val listState = rememberLazyListState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 32.dp, end = 20.dp, bottom = 16.dp)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 12.dp)
            ) {
                items(playlists) { playlist ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectPlaylist(playlist.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
                        border = BorderStroke(1.dp, ColorCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = ColorSurfaceCardElevated,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ColorCardBorder),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = ColorAccentBlue, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(playlist.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ColorTextPrimary)
                                    if (playlist.description.isNotBlank()) {
                                        Text(playlist.description, style = MaterialTheme.typography.bodySmall, color = ColorTextMuted)
                                    }
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = ColorTextMuted)
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                style = defaultScrollbarStyle().copy(
                    unhoverColor = Color.White.copy(alpha = 0.18f),
                    hoverColor = ColorAccentBlue,
                    thickness = 8.dp,
                    shape = RoundedCornerShape(4.dp)
                )
            )
        }
    }

    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ColorSurfaceCardElevated),
                border = BorderStroke(1.dp, ColorCardBorder),
                modifier = Modifier.padding(16.dp).width(380.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text("Create Playlist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Playlist Name *") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ColorSurfaceDark,
                            unfocusedContainerColor = ColorSurfaceDark,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedBorderColor = ColorAccentBlue,
                            unfocusedBorderColor = ColorCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ColorSurfaceDark,
                            unfocusedContainerColor = ColorSurfaceDark,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedBorderColor = ColorAccentBlue,
                            unfocusedBorderColor = ColorCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = ColorTextSecondary) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank()) {
                                    coroutineScope.launch {
                                        DesktopDatabase.instance.createPlaylist(newTitle.trim(), newDesc.trim())
                                        newTitle = ""
                                        newDesc = ""
                                        showCreateDialog = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorAccentBlue),
                            enabled = newTitle.isNotBlank()
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryView(onSelectTab: (String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val history by DesktopDatabase.instance.historyFlow.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Pinned Header (Does not scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Recently Played", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                Text("Quickly resume songs you've practiced recently", style = MaterialTheme.typography.bodyMedium, color = ColorTextMuted)
            }
            if (history.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            DesktopDatabase.instance.clearHistory()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear", fontSize = 13.sp)
                }
            }
        }

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = ColorSurfaceCard,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ColorCardBorder),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(32.dp), tint = ColorTextMuted)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("No Playback History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ColorTextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("Tabs you open and play will automatically appear here.", style = MaterialTheme.typography.bodySmall, color = ColorTextMuted)
                }
            }
        } else {
            val listState = rememberLazyListState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 32.dp, end = 20.dp, bottom = 16.dp)
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 12.dp)
                ) {
                    items(history) { tab ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TabListItem(tab = tab, onClick = { onSelectTab(tab.tabId) })
                            }
                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        DesktopDatabase.instance.removeFromHistory(tab.tabId)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = ColorSurfaceCard,
                                border = BorderStroke(1.dp, ColorCardBorder),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = ColorTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    style = defaultScrollbarStyle().copy(
                        unhoverColor = Color.White.copy(alpha = 0.18f),
                        hoverColor = ColorAccentBlue,
                        thickness = 8.dp,
                        shape = RoundedCornerShape(4.dp)
                    )
                )
            }
        }
    }
}

@Composable
fun TabListItem(
    tab: Tab,
    onClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isFav by remember { mutableStateOf(false) }

    LaunchedEffect(tab.tabId) {
        isFav = DesktopDatabase.instance.isFavorite(tab.tabId)
    }

    val typeColor = when (tab.type.lowercase()) {
        "chords" -> ColorAccentBlue
        "tab", "tabs" -> Color(0xFF10B981)
        "ukulele" -> Color(0xFFF59E0B)
        "bass" -> Color(0xFF8B5CF6)
        else -> ColorAccentBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceCard),
        border = BorderStroke(1.dp, ColorCardBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Subtle Left Accent Indicator
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(typeColor)
                )

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        text = tab.songName.ifBlank { "Untitled Song" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTextPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tab.artistName.ifBlank { "Unknown Artist" },
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTextSecondary
                        )
                        Text(" • ", color = ColorTextMuted)
                        Surface(
                            color = typeColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, typeColor.copy(alpha = 0.25f)),
                            modifier = Modifier.height(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                                Text(
                                    text = tab.type.uppercase() + if (tab.version > 1) " v${tab.version}" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = typeColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.5.sp,
                                    lineHeight = 10.5.sp
                                )
                            }
                        }
                        if (tab.tonalityName.isNotBlank()) {
                            Text(" • ", color = ColorTextMuted)
                            Text(
                                text = "Key: ${tab.tonalityName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTextMuted
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tab.rating > 0.0) {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "%.1f".format(tab.rating),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            lineHeight = 12.sp
                        )
                    }
                }

                val diffColor = when (tab.difficulty.lowercase()) {
                    "novice", "absolute novice", "easy" -> Color(0xFF10B981)
                    "intermediate" -> Color(0xFFF59E0B)
                    "advanced", "hard" -> Color(0xFFEF4444)
                    else -> ColorTextMuted
                }

                Surface(
                    color = diffColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, diffColor.copy(alpha = 0.25f)),
                    modifier = Modifier.height(24.dp).padding(end = 8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = tab.difficulty.ifBlank { "Chords" }.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = diffColor,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Favorite Toggle
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            isFav = DesktopDatabase.instance.toggleFavorite(tab)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) Color(0xFFEF4444) else ColorTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Open Button
                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(6.dp),
                    color = ColorSurfaceCardElevated,
                    border = BorderStroke(1.dp, ColorCardBorder),
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Open",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            lineHeight = 12.5.sp,
                            color = ColorAccentBlue
                        )
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = ColorAccentBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

