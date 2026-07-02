package com.zinema.app.core.domain.session

/** Coarse authentication state derived from the stored token (client-side). */
enum class AuthState {
    /** No token stored — show login. */
    LOGGED_OUT,

    /** Guest token, healthy. */
    GUEST_VALID,

    /** Guest token within the expiry window — prompt the user to log in. */
    GUEST_EXPIRING,

    /** Real (credential) token, healthy. */
    LOGGED_IN,

    /** Token rejected (401) or past its `exp` — force re-auth. */
    EXPIRED,
}
