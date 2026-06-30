package com.zinema.app.core.domain.session

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-wide, mutable session flags read by the network layer and written by
 * the profile/auth feature.
 *
 * Lives in core:domain (pure Kotlin) so both core:network (ClientInfoInterceptor,
 * blueprint §8.4) and the feature modules can depend on it without a cycle.
 *
 * Hilt note: this module has no annotation processor, so the singleton binding is
 * declared with @Provides in NetworkModule rather than via an @Inject constructor.
 *
 * Backed by [AtomicBoolean] because interceptors run on OkHttp's dispatcher
 * threads while the UI writes from the main thread.
 */
class SessionState {

    private val _isKidsProfile = AtomicBoolean(false)

    /** True when the active profile is a Kids profile (drives x-family-mode / x-content-mode). */
    var isKidsProfile: Boolean
        get() = _isKidsProfile.get()
        set(value) = _isKidsProfile.set(value)
}
