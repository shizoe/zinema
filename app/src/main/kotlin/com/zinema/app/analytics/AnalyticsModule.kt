package com.zinema.app.analytics

import com.zinema.app.core.domain.analytics.Analytics
import com.zinema.app.core.domain.analytics.CrashReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the Firebase analytics/crash implementations app-wide (blueprint T-057/T-059). */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalytics(impl: FirebaseAnalyticsTracker): Analytics

    @Binds
    @Singleton
    abstract fun bindCrashReporter(impl: FirebaseCrashReporter): CrashReporter
}
