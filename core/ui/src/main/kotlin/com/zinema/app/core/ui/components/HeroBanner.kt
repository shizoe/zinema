package com.zinema.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.ui.theme.ZinemaColors
import kotlinx.coroutines.delay

/**
 * Auto-advancing hero carousel (blueprint §10.3 / T-034): 56% of screen height,
 * bottom gradient, Play + My List actions, and page-indicator dots.
 */
@Composable
fun HeroBanner(
    items: List<Content>,
    onPlayClick: (Content) -> Unit,
    onWatchlistClick: (Content) -> Unit,
    onItemClick: (Content) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(pagerState.currentPage, items.size) {
        if (items.size > 1) {
            delay(5_000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size)
        }
    }

    val heroHeight = (LocalConfiguration.current.screenHeightDp * 0.56f).dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val content = items[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onItemClick(content) },
            ) {
                AsyncImage(
                    model = content.backdropUrl.ifBlank { content.posterUrl },
                    contentDescription = content.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, ZinemaColors.Background)),
                        ),
                )
            }
        }

        val current = items[pagerState.currentPage]
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = current.title,
                style = androidx.compose.material3.MaterialTheme.typography.displayMedium,
                color = ZinemaColors.OnBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onPlayClick(current) },
                    colors = ButtonDefaults.buttonColors(containerColor = ZinemaColors.Primary),
                ) {
                    Text(text = "▶  Play", color = ZinemaColors.OnBackground, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = { onWatchlistClick(current) }) {
                    Text(text = "+  My List", color = ZinemaColors.OnBackground)
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(items.size) { index ->
                val active = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (active) Color.White else Color.White.copy(alpha = 0.4f)),
                )
            }
        }
    }
}
