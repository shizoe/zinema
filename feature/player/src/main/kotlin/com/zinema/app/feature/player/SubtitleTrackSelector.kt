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
import com.zinema.app.core.domain.model.SubtitleTrack
import com.zinema.app.core.ui.theme.ZinemaColors

/**
 * Subtitle picker (blueprint T-051): an always-present "Off" option plus the
 * stream's tracks. Selection is persisted via DataStore in the ViewModel.
 *
 * Uses a [ModalBottomSheet] for both platforms (the blueprint specifies a Dialog
 * on TV — see PHASE-7 §Deviations).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleTrackSelector(
    tracks: List<SubtitleTrack>,
    selectedLanguage: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ZinemaColors.Surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Subtitles",
                color = ZinemaColors.OnBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SubtitleRow(
                label = "Off",
                selected = selectedLanguage == null,
                onClick = {
                    onSelect(null)
                    onDismiss()
                },
            )
            tracks.forEach { track ->
                SubtitleRow(
                    label = track.language.ifBlank { track.languageCode },
                    selected = selectedLanguage == track.languageCode,
                    onClick = {
                        onSelect(track.languageCode)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun SubtitleRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) ZinemaColors.Primary else ZinemaColors.OnSurface,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}
