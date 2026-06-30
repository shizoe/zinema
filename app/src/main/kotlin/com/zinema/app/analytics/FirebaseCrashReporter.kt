package com.zinema.app.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.zinema.app.core.domain.analytics.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton

/** Firebase-backed [CrashReporter] (blueprint T-059). */
@Singleton
class FirebaseCrashReporter @Inject constructor() : CrashReporter {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }
}
