package com.gbros.tabslite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gbros.tabslite.data.DesktopDatabase
import com.gbros.tabslite.data.Tab
import com.gbros.tabslite.utilities.BackendConnection
import com.gbros.tabslite.utilities.TabPdfExporter
import com.gbros.tabslite.utilities.TabTransposer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.vinceglb.filekit.compose.rememberFileSaverLauncher
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabViewerScreen(
    tabId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var tab by remember { mutableStateOf<Tab?>(null) }
    val settings by DesktopDatabase.instance.settingsFlow.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }

    var transpose by remember { mutableStateOf(0) }
    var fontSize by remember(settings.defaultFontSize) { mutableStateOf(settings.defaultFontSize) }
    var isTwoColumn by remember(settings.twoColumnDefault) { mutableStateOf(settings.twoColumnDefault) }
    var isAutoScrolling by remember { mutableStateOf(false) }
    var scrollSpeed by remember(settings.defaultScrollSpeed) { mutableStateOf(settings.defaultScrollSpeed) }

    var selectedChordDialog by remember { mutableStateOf<String?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Modern Native File Saver Dialog (Windows 11 Dark Common Item Dialog / macOS / Linux)
    val pdfSaverLauncher = rememberFileSaverLauncher { platformFile ->
        platformFile?.let { pFile ->
            tab?.let { currentTab ->
                try {
                    val exportFile = pFile.file
                    TabPdfExporter.exportToPdf(currentTab, transpose, exportFile)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Exported PDF to ${exportFile.name}")
                    }
                } catch (e: Exception) {
                    println("Error exporting PDF: ${e.message}")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Failed to export PDF: ${e.message}")
                    }
                }
            }
        }
    }

    // Fetch tab content
    LaunchedEffect(tabId) {
        isLoading = true
        try {
            // First check local database cache
            var loaded = DesktopDatabase.instance.getTab(tabId)
            if (loaded == null || loaded.content.isBlank() || loaded.strumming.isEmpty()) {
                try {
                    val fresh = BackendConnection.fetchTabContent(tabId)
                    if (fresh.content.isNotBlank()) {
                        loaded = fresh
                        DesktopDatabase.instance.saveTab(loaded)
                    }
                } catch (e: Exception) {
                    println("Could not refresh tab from network: ${e.message}")
                }
            }
            tab = loaded
            transpose = loaded?.transpose ?: 0
            isFavorite = DesktopDatabase.instance.isFavorite(tabId)
            if (loaded != null && loaded.content.isNotBlank()) {
                DesktopDatabase.instance.addToHistory(loaded)
            }

        } catch (e: Exception) {
            println("Error fetching tab: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Auto-scroll loop
    LaunchedEffect(isAutoScrolling, scrollSpeed) {
        if (isAutoScrolling) {
            while (isAutoScrolling) {
                delay(30L)
                val step = (scrollSpeed * 0.03f).coerceAtLeast(1f)
                if (scrollState.value + step.toInt() >= scrollState.maxValue) {
                    isAutoScrolling = false
                    break
                }
                scrollState.dispatchRawDelta(step)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = tab?.songName?.ifBlank { "Tab Viewer" } ?: "Loading...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (tab != null && tab!!.artistName.isNotBlank()) {
                            Text(
                                text = "${tab!!.artistName} • ${tab!!.type} (Ver ${tab!!.version})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Favorite button
                    IconButton(
                        onClick = {
                            tab?.let { currentTab ->
                                coroutineScope.launch {
                                    isFavorite = DesktopDatabase.instance.toggleFavorite(currentTab)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Add to playlist
                    IconButton(onClick = { showAddToPlaylistDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                    }

                    // Two column toggle
                    IconButton(onClick = { isTwoColumn = !isTwoColumn }) {
                        Icon(
                            imageVector = if (isTwoColumn) Icons.Default.ViewAgenda else Icons.Default.ViewWeek,
                            contentDescription = "Toggle Column View"
                        )
                    }

                    // Export to PDF
                    IconButton(
                        onClick = {
                            tab?.let { currentTab ->
                                val defaultBaseName = "${currentTab.songName} - ${currentTab.artistName}".replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                pdfSaverLauncher.launch(
                                    baseName = defaultBaseName,
                                    extension = "pdf"
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Tab as PDF")
                    }
                }
            )
        },
        bottomBar = {
            // Control toolbar for Transpose, Font Size, and Autoscroll
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Transpose controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Transpose: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        IconButton(
                            onClick = { transpose-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Transpose Down", modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = if (transpose > 0) "+$transpose" else "$transpose",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = { transpose++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Transpose Up", modifier = Modifier.size(18.dp))
                        }
                        if (transpose != 0) {
                            TextButton(onClick = { transpose = 0 }) {
                                Text("Reset", fontSize = 12.sp)
                            }
                        }
                    }

                    // Font Size controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Font: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        IconButton(
                            onClick = { if (fontSize > 10f) fontSize -= 1f },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "Smaller font", modifier = Modifier.size(18.dp))
                        }
                        Text("${fontSize.toInt()}pt", fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        IconButton(
                            onClick = { if (fontSize < 32f) fontSize += 1f },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "Larger font", modifier = Modifier.size(18.dp))
                        }
                    }

                    // Autoscroll controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { isAutoScrolling = !isAutoScrolling },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAutoScrolling) Color(0xFFDC2626) else Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isAutoScrolling) "Stop Scroll (Space)" else "Auto Scroll (Space)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isAutoScrolling) {
                            Spacer(Modifier.width(10.dp))
                            Text("Speed: ${scrollSpeed.toInt()}", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(6.dp))
                            Slider(
                                value = scrollSpeed,
                                onValueChange = { scrollSpeed = it },
                                valueRange = 5f..100f,
                                modifier = Modifier.width(130.dp),
                                thumb = {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(Color(0xFF3B82F6), CircleShape)
                                            .border(2.dp, Color.White, CircleShape)
                                    )
                                },
                                track = { _ ->
                                    val fraction = ((scrollSpeed - 5f) / (100f - 5f)).coerceIn(0.0001f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF334155))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction)
                                                .fillMaxHeight()
                                                .background(Color(0xFF3B82F6))
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (tab == null || tab!!.content.isBlank()) {
            Box(modifier = modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    text = "Could not load tab content. Please check your internet connection.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val currentContent = TabTransposer.transposeContent(tab!!.content, transpose)
            val distinctChords = remember(currentContent) { TabTransposer.extractChords(currentContent) }

            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(start = 24.dp, top = 16.dp, bottom = 16.dp, end = 12.dp)
                ) {
                    // Pinned Chords Bar
                    if (distinctChords.isNotEmpty()) {
                        val colors = AppColors
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.card,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chords in this song (click for Piano & Guitar patterns):",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = "${distinctChords.size} chords",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(distinctChords) { chord ->
                                        Surface(
                                            onClick = { selectedChordDialog = chord },
                                            shape = RoundedCornerShape(8.dp),
                                            color = colors.cardElevated,
                                            border = BorderStroke(1.dp, colors.border)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = chord,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = colors.accent
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Strumming Patterns Section
                    if (tab!!.strumming.isNotEmpty()) {
                        StrummingSection(
                            strummingPatterns = tab!!.strumming,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Render Tab Content
                    SelectionContainer {
                        if (isTwoColumn) {
                            // Split tab into two vertical columns for widescreen
                            val lines = currentContent.lines()
                            val half = (lines.size + 1) / 2
                            val col1 = lines.take(half).joinToString("\n")
                            val col2 = lines.drop(half).joinToString("\n")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    RenderTabMarkup(col1, fontSize) { chord ->
                                        if (settings.showChordPopups) selectedChordDialog = chord
                                    }
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    RenderTabMarkup(col2, fontSize) { chord ->
                                        if (settings.showChordPopups) selectedChordDialog = chord
                                    }
                                }
                            }
                        } else {
                            RenderTabMarkup(currentContent, fontSize) { chord ->
                                if (settings.showChordPopups) selectedChordDialog = chord
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scrollState),
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 4.dp, top = 2.dp, bottom = 2.dp),
                    style = defaultScrollbarStyle().copy(
                        unhoverColor = Color.White.copy(alpha = 0.18f),
                        hoverColor = Color(0xFF3B82F6),
                        thickness = 8.dp,
                        shape = RoundedCornerShape(4.dp)
                    )
                )
            }
        }
    }

    // Chord Inspector Dialog (Piano Keyboard & Guitar / Ukulele Fretboards)
    selectedChordDialog?.let { chord ->
        ChordInspectorDialog(
            chord = chord,
            onDismiss = { selectedChordDialog = null }
        )
    }

    // Add to Playlist Dialog
    if (showAddToPlaylistDialog) {
        val playlists by DesktopDatabase.instance.playlistsFlow.collectAsState()
        var isCreatingNew by remember { mutableStateOf(false) }
        var newPlaylistTitle by remember { mutableStateOf("") }
        var newPlaylistDesc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = {
            showAddToPlaylistDialog = false
            isCreatingNew = false
            newPlaylistTitle = ""
            newPlaylistDesc = ""
        }) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161822)),
                border = BorderStroke(1.dp, Color(0xFF282E40)),
                modifier = Modifier.padding(16.dp).width(380.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCreatingNew) "New Playlist" else "Add to Playlist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF1F5F9)
                        )
                        if (!isCreatingNew) {
                            TextButton(
                                onClick = { isCreatingNew = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF3B82F6))
                                Spacer(Modifier.width(4.dp))
                                Text("New", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF3B82F6))
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (isCreatingNew) {
                        // Create New Playlist Form
                        OutlinedTextField(
                            value = newPlaylistTitle,
                            onValueChange = { newPlaylistTitle = it },
                            placeholder = { Text("Playlist name *", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF101218),
                                unfocusedContainerColor = Color(0xFF101218),
                                focusedTextColor = Color(0xFFF1F5F9),
                                unfocusedTextColor = Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF222634)
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newPlaylistDesc,
                            onValueChange = { newPlaylistDesc = it },
                            placeholder = { Text("Description (optional)", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF101218),
                                unfocusedContainerColor = Color(0xFF101218),
                                focusedTextColor = Color(0xFFF1F5F9),
                                unfocusedTextColor = Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF222634)
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isCreatingNew = false }) {
                                Text("Back", color = Color(0xFF94A3B8))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPlaylistTitle.isNotBlank()) {
                                        tab?.let { currentTab ->
                                            coroutineScope.launch {
                                                val newId = DesktopDatabase.instance.createPlaylist(newPlaylistTitle.trim(), newPlaylistDesc.trim())
                                                DesktopDatabase.instance.addTabToPlaylist(newId, currentTab)
                                                showAddToPlaylistDialog = false
                                                isCreatingNew = false
                                                newPlaylistTitle = ""
                                                newPlaylistDesc = ""
                                            }
                                        }
                                    }
                                },
                                enabled = newPlaylistTitle.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Create & Add", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        // List of Existing Playlists
                        if (playlists.isEmpty()) {
                            Text(
                                "No playlists created yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                playlists.forEach { playlist ->
                                    val colors = AppColors
                                    Surface(
                                        onClick = {
                                            tab?.let { currentTab ->
                                                coroutineScope.launch {
                                                    DesktopDatabase.instance.addTabToPlaylist(playlist.id, currentTab)
                                                    showAddToPlaylistDialog = false
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.card,
                                        border = BorderStroke(1.dp, colors.border),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = playlist.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colors.textPrimary
                                                )
                                                if (playlist.description.isNotBlank()) {
                                                    Text(
                                                        text = playlist.description,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.textMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddToPlaylistDialog = false }) {
                                Text("Cancel", color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderTabMarkup(
    content: String,
    fontSize: Float,
    onChordClick: (String) -> Unit
) {
    val chordColor = Color(0xFF60A5FA)

    // Parse [ch]chord[/ch] and [tab] markup
    val annotated = remember(content, chordColor) {
        buildAnnotatedString {
            var index = 0
            val chordRegex = Regex("\\[ch\\](.*?)\\[/ch\\]|\\[tab\\]|\\[/tab\\]")
            val matches = chordRegex.findAll(content)

            for (match in matches) {
                if (match.range.first > index) {
                    append(content.substring(index, match.range.first))
                }
                if (match.value.startsWith("[ch]")) {
                    val chord = match.groupValues[1]
                    val start = length
                    append(chord)
                    addStyle(
                        SpanStyle(
                            color = chordColor,
                            fontWeight = FontWeight.Bold
                        ),
                        start,
                        length
                    )
                    addStringAnnotation(
                        tag = "CHORD",
                        annotation = chord,
                        start = start,
                        end = length
                    )
                }
                index = match.range.last + 1
            }
            if (index < content.length) {
                append(content.substring(index))
            }
        }
    }

    ClickableText(
        text = annotated,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.45f).sp,
            color = Color(0xFFF1F5F9)
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "CHORD", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onChordClick(annotation.item)
                }
        }
    )
}
