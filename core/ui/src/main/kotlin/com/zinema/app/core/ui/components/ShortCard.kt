package com.zinema.app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.AsyncImage
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.ui.util.LocalIsTv
import com.zinema.app.core.ui.util.shimmerBackground
import androidx.compose.material3.Card as M3Card
import androidx.tv.material3.Card as TvCard

/** 9:16 vertical card for ShortTV (blueprint §10.3 / T-032). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ShortCard(
    content: Content,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTv.current
    val cardModifier = modifier.width(if (isTv) 160.dp else 120.dp)

    if (isTv) {
        TvCard(onClick = onClick, modifier = cardModifier) { ShortPoster(content) }
    } else {
        M3Card(onClick = onClick, shape = RoundedCornerShape(4.dp), modifier = cardModifier) {
            ShortPoster(content)
        }
    }
}

@Composable
private fun ShortPoster(content: Content) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(4.dp))
            .shimmerBackground(RoundedCornerShape(4.dp)),
    ) {
        AsyncImage(
            model = content.posterUrl,
            contentDescription = content.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
