package com.zinema.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentTab
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * Android TV browse screen (blueprint T-043): a left navigation rail listing all
 * tabs + the content area (hero + rails) on the right. Full tv-material focus
 * polish (TvLazyColumn / rememberTvLazyListState) is deferred — see PHASE-5
 * §Deviations.
 */
@Composable
fun TvHomeScreen(
    onItemClick: (Content) -> Unit,
    onPlayClick: (Content) -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
    tabContentViewModel: TabContentViewModel = hiltViewModel(),
) {
    val selectedTabId by homeViewModel.selectedTabId.collectAsStateWithLifecycle()
    val state by tabContentViewModel.uiState.collectAsStateWithLifecycle()
    val tabs by homeViewModel.tabs.collectAsStateWithLifecycle()

    LaunchedEffect(selectedTabId) { tabContentViewModel.loadTab(selectedTabId) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(ZinemaColors.Background),
    ) {
        TvNavDrawer(
            tabs = tabs,
            selectedTabId = selectedTabId,
            onSelect = homeViewModel::selectTab,
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(ZinemaColors.Surface),
        )
        HomeContent(
            state = state,
            onItemClick = onItemClick,
            onPlayClick = onPlayClick,
            onWatchlistClick = homeViewModel::onWatchlistToggle,
            onRetry = tabContentViewModel::retry,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TvNavDrawer(
    tabs: List<ContentTab>,
    selectedTabId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEach { tab ->
            val selected = tab.tabId == selectedTabId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selected) ZinemaColors.SurfaceVariant else ZinemaColors.Surface)
                    .clickable { onSelect(tab.tabId) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = glyphForTab(tab.tabId), fontSize = 16.sp)
                Text(
                    text = tab.displayName,
                    color = if (selected) ZinemaColors.OnBackground else ZinemaColors.TextSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
