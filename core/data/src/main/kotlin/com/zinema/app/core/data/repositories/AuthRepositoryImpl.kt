package com.zinema.app.core.data.repositories

import com.zinema.app.core.domain.repository.AuthRepository
import com.zinema.app.core.security.TokenStorage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Session-backed auth (blueprint T-037), wrapping the encrypted [TokenStorage]. */
class AuthRepositoryImpl @Inject constructor(
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    override suspend fun loginAsGuest(guestToken: String) {
        tokenStorage.saveToken(guestToken)
    }

    override suspend fun loginWithCredentials(email: String, password: String): String {
        // TODO: the OneID credential endpoint is not defined in the blueprint (§8.1).
        // Wire this to the real API once the contract is known.
        throw UnsupportedOperationException("Credential login is not yet available")
    }

    override fun isLoggedIn(): Boolean = tokenStorage.getToken().isNotBlank()

    override suspend fun logout() {
        tokenStorage.clearToken()
    }

    override val sessionExpiredEvents: Flow<Unit> = tokenStorage.tokenExpiredEvents
}
