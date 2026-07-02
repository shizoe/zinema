package com.zinema.app.core.domain.repository

import com.zinema.app.core.domain.model.UserProfile

/**
 * Viewer-profile persistence. PINs are stored hashed (never raw); [verifyPin]
 * compares a hash of the entered PIN.
 */
interface ProfileRepository {

    suspend fun getProfiles(): List<UserProfile>

    /** Creates a profile (id generated, PIN hashed) and persists it. */
    suspend fun addProfile(displayName: String, avatarIndex: Int, isKids: Boolean, pin: String?)

    suspend fun setActiveProfileId(id: String)

    suspend fun getActiveProfileId(): String?

    /** True if [profile] has no PIN, or the entered PIN matches the stored hash. */
    fun verifyPin(profile: UserProfile, enteredPin: String): Boolean
}
