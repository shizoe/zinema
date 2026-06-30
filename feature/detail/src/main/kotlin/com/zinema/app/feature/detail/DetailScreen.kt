package com.zinema.app.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentDetail
import com.zinema.app.core.domain.model.ContentType
import com.zinema.app.core.domain.model.Episode
import com.zinema.app.core.domain.model.PlaybackPosition
import com.zinema.app.core.ui.components.ContentRail
import com.zinema.app.core.ui.components.ErrorBanner
import com.zinema.app.core.ui.components.GenreChip
import com.zinema.app.core.ui.components.RatingBadge
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * Content detail screen (blueprint T-045): a collapsing backdrop, primary actions,
 * metadata, an expandable synopsis, a conditional episode list (TV/anime), and a
 * "More Like This" rail.
 */
@Composable
fun DetailScreen(
    onPlayClick: (subjectId: String, season: Int, episode: Int) -> Unit,
    onBackClick: () -> Unit,
    onItemClick: (Content) -> Unit,
    onShareClick: (Content) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().background(ZinemaColors.Background)) {
        when (val state = uiState) {
            DetailUiState.Loading -> {
                CircularProgressIndicator(
                    color = ZinemaColors.Primary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is DetailUiState.Error -> {
                ErrorBanner(
                    message = state.message,
                    onRetry = viewModel::retry,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is DetailUiState.Success -> {
                DetailContent(
                    detail = state.detail,
                    viewModel = viewModel,
                    onPlayClick = onPlayClick,
                    onItemClick = onItemClick,
                    onShareClick = onShareClick,
                )
            }
        }

        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.TopStart)) {
            Text(text = "←", color = ZinemaColors.OnBackground, fontSize = 22.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    detail: ContentDetail,
    viewModel: DetailViewModel,
    onPlayClick: (String, Int, Int) -> Unit,
    onItemClick: (Content) -> Unit,
    onShareClick: (Content) -> Unit,
) {
    val content = detail.content
    val isInWatchlist by viewModel.isInWatchlist.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeason.collectAsStateWithLifecycle()
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val resume by viewModel.resume.collectAsStateWithLifecycle()

    val scroll = rememberScrollState()
    var descExpanded by remember { mutableStateOf(false) }
    val isSeries = content.type == ContentType.TV || content.type == ContentType.ANIME

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Collapsing backdrop: fades + parallaxes as the page scrolls (blueprint T-045).
        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            AsyncImage(
                model = content.backdropUrl.ifBlank { content.posterUrl },
                contentDescription = content.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = (1f - scroll.value / 900f).coerceIn(0f, 1f)
                        translationY = scroll.value * 0.3f
                    },
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, ZinemaColors.Background))),
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = content.title,
                color = ZinemaColors.OnBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                content.year?.let { Text(text = it.toString(), color = ZinemaColors.TextSecondary, fontSize = 13.sp) }
                content.rating?.let { RatingBadge(rating = it) }
                Text(text = typeLabel(content.type), color = ZinemaColors.TextSecondary, fontSize = 13.sp)
            }

            if (content.genres.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    content.genres.forEach { GenreChip(genre = it) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        viewModel.onPlayInitiated()
                        val (season, episode) = playTarget(content, resume, selectedSeason, episodes)
                        onPlayClick(content.id, season, episode)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZinemaColors.Primary),
                ) {
                    Text(text = "▶  Play", color = ZinemaColors.OnBackground, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = viewModel::toggleWatchlist) {
                    Text(
                        text = if (isInWatchlist) "✓" else "+",
                        color = ZinemaColors.OnBackground,
                        fontSize = 22.sp,
                    )
                }
                IconButton(onClick = { onShareClick(content) }) {
                    Text(text = "⤴", color = ZinemaColors.OnBackground, fontSize = 20.sp)
                }
            }

            if (content.description.isNotBlank()) {
                Text(
                    text = content.description,
                    color = ZinemaColors.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = if (descExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { descExpanded = !descExpanded },
                )
            }
        }

        if (isSeries) {
            EpisodeListSection(
                seasons = detail.seasons,
                selectedSeason = selectedSeason,
                onSeasonSelected = viewModel::selectSeason,
                episodes = episodes,
                resume = resume,
                onEpisodeClick = { season, episode -> onPlayClick(content.id, season, episode) },
            )
        }

        if (detail.related.isNotEmpty()) {
            ContentRail(title = "More Like This", items = detail.related, onItemClick = onItemClick)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun typeLabel(type: ContentType): String = when (type) {
    ContentType.MOVIE -> "Movie"
    ContentType.TV -> "TV Series"
    ContentType.ANIME -> "Anime"
    ContentType.SHORT -> "Short"
    ContentType.SPORTS -> "Sports"
}

private fun playTarget(
    content: Content,
    resume: PlaybackPosition?,
    selectedSeason: Int,
    episodes: List<Episode>,
): Pair<Int, Int> {
    if (resume != null) return resume.seasonIndex to resume.episodeIndex
    val isSeries = content.type == ContentType.TV || content.type == ContentType.ANIME
    return if (isSeries) {
        selectedSeason to (episodes.firstOrNull()?.episodeIndex ?: 1)
    } else {
        0 to 0
    }
}
