package com.zinema.app.core.data.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.zinema.app.core.domain.model.UserProfile
import com.zinema.app.core.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

/**
 * [ProfileRepository] backed by [EncryptedSharedPreferences] + JSON. PINs are
 * stored as SHA-256 hashes (the raw PIN is never persisted).
 */
class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : ProfileRepository {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(StoredProfile.serializer())

    override suspend fun getProfiles(): List<UserProfile> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw).map { it.toDomain() } }
            .getOrDefault(emptyList())
    }

    override suspend fun addProfile(displayName: String, avatarIndex: Int, isKids: Boolean, pin: String?) {
        val updated = loadStored() + StoredProfile(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            avatarIndex = avatarIndex,
            isKidsProfile = isKids,
            pinHash = pin?.let { sha256(it) },
        )
        prefs.edit().putString(KEY_PROFILES, json.encodeToString(serializer, updated)).apply()
    }

    override suspend fun setActiveProfileId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    override suspend fun getActiveProfileId(): String? = prefs.getString(KEY_ACTIVE_ID, null)

    override fun verifyPin(profile: UserProfile, enteredPin: String): Boolean {
        val storedHash = profile.pin ?: return true
        return storedHash == sha256(enteredPin)
    }

    private fun loadStored(): List<StoredProfile> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val PREFS_FILE = "zinema_profiles"
        const val KEY_PROFILES = "profiles"
        const val KEY_ACTIVE_ID = "active_profile_id"
    }
}

/** On-disk profile (PIN as hash). [UserProfile.pin] carries the hash once loaded. */
@Serializable
private data class StoredProfile(
    val id: String,
    val displayName: String,
    val avatarIndex: Int,
    val isKidsProfile: Boolean,
    val pinHash: String? = null,
)

private fun StoredProfile.toDomain(): UserProfile = UserProfile(
    id = id,
    displayName = displayName,
    avatarIndex = avatarIndex,
    isKidsProfile = isKidsProfile,
    pin = pinHash,
)
