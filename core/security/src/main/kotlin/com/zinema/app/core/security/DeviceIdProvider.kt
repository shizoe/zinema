package com.zinema.app.core.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

/**
 * Stable per-install device id = SHA-256(ANDROID_ID), hashed once and cached in
 * an encrypted store (blueprint OQ-02 / T-009).
 *
 * ANDROID_ID is read only on the first call; subsequent calls return the cached
 * hash so the value survives even if ANDROID_ID were to change.
 */
class DeviceIdProvider(private val context: Context) {

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

    fun getHashedDeviceId(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }

        val androidId = readAndroidId()
        val hashed = sha256Hex(androidId)
        prefs.edit().putString(KEY_DEVICE_ID, hashed).apply()
        return hashed
    }

    @SuppressLint("HardwareIds")
    private fun readAndroidId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val PREFS_FILE = "zinema_device_prefs"
        const val KEY_DEVICE_ID = "hashed_device_id"
    }
}
