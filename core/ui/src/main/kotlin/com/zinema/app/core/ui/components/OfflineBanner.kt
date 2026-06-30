package com.zinema.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zinema.app.core.ui.theme.ZinemaColors

/** Thin bar shown while the device is offline (observed by HomeScreen, Phase 5). */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Text(
        text = "You're offline. Showing saved content.",
        color = ZinemaColors.OnBackground,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(ZinemaColors.PrimaryVariant)
            .padding(vertical = 6.dp),
    )
}
