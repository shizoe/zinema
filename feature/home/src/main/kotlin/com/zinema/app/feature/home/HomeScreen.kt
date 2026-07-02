package com.zinema.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentTab
import com.zinema.app.core.ui.components.OfflineBanner
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * Mobile browse feed (blueprint T-042): top bar, bottom navigation (5 tabs +
 * More → category grid), and the tab content area (hero + rails / shimmer / empty).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onItemClick: (Content) -> Unit,
    onPlayClick: (Content) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onShortTvClick: () -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
    tabContentViewModel: TabContentViewModel = hiltViewModel(),
) {
    val selectedTabId by homeViewModel.selectedTabId.collectAsStateWithLifecycle()
    val state by tabContentViewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by homeViewModel.isOnline.collectAsStateWithLifecycle()
    val tabs by homeViewModel.tabs.collectAsStateWithLifecycle()
    var moreOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedTabId) { tabContentViewModel.loadTab(selectedTabId) }

    Scaffold(
        modifier = modifier,
        containerColor = ZinemaColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ZINEMA",
                        color = ZinemaColors.Primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Text(text = "🔍", color = ZinemaColors.OnBackground)
                    }
                    IconButton(onClick = onProfileClick) {
                        Text(text = "👤", color = ZinemaColors.OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZinemaColors.Background,
                    titleContentColor = ZinemaColors.Primary,
                ),
            )
        },
        bottomBar = {
            HomeBottomBar(
                selectedTabId = selectedTabId,
                moreOpen = moreOpen,
                onSelect = { id ->
                    if (id == SHORTTV_TAB_ID) {
                        onShortTvClick()
                    } else {
                        moreOpen = false
                        homeViewModel.selectTab(id)
                    }
                },
                onMore = { moreOpen = true },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!isOnline) {
                OfflineBanner()
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (moreOpen) {
                    CategoryGrid(
                        tabs = tabs,
                        onTabClick = { id ->
                            moreOpen = false
                            homeViewModel.selectTab(id)
                        },
                    )
                } else {
                    HomeContent(
                        state = state,
                        onItemClick = onItemClick,
                        onPlayClick = onPlayClick,
                        onWatchlistClick = homeViewModel::onWatchlistToggle,
                        onRetry = tabContentViewModel::retry,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    selectedTabId: Int,
    moreOpen: Boolean,
    onSelect: (Int) -> Unit,
    onMore: () -> Unit,
) {
    NavigationBar(containerColor = ZinemaColors.Surface) {
        BOTTOM_NAV_TAB_IDS.forEach { id ->
            val tab = CONTENT_TABS.first { it.tabId == id }
            NavigationBarItem(
                selected = !moreOpen && id == selectedTabId,
                onClick = { onSelect(id) },
                icon = { Text(text = glyphForTab(id)) },
                label = { Text(text = tab.displayName, fontSize = 10.sp) },
                colors = navItemColors(),
            )
        }
        NavigationBarItem(
            selected = moreOpen,
            onClick = onMore,
            icon = { Text(text = "☰") },
            label = { Text(text = "More", fontSize = 10.sp) },
            colors = navItemColors(),
        )
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = ZinemaColors.Primary,
    selectedTextColor = ZinemaColors.Primary,
    unselectedIconColor = ZinemaColors.TextSecondary,
    unselectedTextColor = ZinemaColors.TextSecondary,
    indicatorColor = ZinemaColors.SurfaceVariant,
)

@Composable
private fun CategoryGrid(
    tabs: List<ContentTab>,
    onTabClick: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = tabs, key = { it.tabId }) { tab ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .aspectRatio(1.4f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ZinemaColors.SurfaceVariant)
                    .clickable { onTabClick(tab.tabId) }
                    .padding(8.dp),
            ) {
                Text(text = glyphForTab(tab.tabId), fontSize = 24.sp)
                Text(
                    text = tab.displayName,
                    color = ZinemaColors.OnSurface,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
