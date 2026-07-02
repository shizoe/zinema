package com.zinema.app.core.data.session

import com.zinema.app.core.domain.session.AuthState
import com.zinema.app.core.domain.session.SessionManager
import com.zinema.app.core.security.JwtUtils
import com.zinema.app.core.security.TokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Computes [AuthState] from the stored token. Recomputes on token changes
 * (login/logout) and forces EXPIRED on a 401 ([TokenStorage.tokenExpiredEvents]).
 */
@Singleton
class SessionManagerImpl @Inject constructor(
    private val tokenStorage: TokenStorage,
    scope: CoroutineScope,
) : SessionManager {

    override val authState: StateFlow<AuthState> = merge(
        merge(flowOf(Unit), tokenStorage.tokenChangedEvents).map { compute() },
        tokenStorage.tokenExpiredEvents.map { AuthState.EXPIRED },
    ).stateIn(scope, SharingStarted.Eagerly, compute())

    private fun compute(): AuthState {
        val token = tokenStorage.getToken()
        if (token.isBlank()) return AuthState.LOGGED_OUT
        if (JwtUtils.isExpired(token)) return AuthState.EXPIRED
        return if (tokenStorage.isGuest()) {
            if (JwtUtils.isExpiringSoon(token)) AuthState.GUEST_EXPIRING else AuthState.GUEST_VALID
        } else {
            AuthState.LOGGED_IN
        }
    }
}
