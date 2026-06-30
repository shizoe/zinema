package com.zinema.app.core.domain.analytics

/**
 * Crash-reporting port (blueprint T-059). Wraps Firebase Crashlytics in the app
 * module. Do not pass PII (tokens, raw URLs, device ids) as keys or values.
 */
interface CrashReporter {
    fun setCustomKey(key: String, value: String)
    fun recordException(throwable: Throwable)
}
