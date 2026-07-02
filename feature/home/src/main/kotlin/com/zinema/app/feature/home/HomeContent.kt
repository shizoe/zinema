package com.zinema.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.ui.components.ContentRail
import com.zinema.app.core.ui.components.ErrorBanner
import com.zinema.app.core.ui.components.HeroBanner
import com.zinema.app.core.ui.components.ShimmerRail
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * Renders a tab's feed for both mobile and TV (blueprint T-042/T-043): a hero
 * carousel followed by content rails, with shimmer / empty / error states.
 */
@Composable
fun HomeContent(
    state: TabContentUiState,
    onItemClick: (Content) -> Unit,
    onPlayClick: (Content) -> Unit,
    onWatchlistClick: (Content) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        TabContentUiState.Loading -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(4) { ShimmerRail() }
            }
        }

        is TabContentUiState.Success -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.hero.isNotEmpty()) {
                    item(key = "hero") {
                        HeroBanner(
                            items = state.hero,
                            onPlayClick = onPlayClick,
                            onWatchlistClick = onWatchlistClick,
                            onItemClick = onItemClick,
                        )
                    }
                }
                items(items = state.rails, key = { it.title }) { rail ->
                    ContentRail(
                        title = rail.title,
                        items = rail.items,
                        onItemClick = onItemClick,
                    )
                }
            }
        }

        TabContentUiState.Empty -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Content not available in your region.",
                    color = ZinemaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }

        is TabContentUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorBanner(message = state.message, onRetry = onRetry)
            }
        }
    }
}
