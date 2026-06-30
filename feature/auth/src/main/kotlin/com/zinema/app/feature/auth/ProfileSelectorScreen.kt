package com.zinema.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zinema.app.core.domain.model.UserProfile
import com.zinema.app.core.ui.theme.ZinemaColors

private val AvatarColors = listOf(
    Color(0xFF5A3FC0), Color(0xFF1E88E5), Color(0xFF00897B),
    Color(0xFFE53935), Color(0xFFF4511E),
)

private const val MAX_PROFILES = 5

/**
 * Profile picker (blueprint T-039): up to 5 avatars, an Add Profile tile, a
 * Manage Profiles action, Kids badges, and a PIN gate for locked profiles.
 *
 * Stateless — profiles + actions are supplied by the caller (the nav host wires
 * the profile source and SessionState updates).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSelectorScreen(
    profiles: List<UserProfile>,
    onProfileSelected: (UserProfile) -> Unit,
    onAddProfile: () -> Unit,
    onManageProfiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pinProfile by remember { mutableStateOf<UserProfile?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZinemaColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.auth_choose_profile),
            color = ZinemaColors.OnBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            profiles.take(MAX_PROFILES).forEach { profile ->
                ProfileAvatar(
                    profile = profile,
                    onClick = {
                        if (profile.pin != null) pinProfile = profile else onProfileSelected(profile)
                    },
                )
            }
            if (profiles.size < MAX_PROFILES) {
                AddProfileTile(onClick = onAddProfile)
            }
        }

        TextButton(onClick = onManageProfiles, modifier = Modifier.padding(top = 32.dp)) {
            Text(text = stringResource(R.string.auth_manage_profiles), color = ZinemaColors.OnSurface)
        }
    }

    pinProfile?.let { profile ->
        PinDialog(
            expectedPin = profile.pin.orEmpty(),
            onDismiss = { pinProfile = null },
            onValid = {
                pinProfile = null
                onProfileSelected(profile)
            },
        )
    }
}

@Composable
private fun ProfileAvatar(profile: UserProfile, onClick: () -> Unit) {
    val color = AvatarColors[profile.avatarIndex.mod(AvatarColors.size)]
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
        ) {
            Text(
                text = profile.displayName.take(1).uppercase(),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = profile.displayName,
            color = ZinemaColors.OnSurface,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (profile.isKidsProfile) {
            Text(
                text = stringResource(R.string.auth_kids),
                color = ZinemaColors.RatingGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        } else if (profile.pin != null) {
            Text(text = "🔒", fontSize = 10.sp) // lock glyph
        }
    }
}

@Composable
private fun AddProfileTile(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ZinemaColors.SurfaceVariant)
                .clickable(onClick = onClick),
        ) {
            Text(text = "+", color = ZinemaColors.OnBackground, fontSize = 40.sp)
        }
        Text(
            text = stringResource(R.string.auth_add_profile),
            color = ZinemaColors.OnSurface,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun PinDialog(
    expectedPin: String,
    onDismiss: () -> Unit,
    onValid: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.auth_enter_pin)) },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it.filter(Char::isDigit).take(4)
                    error = false
                },
                singleLine = true,
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin == expectedPin) onValid() else error = true
            }) {
                Text(text = stringResource(R.string.auth_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.auth_cancel))
            }
        },
    )
}
