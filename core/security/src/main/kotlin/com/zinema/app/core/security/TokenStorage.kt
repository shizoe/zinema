package com.zinema.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Encrypted store for the JWT and the server clock-skew offset (blueprint T-008).
 *
 * All values live in an [EncryptedSharedPreferences] file (AES-256). Token
 * expiry is surfaced as a hot [SharedFlow] that the auth layer observes to drive
 * re-authentication (see [markTokenExpired]).
 */
class TokenStorage(context: Context) {

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

    private val _tokenExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits whenever a 401 is observed; collected by AuthViewModel (blueprint §8.3). */
    val tokenExpiredEvents: SharedFlow<Unit> = _tokenExpiredEvents.asSharedFlow()

    fun getToken(): String = prefs.getString(KEY_TOKEN, "").orEmpty()

    fun saveToken(jwt: String) {
        prefs.edit().putString(KEY_TOKEN, jwt).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    /** Signals listeners that the current token was rejected. Non-blocking. */
    fun markTokenExpired() {
        _tokenExpiredEvents.tryEmit(Unit)
    }

    fun getServerTimeOffsetMs(): Long = prefs.getLong(KEY_SERVER_OFFSET, 0L)

    fun saveServerTimeOffsetMs(offset: Long) {
        prefs.edit().putLong(KEY_SERVER_OFFSET, offset).apply()
    }

    private companion object {
        const val PREFS_FILE = "zinema_token_prefs"
        const val KEY_TOKEN = "jwt_token"
        const val KEY_SERVER_OFFSET = "server_time_offset_ms"
    }
}
