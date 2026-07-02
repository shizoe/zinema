package com.zinema.app.feature.shorttv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.ui.components.ErrorBanner
import com.zinema.app.core.ui.theme.ZinemaColors
import kotlinx.coroutines.delay

/**
 * Vertical ShortTV feed, mobile only (blueprint T-056): a full-screen snapping
 * [VerticalPager]; the current page autoplays (muted) via a single shared
 * [ExoPlayer], tap toggles mute, and a "+ Watch Series" button appears after 2s.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ShortTvScreen(
    onWatchSeries: (Content) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShortTvViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        when (val state = uiState) {
            ShortTvUiState.Loading -> {
                CircularProgressIndicator(
                    color = ZinemaColors.Primary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            ShortTvUiState.Empty -> {
                Text(
                    text = "No shorts available right now.",
                    color = ZinemaColors.TextSecondary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is ShortTvUiState.Error -> {
                ErrorBanner(
                    message = state.message,
                    onRetry = viewModel::load,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is ShortTvUiState.Success -> {
                val pagerState = rememberPagerState(pageCount = { state.items.size })
                LaunchedEffect(pagerState.currentPage) {
                    viewModel.onPageVisible(pagerState.currentPage)
                }
                VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    ShortItem(
                        content = state.items[page],
                        isCurrent = page == pagerState.currentPage,
                        player = viewModel.player,
                        onToggleMute = viewModel::toggleMute,
                        onWatchSeries = { onWatchSeries(state.items[page]) },
                    )
                }
            }
        }

        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
            Text(text = "←", color = ZinemaColors.OnBackground, fontSize = 22.sp)
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ShortItem(
    content: Content,
    isCurrent: Boolean,
    player: ExoPlayer,
    onToggleMute: () -> Unit,
    onWatchSeries: () -> Unit,
) {
    var showWatchSeries by remember { mutableStateOf(false) }
    LaunchedEffect(isCurrent) {
        showWatchSeries = false
        if (isCurrent) {
            delay(2_000)
            showWatchSeries = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onToggleMute() },
    ) {
        AsyncImage(
            model = content.posterUrl,
            contentDescription = content.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isCurrent) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = player
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = content.title,
                color = ZinemaColors.OnBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            if (content.totalEpisodes > 0) {
                Text(
                    text = "${content.totalEpisodes} episodes",
                    color = ZinemaColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            if (showWatchSeries) {
                Button(
                    onClick = onWatchSeries,
                    colors = ButtonDefaults.buttonColors(containerColor = ZinemaColors.Primary),
                ) {
                    Text(text = "+  Watch Series", color = ZinemaColors.OnBackground)
                }
            }
        }
    }
}
