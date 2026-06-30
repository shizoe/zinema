package com.zinema.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response model for `wefeed-mobile-bff/app/config` (blueprint §5.5).
 * [serverTimestamp] drives clock-skew correction (OQ-01); [signKeyVersion]
 * is checked against the pinned signing key version (OQ-03).
 */
@Serializable
data class AppConfigData(
    val serverTimestamp: Long = 0L,
    val signKeyVersion: Int = 2,
    val tabs: List<TabConfig>? = null,
)

@Serializable
data class TabConfig(
    val tabId: Int = 0,
    val tabName: String = "",
    val tabType: String = "API",         // "API" | "H5"
    val h5Url: String? = null,
    val sortOrder: Int = 0,
)
