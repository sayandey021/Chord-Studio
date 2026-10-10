package com.gbros.tabslite.desktop

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gbros.tabslite.ui.*

sealed class DesktopScreen {
    object Home : DesktopScreen()
    data class TabViewer(val tabId: String) : DesktopScreen()
    data class PlaylistDetail(val playlistId: Int) : DesktopScreen()
}

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val logFile = java.io.File("chordstudio_error.log")
        logFile.writeText("Crash on ${thread.name}: ${throwable.stackTraceToString()}")
        throwable.printStackTrace()
    }

    application {
        val windowState = rememberWindowState(width = 1180.dp, height = 820.dp)
        var selectedSection by remember { mutableStateOf(NavigationSection.SEARCH) }
        val sectionStacks = remember {
            mutableStateMapOf<NavigationSection, SnapshotStateList<DesktopScreen>>().apply {
                NavigationSection.entries.forEach { section ->
                    put(section, mutableStateListOf(DesktopScreen.Home))
                }
            }
        }

        val currentStack = sectionStacks.getOrPut(selectedSection) { mutableStateListOf(DesktopScreen.Home) }
        val currentScreen = currentStack.lastOrNull() ?: DesktopScreen.Home

        fun navigateTo(screen: DesktopScreen) {
            currentStack.add(screen)
        }

        fun navigateBack() {
            if (currentStack.size > 1) {
                currentStack.removeAt(currentStack.size - 1)
            }
        }

        val dynamicTitle = when (currentScreen) {
            is DesktopScreen.Home -> "Chord Studio • ${selectedSection.title}"
            is DesktopScreen.TabViewer -> "Chord Studio • Tab Viewer"
            is DesktopScreen.PlaylistDetail -> "Chord Studio • Playlist"
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = dynamicTitle,
            icon = painterResource("app_icon.png"),
            state = windowState,
            undecorated = false,
            onKeyEvent = { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when {
                        keyEvent.key == Key.Escape -> {
                            if (currentStack.size > 1) {
                                navigateBack()
                                true
                            } else false
                        }
                        keyEvent.isCtrlPressed && keyEvent.key == Key.One -> {
                            selectedSection = NavigationSection.SEARCH
                            true
                        }
                        keyEvent.isCtrlPressed && keyEvent.key == Key.Two -> {
                            selectedSection = NavigationSection.FAVORITES
                            true
                        }
                        keyEvent.isCtrlPressed && keyEvent.key == Key.Three -> {
                            selectedSection = NavigationSection.PLAYLISTS
                            true
                        }
                        keyEvent.isCtrlPressed && keyEvent.key == Key.Four -> {
                            selectedSection = NavigationSection.HISTORY
                            true
                        }
                        keyEvent.isCtrlPressed && keyEvent.key == Key.Five -> {
                            selectedSection = NavigationSection.TUNER
                            true
                        }
                        keyEvent.isCtrlPressed && keyEvent.key == Key.N -> {
                            selectedSection = NavigationSection.CREATE_TAB
                            true
                        }
                        keyEvent.isCtrlPressed && keyEvent.key == Key.Comma -> {
                            selectedSection = NavigationSection.SETTINGS
                            true
                        }
                        else -> false
                    }
                } else false
            }
        ) {
            val settings by com.gbros.tabslite.data.DesktopDatabase.instance.settingsFlow.collectAsState()

            LaunchedEffect(window, settings.themeMode) {
                val captionColor = when (settings.themeMode) {
                    "OBSIDIAN_DARK" -> 0x0C0D12
                    "MIDNIGHT_BLUE" -> 0x090D1A
                    "AMOLED_BLACK" -> 0x000000
                    "NORD_SLATE" -> 0x11141A
                    "WARM_EMBER" -> 0x120E0C
                    else -> 0x151821
                }
                com.gbros.tabslite.utilities.WindowsDwmHelper.applyWindows11DarkTheme(window, captionColor)
            }

            TabsLiteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.bg
                ) {
                    DesktopAppScaffold(
                        currentScreen = currentScreen,
                        selectedSection = selectedSection,
                        onSectionSelected = { section ->
                            if (selectedSection == section && currentStack.size > 1) {
                                currentStack.clear()
                                currentStack.add(DesktopScreen.Home)
                            } else {
                                selectedSection = section
                            }
                        },
                        onSelectTab = { tabId -> navigateTo(DesktopScreen.TabViewer(tabId)) },
                        onSelectPlaylist = { playlistId -> navigateTo(DesktopScreen.PlaylistDetail(playlistId)) },
                        onNavigateBack = ::navigateBack,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
