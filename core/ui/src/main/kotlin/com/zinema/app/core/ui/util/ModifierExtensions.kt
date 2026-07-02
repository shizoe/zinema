package com.zinema.app.core.ui.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * Animated shimmer fill for loading placeholders (blueprint §10.3 / ShimmerRail).
 * Uses an [androidx.compose.animation.core.InfiniteTransition] sweeping a linear
 * gradient between [ZinemaColors.Shimmer] and [ZinemaColors.ShimmerHighlight].
 */
fun Modifier.shimmerBackground(shape: Shape = RectangleShape): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-translate",
    )
    val colors = listOf(
        ZinemaColors.Shimmer,
        ZinemaColors.ShimmerHighlight,
        ZinemaColors.Shimmer,
    )
    background(
        brush = Brush.linearGradient(
            colors = colors,
            start = Offset(translate - 400f, 0f),
            end = Offset(translate, 0f),
        ),
        shape = shape,
    )
}
