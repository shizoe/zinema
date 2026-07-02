package com.zinema.app.core.domain.session

import kotlinx.coroutines.flow.StateFlow

/**
 * Reactive [AuthState] derived from the stored token. The UI observes this to force
 * re-auth on expiry and to warn when a guest token is expiring soon.
 */
interface SessionManager {
    val authState: StateFlow<AuthState>
}
