package com.zinema.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Common envelope returned by every wefeed-mobile-bff endpoint (blueprint §5.1).
 * [data] is null on error responses; [code] / [msg] carry the server status.
 */
@Serializable
data class ApiResponse<T>(
    val code: Int = 0,
    val msg: String = "",
    val data: T? = null,
)
