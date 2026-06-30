package com.zinema.app.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zinema.app.core.domain.model.Episode
import com.zinema.app.core.domain.model.PlaybackPosition
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * Season selector + episode list (blueprint T-046). Rendered as a plain Column
 * (not a LazyColumn) because it lives inside the detail screen's vertical scroll
 * (PHASE-6 §Deviations). A progress bar shows on the in-progress episode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListSection(
    seasons: List<Int>,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit,
    episodes: List<Episode>,
    resume: PlaybackPosition?,
    onEpisodeClick: (season: Int, episode: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (seasons.size > 1) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = "Season $selectedSeason",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedTextColor = ZinemaColors.OnBackground,
                        unfocusedTextColor = ZinemaColors.OnBackground,
                    ),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    seasons.forEach { season ->
                        DropdownMenuItem(
                            text = { Text("Season $season") },
                            onClick = {
                                onSeasonSelected(season)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        episodes.forEach { episode ->
            val inProgress = resume != null &&
                resume.seasonIndex == episode.seasonIndex &&
                resume.episodeIndex == episode.episodeIndex
            val progress = if (inProgress && resume!!.totalDurationMs > 0) {
                (resume.positionMs.toFloat() / resume.totalDurationMs).coerceIn(0f, 1f)
            } else {
                0f
            }
            EpisodeRow(
                episode = episode,
                progress = progress,
                onClick = { onEpisodeClick(episode.seasonIndex, episode.episodeIndex) },
            )
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, progress: Float, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(4.dp))
                .background(ZinemaColors.SurfaceVariant),
        ) {
            AsyncImage(
                model = episode.thumbnailUrl,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "${episode.episodeIndex}. ${episode.title}".ifBlank { "Episode ${episode.episodeIndex}" },
                color = ZinemaColors.OnBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val duration = formatDuration(episode.durationMs)
            if (duration.isNotEmpty()) {
                Text(text = duration, color = ZinemaColors.TextSecondary, fontSize = 12.sp)
            }
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = ZinemaColors.ProgressBar,
                    trackColor = ZinemaColors.SurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return ""
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
