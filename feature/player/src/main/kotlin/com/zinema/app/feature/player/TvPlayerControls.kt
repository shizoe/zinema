package com.zinema.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * TV player control overlay (blueprint T-050). D-pad-friendly focusable buttons
 * (seek ±10s, play/pause, CC, quality) over a progress bar + time readout.
 */
@Composable
fun TvPlayerControls(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleQuality: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(ZinemaColors.Overlay)) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { fraction },
                color = ZinemaColors.Primary,
                trackColor = ZinemaColors.SurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                color = ZinemaColors.OnSurface,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TvControlButton(label = "« 10s", onClick = onSeekBack)
                TvControlButton(label = if (isPlaying) "Pause" else "Play", onClick = onPlayPause)
                TvControlButton(label = "10s »", onClick = onSeekForward)
                TvControlButton(label = "CC", onClick = onToggleSubtitles)
                TvControlButton(label = "Quality", onClick = onToggleQuality)
            }
        }
    }
}

@Composable
private fun TvControlButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ZinemaColors.SurfaceVariant),
    ) {
        Text(text = label, color = ZinemaColors.OnBackground)
    }
}
