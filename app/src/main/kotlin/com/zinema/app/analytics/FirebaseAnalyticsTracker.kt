package com.zinema.app.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.zinema.app.core.domain.analytics.Analytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Firebase-backed [Analytics] (blueprint T-057). Events mirror PRD §6.1. */
@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext context: Context,
) : Analytics {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun trackAppOpen() {
        firebaseAnalytics.logEvent("app_open", null)
    }

    override fun trackTabViewed(tabId: Int, tabName: String) {
        firebaseAnalytics.logEvent("tab_viewed", bundle {
            putLong("tab_id", tabId.toLong())
            putString("tab_name", tabName)
        })
    }

    override fun trackContentImpressed(subjectId: String, contentType: String, positionInFeed: Int) {
        firebaseAnalytics.logEvent("content_impressed", bundle {
            putString("subject_id", subjectId)
            putString("content_type", contentType)
            putLong("position_in_feed", positionInFeed.toLong())
        })
    }

    override fun trackPlayInitiated(subjectId: String, contentType: String) {
        firebaseAnalytics.logEvent("play_initiated", bundle {
            putString("subject_id", subjectId)
            putString("content_type", contentType)
        })
    }

    override fun trackPlaybackStarted(ttffMs: Long) {
        firebaseAnalytics.logEvent("playback_started", bundle { putLong("ttff_ms", ttffMs) })
    }

    override fun trackBufferEvent(buffering: Boolean) {
        firebaseAnalytics.logEvent(if (buffering) "buffer_start" else "buffer_end", null)
    }

    override fun trackApiRetry(endpoint: String) {
        firebaseAnalytics.logEvent("api_retry", bundle { putString("endpoint", endpoint) })
    }

    override fun trackAssetBlocked(urlHash: String) {
        firebaseAnalytics.logEvent("asset_blocked", bundle { putString("url_hash", urlHash) })
    }

    private inline fun bundle(block: Bundle.() -> Unit): Bundle = Bundle().apply(block)
}
