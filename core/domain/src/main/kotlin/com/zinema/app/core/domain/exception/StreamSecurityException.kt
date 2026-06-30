package com.zinema.app.core.domain.exception

/**
 * Thrown when a resolved stream URL is not served by an allowlisted streaming CDN
 * host (blueprint T-026). Caught by the player layer, which surfaces an error
 * state and reports to Crashlytics.
 */
class StreamSecurityException(message: String) : Exception(message)
