package com.zinema.app.core.network.interceptors

import com.zinema.app.core.security.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Injects the Bearer JWT and signals token expiry on 401 (blueprint §8.3).
 *
 * The actual re-auth flow lives in AuthViewModel, which observes
 * [TokenStorage.tokenExpiredEvents]; this interceptor only flips that signal.
 */
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStorage.getToken()
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(request)
        if (response.code == 401) {
            tokenStorage.markTokenExpired()
        }
        return response
    }
}
