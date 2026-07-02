package com.zinema.app.core.data.repositories

import com.zinema.app.core.domain.model.EmailAccountStatus
import com.zinema.app.core.domain.repository.AuthRepository
import com.zinema.app.core.network.ApiService
import com.zinema.app.core.network.dto.CheckEmailBody
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

    override suspend fun checkEmail(email: String): EmailAccountStatus {
        val data = api.checkEmailExists(CheckEmailBody(mail = email)).data
        return EmailAccountStatus(
            exists = data?.exists ?: false,
            hasPassword = data?.hasPassword ?: false,
        )
    }

    override suspend fun sendEmailCode(email: String) {
        val response = api.getSmsCode(LoginRequestBody(mail = email, type = 1, authType = 1))
        if (response.code != 0) {
            throw IllegalStateException(response.statusText.ifBlank { "Couldn't send the code." })
        }
    }

    override suspend fun loginWithCode(email: String, code: String): String {
        val response = api.checkSmsCode(
            LoginRequestBody(mail = email, verificationCode = code, type = 1, authType = 1),
        )
        val token = response.data?.token?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(response.statusText.ifBlank { "Invalid or expired code." })
        tokenStorage.saveToken(token)
        tokenStorage.setGuest(false)
        return token
    }

    override suspend fun loginWithCredentials(email: String, password: String): String {
        // Email + password login: authType = 1 (email), type = 1 (password).
        val response = api.login(
            LoginRequestBody(mail = email, password = password, type = 1, authType = 1),
        )
        val token = response.data?.token?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(response.statusText.ifBlank { "Invalid email or password." })
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
