package com.zinema.app.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * Quality picker (blueprint T-052). Lists the resolutions available on the current
 * stream; selecting one re-resolves at that quality via the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelector(
    qualities: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ZinemaColors.Surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quality",
                color = ZinemaColors.OnBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            qualities.forEach { quality ->
                Text(
                    text = "${quality}p",
                    color = if (quality == selected) ZinemaColors.Primary else ZinemaColors.OnSurface,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(quality)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}
