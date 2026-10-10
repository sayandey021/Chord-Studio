package com.gbros.tabslite.ui

import androidx.compose.animation.*
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gbros.tabslite.data.AppSettings
import com.gbros.tabslite.data.DesktopDatabase
import com.gbros.tabslite.utilities.AudioDeviceManager
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsTab(val title: String, val icon: ImageVector) {
    APPEARANCE("Appearance & Theme", Icons.Default.Palette),
    TAB_VIEWER("Tab Viewer & Playing", Icons.AutoMirrored.Filled.MenuBook),
    AUDIO_AI("AI Studio & Audio", Icons.Default.GraphicEq),
    DATA_BACKUP("Data & Storage", Icons.Default.Storage),
    ABOUT("About & Info", Icons.Default.Info)
}

data class ThemePreset(
    val id: String,
    val name: String,
    val description: String,
    val bgPreview: Color,
    val cardPreview: Color,
    val accentPreview: Color
)

data class AccentOption(
    val id: String,
    val name: String,
    val color: Color
)

val THEME_PRESETS = listOf(
    ThemePreset(
        id = "OBSIDIAN_DARK",
        name = "Obsidian Dark",
        description = "Deep slate with modern neon blue highlights (Default)",
        bgPreview = Color(0xFF0C0D12),
        cardPreview = Color(0xFF151821),
        accentPreview = Color(0xFF3B82F6)
    ),
    ThemePreset(
        id = "MIDNIGHT_BLUE",
        name = "Midnight Blue",
        description = "Rich naval blue tones with sapphire luminescence",
        bgPreview = Color(0xFF090D1A),
        cardPreview = Color(0xFF12192B),
        accentPreview = Color(0xFF60A5FA)
    ),
    ThemePreset(
        id = "AMOLED_BLACK",
        name = "AMOLED Pitch Black",
        description = "True pure OLED #000000 black for maximum contrast",
        bgPreview = Color(0xFF000000),
        cardPreview = Color(0xFF121212),
        accentPreview = Color(0xFF38BDF8)
    ),
    ThemePreset(
        id = "NORD_SLATE",
        name = "Nordic Arctic",
        description = "Cool icy grey-blue tones inspired by arctic winter",
        bgPreview = Color(0xFF11141A),
        cardPreview = Color(0xFF1B202A),
        accentPreview = Color(0xFF818CF8)
    ),
    ThemePreset(
        id = "WARM_EMBER",
        name = "Warm Espresso",
        description = "Cozy dark roasted coffee & ember hues",
        bgPreview = Color(0xFF120E0C),
        cardPreview = Color(0xFF1D1714),
        accentPreview = Color(0xFFF59E0B)
    )
)

val ACCENT_OPTIONS = listOf(
    AccentOption("BLUE", "Electric Blue", Color(0xFF3B82F6)),
    AccentOption("CYAN", "Cyber Cyan", Color(0xFF06B6D4)),
    AccentOption("GREEN", "Emerald Mint", Color(0xFF10B981)),
    AccentOption("PURPLE", "Neon Purple", Color(0xFF8B5CF6)),
    AccentOption("AMBER", "Sunset Amber", Color(0xFFF59E0B)),
    AccentOption("ROSE", "Coral Rose", Color(0xFFF43F5E))
)

val INSTRUMENT_OPTIONS = listOf(
    "Guitar (Standard E)",
    "Guitar (Drop D)",
    "Guitar (Half-Step Down)",
    "Ukulele (Standard)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val settings by DesktopDatabase.instance.settingsFlow.collectAsState()
    var currentTab by remember { mutableStateOf(SettingsTab.APPEARANCE) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    var storageStats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        storageStats = DesktopDatabase.instance.getStats()
    }

    // Modern Native File Saver for Database Export
    val dbExportLauncher = rememberFileSaverLauncher { platformFile ->
        platformFile?.let { pFile ->
            coroutineScope.launch {
                try {
                    val jsonStr = DesktopDatabase.instance.exportDatabaseJson()
                    pFile.file.writeText(jsonStr)
                    snackbarMessage = "Database exported successfully to ${pFile.file.name}"
                } catch (e: Exception) {
                    snackbarMessage = "Export failed: ${e.message}"
                }
            }
        }
    }

    // Modern Native File Picker for Database Import
    val dbImportLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("json")),
        title = "Select Chord Studio Database Backup (.json)"
    ) { platformFile ->
        platformFile?.let { pFile ->
            coroutineScope.launch {
                try {
                    val content = pFile.file.readText()
                    val success = DesktopDatabase.instance.importDatabaseJson(content)
                    if (success) {
                        storageStats = DesktopDatabase.instance.getStats()
                        snackbarMessage = "Database imported successfully!"
                    } else {
                        snackbarMessage = "Invalid backup file format."
                    }
                } catch (e: Exception) {
                    snackbarMessage = "Import failed: ${e.message}"
                }
            }
        }
    }

    val colors = AppColors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Settings Category Sub-Navigation
            Surface(
                modifier = Modifier.width(260.dp).fillMaxHeight(),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Surface(
                            color = colors.cardElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Preferences & System",
                                fontSize = 11.5.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    SettingsTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        Surface(
                            onClick = { currentTab = tab },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) colors.cardElevated else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, colors.borderHover) else null,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.accent else colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = if (isSelected) colors.textPrimary else colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Settings Content Body
            val listState = rememberLazyListState()
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, top = 28.dp, bottom = 20.dp, end = 20.dp)
                ) {
                    // Section Header (Pinned at top, does not scroll under scrollbar)
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp, end = 12.dp)) {
                        Text(
                            text = currentTab.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (currentTab) {
                                SettingsTab.APPEARANCE -> "Personalize color palettes, studio aesthetics, and typography styles."
                                SettingsTab.TAB_VIEWER -> "Customize chords display, autoscroll behavior, and sheet music instruments."
                                SettingsTab.AUDIO_AI -> "Tune on-device neural chord recognition, DSP engine, and audio latency."
                                SettingsTab.DATA_BACKUP -> "Manage local tab cache, database storage, and complete backup exports."
                                SettingsTab.ABOUT -> "View application architecture, licensing, and system diagnostic info."
                            },
                            fontSize = 13.5.sp,
                            color = colors.textSecondary
                        )
                    }

                    // Scrollable Body & Scrollbar (Scrollbar strictly starts below title and never overlaps content)
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {

                    when (currentTab) {
                        SettingsTab.APPEARANCE -> {
                            item {
                                SettingsCard(title = "Studio Theme Preset") {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        THEME_PRESETS.forEach { preset ->
                                            val isSelected = settings.themeMode == preset.id
                                            Surface(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        DesktopDatabase.instance.updateSettings { it.copy(themeMode = preset.id) }
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) colors.cardElevated else colors.card,
                                                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        // Color Swatch Trio
                                                        Row(
                                                            modifier = Modifier
                                                                .size(width = 44.dp, height = 28.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .border(1.dp, colors.borderHover, RoundedCornerShape(6.dp))
                                                        ) {
                                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(preset.bgPreview))
                                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(preset.cardPreview))
                                                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(preset.accentPreview))
                                                        }
                                                        Spacer(Modifier.width(14.dp))
                                                        Column {
                                                            Text(preset.name, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = colors.textPrimary)
                                                            Text(preset.description, fontSize = 12.sp, color = colors.textSecondary)
                                                        }
                                                    }
                                                    if (isSelected) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = colors.accent, modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                SettingsCard(title = "Accent Color") {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ACCENT_OPTIONS.forEach { opt ->
                                            val isSelected = settings.accentColor == opt.id
                                            Surface(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        DesktopDatabase.instance.updateSettings { it.copy(accentColor = opt.id) }
                                                    }
                                                },
                                                shape = CircleShape,
                                                color = opt.color,
                                                border = BorderStroke(2.dp, if (isSelected) Color.White else Color.Transparent),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                if (isSelected) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                SettingsCard(title = "Default Tab Sheet Font Size") {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Scale: ${settings.defaultFontSize.toInt()} pt", fontSize = 13.5.sp, color = colors.textSecondary)
                                            Text("Preview: [Am] [C] [G]", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = settings.defaultFontSize.sp, color = colors.accent)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Slider(
                                            value = settings.defaultFontSize,
                                            onValueChange = { newSize ->
                                                coroutineScope.launch {
                                                    DesktopDatabase.instance.updateSettings { it.copy(defaultFontSize = newSize) }
                                                }
                                            },
                                            valueRange = 12f..24f,
                                            steps = 11,
                                            thumb = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(15.dp)
                                                        .background(colors.accent, CircleShape)
                                                        .border(2.dp, Color.White, CircleShape)
                                                )
                                            },
                                            track = { _ ->
                                                val fraction = ((settings.defaultFontSize - 12f) / (24f - 12f)).coerceIn(0.0001f, 1f)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(colors.border)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(fraction)
                                                            .fillMaxHeight()
                                                            .background(colors.accent)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        SettingsTab.TAB_VIEWER -> {
                            item {
                                SettingsCard(title = "Default Autoscroll Speed") {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Playback Scroll Speed", fontSize = 13.5.sp, color = colors.textSecondary)
                                            Text("${settings.defaultScrollSpeed.toInt()} px/sec", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = colors.accent)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Slider(
                                            value = settings.defaultScrollSpeed,
                                            onValueChange = { newSpeed ->
                                                coroutineScope.launch {
                                                    DesktopDatabase.instance.updateSettings { it.copy(defaultScrollSpeed = newSpeed) }
                                                }
                                            },
                                            valueRange = 10f..60f,
                                            steps = 24,
                                            thumb = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(15.dp)
                                                        .background(colors.accent, CircleShape)
                                                        .border(2.dp, Color.White, CircleShape)
                                                )
                                            },
                                            track = { _ ->
                                                val fraction = ((settings.defaultScrollSpeed - 10f) / (60f - 10f)).coerceIn(0.0001f, 1f)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(colors.border)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(fraction)
                                                            .fillMaxHeight()
                                                            .background(colors.accent)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            item {
                                SettingsCard(title = "Default Instrument Voicing") {
                                    var expanded by remember { mutableStateOf(false) }

                                    Box {
                                        Surface(
                                            onClick = { expanded = true },
                                            shape = RoundedCornerShape(10.dp),
                                            color = colors.cardElevated,
                                            border = BorderStroke(1.dp, colors.border),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(settings.defaultInstrument, fontSize = 14.sp, color = colors.textPrimary)
                                                }
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.textSecondary)
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.background(colors.cardElevated).border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                        ) {
                                            INSTRUMENT_OPTIONS.forEach { inst ->
                                                DropdownMenuItem(
                                                    text = { Text(inst, color = colors.textPrimary) },
                                                    onClick = {
                                                        expanded = false
                                                        coroutineScope.launch {
                                                            DesktopDatabase.instance.updateSettings { it.copy(defaultInstrument = inst) }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                SettingsCard(title = "Sheet View & Interaction Preferences") {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        SettingsToggleRow(
                                            title = "Interactive Chord Popups",
                                            subtitle = "Clicking a chord name displays full fretboard chord diagrams",
                                            checked = settings.showChordPopups,
                                            onCheckedChange = { checked ->
                                                coroutineScope.launch {
                                                    DesktopDatabase.instance.updateSettings { it.copy(showChordPopups = checked) }
                                                }
                                            }
                                        )
                                        HorizontalDivider(color = colors.border)
                                        SettingsToggleRow(
                                            title = "60 FPS Smooth Interpolation",
                                            subtitle = "Hardware-accelerated subpixel smooth autoscrolling",
                                            checked = settings.smoothScroll,
                                            onCheckedChange = { checked ->
                                                coroutineScope.launch {
                                                    DesktopDatabase.instance.updateSettings { it.copy(smoothScroll = checked) }
                                                }
                                            }
                                        )
                                        HorizontalDivider(color = colors.border)
                                        SettingsToggleRow(
                                            title = "Two-Column Mode by Default",
                                            subtitle = "Automatically splits wide screens into dual column reader view",
                                            checked = settings.twoColumnDefault,
                                            onCheckedChange = { checked ->
                                                coroutineScope.launch {
                                                    DesktopDatabase.instance.updateSettings { it.copy(twoColumnDefault = checked) }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        SettingsTab.AUDIO_AI -> {
                            item {
                                var inputExpanded by remember { mutableStateOf(false) }
                                var outputExpanded by remember { mutableStateOf(false) }
                                var inputDevices by remember { mutableStateOf(AudioDeviceManager.getInputDevices()) }
                                var outputDevices by remember { mutableStateOf(AudioDeviceManager.getOutputDevices()) }

                                LaunchedEffect(Unit) {
                                    AudioDeviceManager.refreshDevicesAsync {
                                        inputDevices = AudioDeviceManager.getInputDevices()
                                        outputDevices = AudioDeviceManager.getOutputDevices()
                                    }
                                }

                                SettingsCard(title = "Audio Hardware & Devices") {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        // Audio Input Device
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Audio Input Device (Microphone)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                                                    Text("Used for Guitar Tuner, pitch detection, and live audio analysis", fontSize = 12.sp, color = colors.textSecondary)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        AudioDeviceManager.refreshDevicesAsync {
                                                            inputDevices = AudioDeviceManager.getInputDevices()
                                                            outputDevices = AudioDeviceManager.getOutputDevices()
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh devices", tint = colors.accent, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Box {
                                                Surface(
                                                    onClick = { inputExpanded = true },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = colors.cardElevated,
                                                    border = BorderStroke(1.dp, colors.border),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(14.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                            Icon(Icons.Default.Mic, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                                            Spacer(Modifier.width(10.dp))
                                                            Text(
                                                                text = settings.audioInputDevice,
                                                                fontSize = 13.5.sp,
                                                                color = colors.textPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.textSecondary)
                                                    }
                                                }

                                                DropdownMenu(
                                                    expanded = inputExpanded,
                                                    onDismissRequest = { inputExpanded = false },
                                                    modifier = Modifier.background(colors.cardElevated).border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                                ) {
                                                    inputDevices.forEach { dev ->
                                                        val isSelected = dev == settings.audioInputDevice
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    if (isSelected) {
                                                                        Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                                                        Spacer(Modifier.width(8.dp))
                                                                    }
                                                                    Text(
                                                                        dev,
                                                                        color = if (isSelected) colors.accent else colors.textPrimary,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                inputExpanded = false
                                                                coroutineScope.launch {
                                                                    DesktopDatabase.instance.updateSettings { it.copy(audioInputDevice = dev) }
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = colors.border)

                                        // Audio Output Device
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Column {
                                                Text("Audio Output Device (Playback)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                                                Text("Used for AI Studio playback, backing tracks, and reference tone generator", fontSize = 12.sp, color = colors.textSecondary)
                                            }

                                            Box {
                                                Surface(
                                                    onClick = { outputExpanded = true },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = colors.cardElevated,
                                                    border = BorderStroke(1.dp, colors.border),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(14.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                                            Spacer(Modifier.width(10.dp))
                                                            Text(
                                                                text = settings.audioOutputDevice,
                                                                fontSize = 13.5.sp,
                                                                color = colors.textPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.textSecondary)
                                                    }
                                                }

                                                DropdownMenu(
                                                    expanded = outputExpanded,
                                                    onDismissRequest = { outputExpanded = false },
                                                    modifier = Modifier.background(colors.cardElevated).border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                                ) {
                                                    outputDevices.forEach { dev ->
                                                        val isSelected = dev == settings.audioOutputDevice
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    if (isSelected) {
                                                                        Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                                                        Spacer(Modifier.width(8.dp))
                                                                    }
                                                                    Text(
                                                                        dev,
                                                                        color = if (isSelected) colors.accent else colors.textPrimary,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                outputExpanded = false
                                                                coroutineScope.launch {
                                                                    DesktopDatabase.instance.updateSettings { it.copy(audioOutputDevice = dev) }
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                SettingsCard(title = "AI Model & Inference Engine") {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Neural Network Backend", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                                                Text("Microsoft ONNX Runtime 1.17 + Pure Kotlin DSP", fontSize = 12.sp, color = colors.textSecondary)
                                            }
                                            Surface(
                                                color = Color(0xFF064E3B),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.padding(2.dp)
                                            ) {
                                                Text("Hardware Active", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                            }
                                        }

                                        HorizontalDivider(color = colors.border)

                                        SettingsToggleRow(
                                            title = "Sub-harmonic Bass Filtering & CQT",
                                            subtitle = "Dramatically reduces relative Major/Minor root chord confusions",
                                            checked = settings.highPrecisionDsp,
                                            onCheckedChange = { checked ->
                                                coroutineScope.launch {
                                                    DesktopDatabase.instance.updateSettings { it.copy(highPrecisionDsp = checked) }
                                                }
                                            }
                                        )

                                        HorizontalDivider(color = colors.border)

                                        SettingsToggleRow(
                                            title = "Auto-Fetch Synced Lyrics (LRCLIB)",
                                            subtitle = "Automatically downloads time-synced lyrics on audio import",
                                            checked = settings.autoFetchLyrics,
                                            onCheckedChange = { checked ->
                                                coroutineScope.launch {
                                                    DesktopDatabase.instance.updateSettings { it.copy(autoFetchLyrics = checked) }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        SettingsTab.DATA_BACKUP -> {
                            item {
                                SettingsCard(title = "Library Statistics & Storage") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        StatMiniCard(title = "Saved Tabs", count = storageStats["tabs"] ?: 0, icon = Icons.Default.Description, modifier = Modifier.weight(1f))
                                        StatMiniCard(title = "Favorites", count = storageStats["favorites"] ?: 0, icon = Icons.Default.Favorite, modifier = Modifier.weight(1f))
                                        StatMiniCard(title = "Playlists", count = storageStats["playlists"] ?: 0, icon = Icons.AutoMirrored.Filled.List, modifier = Modifier.weight(1f))
                                        StatMiniCard(title = "History", count = storageStats["history"] ?: 0, icon = Icons.Default.History, modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            item {
                                SettingsCard(title = "Backup & Restore (Native Common Dialogs)") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                                dbExportLauncher.launch(
                                                    baseName = "TabsLite_Backup_$timestamp",
                                                    extension = "json"
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                            modifier = Modifier.weight(1f).height(44.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Export Library (.json)", fontWeight = FontWeight.SemiBold)
                                        }

                                        OutlinedButton(
                                            onClick = { dbImportLauncher.launch() },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, colors.border),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                                            modifier = Modifier.weight(1f).height(44.dp)
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Import Backup (.json)", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            item {
                                SettingsCard(title = "Maintenance & Cleanup") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    DesktopDatabase.instance.clearNonLibraryCache()
                                                    storageStats = DesktopDatabase.instance.getStats()
                                                    snackbarMessage = "Search cache & unpinned temp tabs cleared."
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, colors.border),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                                            modifier = Modifier.weight(1f).height(42.dp)
                                        ) {
                                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Clear Search Cache", fontSize = 13.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { showClearHistoryDialog = true },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF5A2A2A)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                                            modifier = Modifier.weight(1f).height(42.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Clear History", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        SettingsTab.ABOUT -> {
                            item {
                                fun openWebUrl(url: String) {
                                    try {
                                        if (Desktop.isDesktopSupported()) {
                                            Desktop.getDesktop().browse(URI(url))
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.card),
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp, horizontal = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Centered App Icon with subtle glowing rim
                                        Box(
                                            modifier = Modifier
                                                .size(84.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(colors.cardElevated)
                                                .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource("app_icon_128.png"),
                                                contentDescription = "Chord Studio Logo",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(20.dp))
                                            )
                                        }

                                        // Title, Version & Rich Description
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Chord Studio",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 23.sp,
                                                color = colors.textPrimary,
                                                letterSpacing = 0.3.sp
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = "v0.8.0 • Windows 64-bit Edition",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textMuted
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            Text(
                                                text = "Clean, fast, and effortless guitar tablature, chords & AI audio studio.",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "Built for musicians, songwriters, and learners with hardware-accelerated 60fps rendering,\nreal-time neural chord recognition, chromatic instrument tuning, and studio PDF sheet generation.",
                                                fontSize = 12.5.sp,
                                                lineHeight = 18.sp,
                                                color = colors.textSecondary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 20.dp)
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = "Privacy Focused •  Offline-First Storage  •  Zero Ads",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF10B981),
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        // Subtle Center Divider
                                        Box(
                                            modifier = Modifier
                                                .width(52.dp)
                                                .height(1.5.dp)
                                                .background(colors.border)
                                        )

                                        // Developed by
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Developed by",
                                                fontSize = 12.sp,
                                                color = colors.textMuted
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Sayan Dey",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        }

                                        // Social & Store Links - closely spaced
                                        Column(
                                            horizontalAlignment = Alignment.Start,
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.wrapContentWidth()
                                        ) {
                                            // Microsoft Store
                                            Surface(
                                                onClick = { openWebUrl("https://apps.microsoft.com/detail/9P71JGK9JB2Q") },
                                                color = Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(Color(0xFF0078D4)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ShoppingCart,
                                                            contentDescription = "Microsoft Store",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(
                                                        text = "Microsoft Store (ID: 9P71JGK9JB2Q)",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF38BDF8)
                                                    )
                                                }
                                            }

                                            // LinkedIn
                                            Surface(
                                                onClick = { openWebUrl("https://www.linkedin.com/in/sayan-dey021") },
                                                color = Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(Color(0xFF0A66C2)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "in",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            lineHeight = 11.sp
                                                        )
                                                    }
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(
                                                        text = "www.linkedin.com/in/sayan-dey021",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF38BDF8)
                                                    )
                                                }
                                            }

                                            // GitHub
                                            Surface(
                                                onClick = { openWebUrl("https://github.com/sayandey021") },
                                                color = Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.size(18.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Code,
                                                            contentDescription = "GitHub",
                                                            tint = colors.textPrimary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(
                                                        text = "github.com/sayandey021",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF38BDF8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // Diagnostic Row Details & Package Build Info
                                SettingsCard(title = "Engine Diagnostics & Build Specs") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DiagnosticRow(label = "Package Identity", value = "Saayan.ChordStudio (PFN: Saayan.ChordStudio_f0v6x7d2rzc78)", statusColor = Color(0xFF34D399))
                                        DiagnosticRow(label = "Publisher", value = "SaayanSoft (CN=37E2AF47-D2FC-489C-BDC1-02C989A7B989)", statusColor = Color(0xFF34D399))
                                        DiagnosticRow(label = "Microsoft Store ID", value = "9P71JGK9JB2Q", statusColor = Color(0xFF34D399))
                                        DiagnosticRow(label = "Distribution Formats", value = "Windows 64-bit EXE & MSIX Package", statusColor = Color(0xFF34D399))
                                        DiagnosticRow(label = "Desktop Runtime", value = "Compose Multiplatform 1.7.3 (Skiko 60fps)", statusColor = Color(0xFF34D399))
                                        DiagnosticRow(label = "Audio DSP Engine", value = "Active (16 kHz Resampler + Subharmonic CQT)", statusColor = Color(0xFF34D399))
                                        DiagnosticRow(label = "Neural Model Runtime", value = "ONNX Runtime 1.17.1 (BTC & ChordNet)", statusColor = Color(0xFF34D399))
                                        DiagnosticRow(label = "Local Storage", value = "SQLite Engine (Zero-Latency Offline Cache)", statusColor = Color(0xFF34D399))
                                        DiagnosticRow(label = "PDF Engine", value = "Apache PDFBox 2.0.31 Vector Exporter", statusColor = Color(0xFF34D399))
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
                        hoverColor = colors.accent,
                        thickness = 8.dp,
                        shape = RoundedCornerShape(4.dp)
                    )
                )
            }
        }

        // Toast / Snackbar feedback
        snackbarMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3500)
                snackbarMessage = null
            }
            Surface(
                color = colors.cardElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colors.accent),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = msg,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
        }
    }

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Playback History?", color = colors.textPrimary) },
            text = { Text("This will permanently remove all recently viewed tabs from your history log.", color = colors.textSecondary) },
            containerColor = colors.card,
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryDialog = false
                        coroutineScope.launch {
                            DesktopDatabase.instance.clearHistory()
                            storageStats = DesktopDatabase.instance.getStats()
                            snackbarMessage = "History cleared."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = AppColors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = AppColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.cardElevated
            )
        )
    }
}

@Composable
fun StatMiniCard(
    title: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = AppColors
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.cardElevated,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, color = colors.textSecondary)
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("$count", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.textPrimary)
        }
    }
}

@Composable
fun DiagnosticRow(
    label: String,
    value: String,
    statusColor: Color
) {
    val colors = AppColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(end = 12.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(statusColor))
            Spacer(Modifier.width(8.dp))
            Text(
                value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

