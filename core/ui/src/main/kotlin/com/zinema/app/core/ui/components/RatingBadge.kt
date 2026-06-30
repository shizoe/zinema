package com.zinema.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zinema.app.core.ui.theme.ZinemaColors

/** IMDb-style rating chip (blueprint §10.3): gold text on a dark overlay. */
@Composable
fun RatingBadge(rating: String, modifier: Modifier = Modifier) {
    Text(
        text = "★ $rating",
        color = ZinemaColors.RatingGold,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(ZinemaColors.Overlay)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}
