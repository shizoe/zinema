package com.zinema.app.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.zinema.app.core.ui.components.ErrorBanner
import com.zinema.app.core.ui.theme.ZinemaColors
import kotlinx.coroutines.delay

/**
 * Mobile player (blueprint T-048): forced landscape, ExoPlayer surface, custom
 * controls (auto-hide), double-tap ±10s seek, and PiP. Swipe volume/brightness is
 * not yet implemented (PHASE-7 §Deviations).
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSubtitle by viewModel.selectedSubtitleLanguage.collectAsStateWithLifecycle()
    val player = viewModel.player
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
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
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3_000)
            controlsVisible = false
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { useController = false; this.player = player } },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2f) {
                                player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
                            } else {
                                player.seekTo(player.currentPosition + 10_000)
                            }
                        },
                    )
                },
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
                if (controlsVisible) {
                    PlayerControls(
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        currentQuality = state.streamInfo.quality,
                        onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                        onSeekTo = { player.seekTo(it) },
                        onToggleSubtitles = { showSubtitles = true },
                        onToggleQuality = { showQuality = true },
                        onPip = { context.findActivity()?.enterPip() },
                        onBack = onBack,
                    )
                }
            }
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

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Activity.enterPip() {
    enterPictureInPictureMode(PictureInPictureParams.Builder().build())
}
