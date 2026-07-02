package com.zinema.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zinema.app.core.ui.util.LocalIsTv
import com.zinema.app.core.ui.util.shimmerBackground

/** Loading skeleton: a title bar + five poster placeholders (blueprint §10.3 / T-035). */
@Composable
fun ShimmerRail(modifier: Modifier = Modifier) {
    val cardWidth = if (LocalIsTv.current) 160.dp else 110.dp
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .width(140.dp)
                .height(18.dp)
                .shimmerBackground(RoundedCornerShape(4.dp)),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .aspectRatio(2f / 3f)
                        .shimmerBackground(RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}
