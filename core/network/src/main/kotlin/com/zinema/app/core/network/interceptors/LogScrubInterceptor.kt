package com.zinema.app.core.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Log-scrubbing hook (blueprint T-019).
 *
 * This interceptor is intentionally a pass-through and never logs anything
 * itself. The actual redaction of sensitive header *values* is enforced in
 * [com.zinema.app.core.network.NetworkModule] by calling
 * `HttpLoggingInterceptor.redactHeader(...)` for every entry in
 * [SENSITIVE_HEADERS]. Keeping the list here gives a single source of truth and
 * a place to add request/response body scrubbing later.
 *
 * Logging policy:
 *  - DEBUG: BASIC level logging, with the headers below redacted.
 *  - RELEASE: HttpLoggingInterceptor is set to Level.NONE (nothing logged).
 */
class LogScrubInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request())

    companion object {
        /** Header values that must never appear in logs. */
        val SENSITIVE_HEADERS: List<String> = listOf(
            "Authorization",
            "x-tr-signature",
            "Cookie",
            "Set-Cookie",
        )
    }
}
