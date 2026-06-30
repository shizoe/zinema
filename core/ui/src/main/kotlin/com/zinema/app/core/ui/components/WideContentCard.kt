package com.zinema.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.AsyncImage
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.ui.theme.ZinemaColors
import com.zinema.app.core.ui.util.LocalIsTv
import com.zinema.app.core.ui.util.shimmerBackground
import androidx.compose.material3.Card as M3Card
import androidx.tv.material3.Card as TvCard

/**
 * 16:9 thumbnail card with a bottom-gradient title overlay (blueprint §10.3 /
 * T-032). Width 200dp mobile / 280dp TV.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WideContentCard(
    content: Content,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalIsTv.current
    val cardModifier = modifier.width(if (isTv) 280.dp else 200.dp)

    if (isTv) {
        TvCard(onClick = onClick, modifier = cardModifier) { WideThumbnail(content) }
    } else {
        M3Card(onClick = onClick, shape = RoundedCornerShape(4.dp), modifier = cardModifier) {
            WideThumbnail(content)
        }
    }
}

@Composable
private fun WideThumbnail(content: Content) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(4.dp))
            .shimmerBackground(RoundedCornerShape(4.dp)),
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
                    Brush.verticalGradient(
                        listOf(androidx.compose.ui.graphics.Color.Transparent, ZinemaColors.Overlay),
                    ),
                ),
        )
        Text(
            text = content.title,
            color = ZinemaColors.OnBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}
