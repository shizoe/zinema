package com.zinema.app.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.zinema.app.core.ui.components.ErrorBanner
import com.zinema.app.core.ui.theme.ZinemaColors
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Mobile player (blueprint T-048): forced landscape, ExoPlayer surface, custom
 * controls (auto-hide), double-tap ±10s seek, PiP, and vertical-swipe gestures —
 * volume on the right half, screen brightness on the left — with a transient
 * on-screen indicator that auto-hides after ~2s.
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
    var videoScale by remember { mutableStateOf(VideoScale.FIT) }

    // Swipe volume (right half) / brightness (left half) + double-tap seek feedback.
    val activity = context.findActivity()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeFraction by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }
    var brightnessFraction by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f }
                ?: runCatching {
                    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                }.getOrDefault(0.5f),
        )
    }
    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }

    // Feedback overlay auto-hides ~2s after the last gesture update.
    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(GESTURE_FEEDBACK_MS)
            gestureFeedback = null
        }
    }

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
            update = { it.resizeMode = videoScale.resizeMode },
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
                                player.seekTo((player.currentPosition - SEEK_STEP_MS).coerceAtLeast(0))
                                gestureFeedback = GestureFeedback(GestureType.SEEK_BACK, 0f)
                            } else {
                                player.seekTo(player.currentPosition + SEEK_STEP_MS)
                                gestureFeedback = GestureFeedback(GestureType.SEEK_FORWARD, 0f)
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    // Right half = volume, left half = brightness; drag up to raise.
                    var onRightHalf = false
                    detectVerticalDragGestures(
                        onDragStart = { offset -> onRightHalf = offset.x >= size.width / 2f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount / size.height // full swipe ≈ full range
                            if (onRightHalf) {
                                volumeFraction = (volumeFraction + delta).coerceIn(0f, 1f)
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    (volumeFraction * maxVolume).roundToInt(),
                                    0,
                                )
                                gestureFeedback = GestureFeedback(GestureType.VOLUME, volumeFraction)
                            } else {
                                brightnessFraction = (brightnessFraction + delta).coerceIn(0.01f, 1f)
                                activity?.window?.let { w ->
                                    w.attributes = w.attributes.apply { screenBrightness = brightnessFraction }
                                }
                                gestureFeedback = GestureFeedback(GestureType.BRIGHTNESS, brightnessFraction)
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
                        videoScaleLabel = videoScale.label,
                        onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                        onSeekTo = { player.seekTo(it) },
                        onToggleSubtitles = { showSubtitles = true },
                        onToggleQuality = { showQuality = true },
                        onCycleScale = {
                            videoScale = videoScale.next()
                            gestureFeedback = GestureFeedback(GestureType.SCALE, label = videoScale.label)
                        },
                        onPip = { context.findActivity()?.enterPip() },
                        onBack = onBack,
                    )
                }
            }
        }

        gestureFeedback?.let { fb ->
            GestureFeedbackOverlay(feedback = fb, modifier = Modifier.align(Alignment.Center))
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

/** How the video surface fills the screen. Cycled by the aspect button. */
@androidx.annotation.OptIn(UnstableApi::class)
private enum class VideoScale(val label: String, val resizeMode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    CROP("Crop", AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

    fun next(): VideoScale = entries[(ordinal + 1) % entries.size]
}

/** Transient overlay shown mid-gesture: volume/brightness level, seek nudge, or scale mode. */
private enum class GestureType { VOLUME, BRIGHTNESS, SEEK_BACK, SEEK_FORWARD, SCALE }

private data class GestureFeedback(val type: GestureType, val level: Float = 0f, val label: String = "")

@Composable
private fun GestureFeedbackOverlay(feedback: GestureFeedback, modifier: Modifier = Modifier) {
    val (icon, label) = when (feedback.type) {
        GestureType.VOLUME -> volumeGlyph(feedback.level) to "${(feedback.level * 100).roundToInt()}%"
        GestureType.BRIGHTNESS -> "☀" to "${(feedback.level * 100).roundToInt()}%"
        GestureType.SEEK_BACK -> "«" to "-${SEEK_STEP_MS / 1000}s"
        GestureType.SEEK_FORWARD -> "»" to "+${SEEK_STEP_MS / 1000}s"
        GestureType.SCALE -> "⤢" to feedback.label
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ZinemaColors.Overlay)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = icon, color = ZinemaColors.OnBackground, fontSize = 28.sp)
        Text(
            text = label,
            color = ZinemaColors.OnBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (feedback.type == GestureType.VOLUME || feedback.type == GestureType.BRIGHTNESS) {
            LinearProgressIndicator(
                progress = { feedback.level },
                color = ZinemaColors.Primary,
                trackColor = ZinemaColors.SurfaceVariant,
                modifier = Modifier.padding(top = 10.dp).width(120.dp),
            )
        }
    }
}

private fun volumeGlyph(level: Float): String = when {
    level <= 0f -> "🔇"
    level < 0.5f -> "🔉"
    else -> "🔊"
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

private const val SEEK_STEP_MS = 10_000L
private const val GESTURE_FEEDBACK_MS = 2_000L
