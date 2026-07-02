package com.zinema.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Common envelope returned by every wefeed-mobile-bff endpoint.
 *
 * Confirmed from captured traffic: the server uses `code` / `message` / `data`
 * (the blueprint's `msg` is never populated — kept for safety). [data] is null on
 * error responses.
 */
@Serializable
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val msg: String = "",
    val data: T? = null,
) {
    /** Server-provided status text, from whichever field is populated. */
    val statusText: String get() = message.ifBlank { msg }
}
