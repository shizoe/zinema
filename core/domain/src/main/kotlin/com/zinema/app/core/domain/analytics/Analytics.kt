package com.zinema.app.core.domain.analytics

/**
 * Analytics port (blueprint T-057). The implementation wraps Firebase Analytics in
 * the app module.
 *
 * PII guard: every parameter here is a stable id, content type, count, or a
 * pre-hashed value — never a raw URL, JWT, or device id.
 */
interface Analytics {
    fun trackAppOpen()
    fun trackTabViewed(tabId: Int, tabName: String)
    fun trackContentImpressed(subjectId: String, contentType: String, positionInFeed: Int)
    fun trackPlayInitiated(subjectId: String, contentType: String)
    fun trackPlaybackStarted(ttffMs: Long)
    fun trackBufferEvent(buffering: Boolean)
    fun trackApiRetry(endpoint: String)
    fun trackAssetBlocked(urlHash: String)
}
