package com.zinema.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * Mobile player control overlay (blueprint T-050): back / PiP, a center play-pause,
 * a scrubber with position+duration, and CC / Quality actions.
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleQuality: () -> Unit,
    onPip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(ZinemaColors.Overlay)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Text(text = "←", color = ZinemaColors.OnBackground, fontSize = 22.sp)
            }
            IconButton(onClick = onPip) {
                Text(text = "⧉", color = ZinemaColors.OnBackground, fontSize = 18.sp)
            }
        }

        IconButton(onClick = onPlayPause, modifier = Modifier.align(Alignment.Center)) {
            Text(
                text = if (isPlaying) "⏸" else "▶",
                color = ZinemaColors.OnBackground,
                fontSize = 40.sp,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            Slider(
                value = fraction,
                onValueChange = { onSeekTo((it * durationMs).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = ZinemaColors.Primary,
                    activeTrackColor = ZinemaColors.Primary,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                    color = ZinemaColors.OnSurface,
                    fontSize = 12.sp,
                )
                Box(modifier = Modifier.weight(1f))
                TextButton(onClick = onToggleSubtitles) {
                    Text(text = "CC", color = ZinemaColors.OnBackground)
                }
                TextButton(onClick = onToggleQuality) {
                    Text(text = "HD", color = ZinemaColors.OnBackground)
                }
            }
        }
    }
}

/** Formats milliseconds as `m:ss` or `h:mm:ss`. */
internal fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
