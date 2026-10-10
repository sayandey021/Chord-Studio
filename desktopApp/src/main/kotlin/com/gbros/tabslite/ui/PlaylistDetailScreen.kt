package com.gbros.tabslite.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gbros.tabslite.data.DesktopDatabase
import com.gbros.tabslite.data.Playlist
import com.gbros.tabslite.data.Tab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    onBack: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val playlists by DesktopDatabase.instance.playlistsFlow.collectAsState()
    val currentPlaylist = remember(playlists, playlistId) {
        playlists.find { it.id == playlistId } ?: Playlist(id = playlistId, title = "Playlist")
    }

    var tabs by remember { mutableStateOf<List<Tab>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshTabs() {
        coroutineScope.launch {
            isLoading = true
            tabs = DesktopDatabase.instance.getPlaylistTabs(playlistId)
            isLoading = false
        }
    }

    LaunchedEffect(playlistId) {
        refreshTabs()
    }

    val colors = AppColors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentPlaylist.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${tabs.size} ${if (tabs.size == 1) "song" else "songs"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.bg,
                    titleContentColor = colors.textPrimary,
                    navigationIconContentColor = colors.textPrimary
                )
            )
        },
        containerColor = colors.bg
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = colors.accent,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (tabs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No songs in this playlist yet. Add songs from the Tab viewer!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tabs) { tab ->
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
                                        DesktopDatabase.instance.removeTabFromPlaylist(playlistId, tab.tabId)
                                        refreshTabs()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = colors.cardElevated,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove from playlist",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
