package com.zinema.app.core.domain.model

/** A viewer profile (blueprint §6). [pin] is null for unlocked profiles. */
data class UserProfile(
    val id: String,
    val displayName: String,
    val avatarIndex: Int,
    val isKidsProfile: Boolean,
    val pin: String?,
)
