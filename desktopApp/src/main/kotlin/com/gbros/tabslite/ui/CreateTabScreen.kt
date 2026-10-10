package com.gbros.tabslite.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gbros.tabslite.data.DesktopDatabase
import com.gbros.tabslite.data.Tab
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTabScreen(
    onBack: () -> Unit,
    onTabSaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var songName by remember { mutableStateOf("") }
    var artistName by remember { mutableStateOf("") }
    var tuning by remember { mutableStateOf("E A D G B E") }
    var key by remember { mutableStateOf("C") }
    var capo by remember { mutableIntStateOf(0) }
    var content by remember {
        mutableStateOf(
            """
            [Intro]
            [ch]C[/ch]  [ch]G[/ch]  [ch]Am[/ch]  [ch]F[/ch]

            [Verse 1]
            [ch]C[/ch]              [ch]G[/ch]
            Here is where your chords and lyrics go
            [ch]Am[/ch]             [ch]F[/ch]
            Wrapped in [ch]chord tags[/ch] for easy transpose!
            """.trimIndent()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Custom Tab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (songName.isNotBlank() && artistName.isNotBlank()) {
                                val newId = "custom_" + UUID.randomUUID().toString().take(8)
                                val newTab = Tab(
                                    tabId = newId,
                                    songName = songName.trim(),
                                    artistName = artistName.trim(),
                                    tonalityName = key.trim(),
                                    tuning = tuning.trim(),
                                    capo = capo,
                                    type = "Chords",
                                    content = content,
                                    contributorUserName = "You"
                                )
                                coroutineScope.launch {
                                    DesktopDatabase.instance.saveTab(newTab)
                                    onTabSaved(newId)
                                }
                            }
                        },
                        enabled = songName.isNotBlank() && artistName.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Tab")
                    }
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = songName,
                        onValueChange = { songName = it },
                        label = { Text("Song Title *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = artistName,
                        onValueChange = { artistName = it },
                        label = { Text("Artist Name *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("Key / Tonality") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tuning,
                        onValueChange = { tuning = it },
                        label = { Text("Tuning") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text("Tab Content & Chords:", fontWeight = FontWeight.SemiBold)
                Text(
                    "Tip: Put [ch] and [/ch] around chord names (e.g. [ch]Am[/ch], [ch]G[/ch]) for auto-transpose & diagram support.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().height(450.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                )
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
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
