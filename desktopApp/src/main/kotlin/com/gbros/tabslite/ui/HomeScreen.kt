package com.gbros.tabslite.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * HomeScreen has been upgraded to DesktopAppScaffold for the full desktop multi-tab experience.
 */
@Composable
fun HomeScreen(
    onSelectTab: (String) -> Unit,
    onSelectPlaylist: (Int) -> Unit,
    onCreateTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    DesktopAppScaffold(
        selectedSection = NavigationSection.SEARCH,
        onSectionSelected = { section ->
            if (section == NavigationSection.CREATE_TAB) onCreateTab()
        },
        onSelectTab = onSelectTab,
        onSelectPlaylist = onSelectPlaylist,
        modifier = modifier
    )
}
