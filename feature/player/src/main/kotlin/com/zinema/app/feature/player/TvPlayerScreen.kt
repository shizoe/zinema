package com.zinema.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.zinema.app.core.ui.components.ErrorBanner
import com.zinema.app.core.ui.theme.ZinemaColors
import kotlinx.coroutines.delay

/**
 * Android TV player (blueprint T-049): full-screen ExoPlayer + D-pad controls and a
 * back affordance. The "next episode" row is deferred (PHASE-7 §Deviations).
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun TvPlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSubtitle by viewModel.selectedSubtitleLanguage.collectAsStateWithLifecycle()
    val player = viewModel.player

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var showSubtitles by remember { mutableStateOf(false) }
    var showQuality by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition
            durationMs = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            delay(500)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { useController = false; this.player = player } },
            modifier = Modifier.fillMaxSize(),
        )

        when (val state = uiState) {
            PlayerUiState.Loading -> {
                CircularProgressIndicator(
                    color = ZinemaColors.Primary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is PlayerUiState.Error -> {
                ErrorBanner(
                    message = state.message,
                    onRetry = { viewModel.loadStreamInfo() },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is PlayerUiState.Ready -> {
                TvPlayerControls(
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    currentQuality = state.streamInfo.quality,
                    onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                    onSeekBack = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) },
                    onSeekForward = { player.seekTo(player.currentPosition + 10_000) },
                    onToggleSubtitles = { showSubtitles = true },
                    onToggleQuality = { showQuality = true },
                )
            }
        }

        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
            Text(text = "←", color = ZinemaColors.OnBackground, fontSize = 22.sp)
        }
    }

    (uiState as? PlayerUiState.Ready)?.let { ready ->
        if (showSubtitles) {
            SubtitleTrackSelector(
                tracks = ready.streamInfo.subtitles,
                selectedLanguage = selectedSubtitle,
                onSelect = viewModel::selectSubtitle,
                onDismiss = { showSubtitles = false },
            )
        }
        if (showQuality) {
            QualitySelector(
                qualities = ready.streamInfo.availableQualities,
                selected = ready.streamInfo.quality,
                onSelect = viewModel::changeQuality,
                onDismiss = { showQuality = false },
            )
        }
    }
}
