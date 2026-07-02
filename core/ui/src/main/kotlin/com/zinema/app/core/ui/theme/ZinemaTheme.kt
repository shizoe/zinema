package com.zinema.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.zinema.app.core.ui.util.LocalIsTv
import com.zinema.app.core.ui.util.isTvDevice
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.darkColorScheme as tvDarkColorScheme

/**
 * App theme (blueprint T-030). Provides both the Material3 theme (mobile chrome)
 * and the tv-material theme (TV chrome) so shared components can use whichever
 * matches the platform, and exposes [LocalIsTv] for branching.
 */
@Composable
fun ZinemaTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isTv = remember(context) { context.isTvDevice() }

    val m3Colors = darkColorScheme(
        primary = ZinemaColors.Primary,
        onPrimary = ZinemaColors.OnBackground,
        background = ZinemaColors.Background,
        onBackground = ZinemaColors.OnBackground,
        surface = ZinemaColors.Surface,
        onSurface = ZinemaColors.OnSurface,
        surfaceVariant = ZinemaColors.SurfaceVariant,
    )

    val tvColors = tvDarkColorScheme(
        primary = ZinemaColors.Primary,
        onPrimary = ZinemaColors.OnBackground,
        background = ZinemaColors.Background,
        onBackground = ZinemaColors.OnBackground,
        surface = ZinemaColors.Surface,
        onSurface = ZinemaColors.OnSurface,
    )

    CompositionLocalProvider(LocalIsTv provides isTv) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography = ZinemaTypography,
            shapes = ZinemaShapes,
        ) {
            TvMaterialTheme(colorScheme = tvColors) {
                content()
            }
        }
    }
}
