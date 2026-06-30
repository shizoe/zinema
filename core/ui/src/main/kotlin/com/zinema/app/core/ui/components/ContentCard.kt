package com.zinema.app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

/**
 * 2:3 poster card (blueprint §10.3 / T-031). TV uses a tv-material card for
 * built-in D-pad focus scaling; mobile uses a Material3 card. Width 110dp mobile
 * / 160dp TV, 4dp corners, rating badge bottom-start.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContentCard(
    content: Content,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTv.current
    val cardModifier = modifier.width(if (isTv) 160.dp else 110.dp)

    if (isTv) {
        TvCard(onClick = onClick, modifier = cardModifier) {
            PosterImage(content = content)
        }
    } else {
        M3Card(onClick = onClick, shape = RoundedCornerShape(4.dp), modifier = cardModifier) {
            PosterImage(content = content)
        }
    }
}

@Composable
private fun PosterImage(content: Content) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(4.dp))
            .shimmerBackground(RoundedCornerShape(4.dp)),
    ) {
        AsyncImage(
            model = content.posterUrl,
            contentDescription = content.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        content.rating?.let { rating ->
            RatingBadge(
                rating = rating,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
            )
        }
    }
}
