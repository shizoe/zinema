package com.zinema.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zinema.app.core.ui.theme.ZinemaColors

/** A pill showing a single genre label. */
@Composable
fun GenreChip(genre: String, modifier: Modifier = Modifier) {
    Text(
        text = genre,
        color = ZinemaColors.OnSurface,
        fontSize = 11.sp,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(ZinemaColors.SurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
