package com.zinema.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.ui.theme.ZinemaColors
import com.zinema.app.core.ui.util.LocalIsTv

/**
 * Horizontal rail of poster cards (blueprint §10.3 / T-033): 8dp item spacing,
 * 16dp horizontal padding, and a trailing "See All" tile when there are ≥ 10 items.
 */
@Composable
fun ContentRail(
    title: String,
    items: List<Content>,
    onItemClick: (Content) -> Unit,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = ZinemaColors.OnBackground,
            modifier = Modifier.padding(start = 16.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(items = items, key = { it.id }) { content ->
                ContentCard(content = content, onClick = { onItemClick(content) })
            }
            if (items.size >= 10 && onSeeAll != null) {
                item { SeeAllTile(onClick = onSeeAll) }
            }
        }
    }
}

@Composable
private fun SeeAllTile(onClick: () -> Unit) {
    val width = if (LocalIsTv.current) 160.dp else 110.dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(4.dp))
            .background(ZinemaColors.SurfaceVariant)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Text(text = "See All", color = ZinemaColors.OnSurface)
    }
}
