package com.zinema.app.core.data.repositories

import com.zinema.app.core.domain.repository.AuthRepository
import com.zinema.app.core.network.ApiService
import com.zinema.app.core.network.dto.LoginRequestBody
import com.zinema.app.core.security.TokenStorage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Session-backed auth (blueprint T-037), wrapping the encrypted [TokenStorage]. */
class AuthRepositoryImpl @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val api: ApiService,
) : AuthRepository {

    override suspend fun loginAsGuest(guestToken: String) {
        tokenStorage.saveToken(guestToken)
        tokenStorage.setGuest(true)
    }

    override suspend fun loginWithCredentials(email: String, password: String): String {
        // Email + password login: authType = 1 (email), type = 1 (password).
        val response = api.login(
            LoginRequestBody(mail = email, password = password, type = 1, authType = 1),
        )
        val token = response.data?.token?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(response.msg.ifBlank { "Invalid email or password." })
        tokenStorage.saveToken(token)
        tokenStorage.setGuest(false)
        return token
    }

    override fun isLoggedIn(): Boolean = tokenStorage.getToken().isNotBlank()

    override suspend fun logout() {
        tokenStorage.clearToken()
    }

    override val sessionExpiredEvents: Flow<Unit> = tokenStorage.tokenExpiredEvents
}
