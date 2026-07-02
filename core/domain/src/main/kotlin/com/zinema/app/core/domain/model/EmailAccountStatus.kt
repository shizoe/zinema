package com.zinema.app.core.domain.model

/**
 * Result of checking an email against the backend (check-mail-account). Drives the
 * login branch: exists && hasPassword → password; exists && !hasPassword → SMS code;
 * !exists → register (also via the SMS-code path).
 */
data class EmailAccountStatus(
    val exists: Boolean,
    val hasPassword: Boolean,
)
