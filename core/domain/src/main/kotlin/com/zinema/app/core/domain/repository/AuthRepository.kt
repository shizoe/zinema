package com.zinema.app.core.domain.repository

import com.zinema.app.core.domain.model.EmailAccountStatus
import kotlinx.coroutines.flow.Flow

/**
 * Authentication / session state (blueprint T-037).
 *
 * Introduced so the auth feature does not depend on core:security directly: the
 * implementation (core:data) wraps TokenStorage, keeping feature → domain → data
 * layering intact (PHASE-4 §Deviations).
 */
interface AuthRepository {

    /** Persists a guest JWT, signing the user in as a guest. */
    suspend fun loginAsGuest(guestToken: String)

    /** Checks whether an email account exists and whether it has a password. */
    suspend fun checkEmail(email: String): EmailAccountStatus

    /** Sends an email verification code (get-sms-code). */
    suspend fun sendEmailCode(email: String)

    /** Verifies an email code (check-sms-code) and returns the issued token. */
    suspend fun loginWithCode(email: String, code: String): String

    /** Password login via user-api/login; returns the issued token. */
    suspend fun loginWithCredentials(email: String, password: String): String

    fun isLoggedIn(): Boolean

    suspend fun logout()

    /** Emits when the stored token is rejected (HTTP 401) so the UI can re-auth. */
    val sessionExpiredEvents: Flow<Unit>
}
