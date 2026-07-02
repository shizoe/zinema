package com.zinema.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response for `wefeed-mobile-bff/subject-api/bottom-tab` (confirmed from captured
 * traffic). The HOME bottom tab's [BottomTab.subTabs] are the horizontal content
 * categories; external tabs (Live/Novel) carry a non-empty [SubTab.url] or a
 * non-REDIRECT displayType and are filtered out when mapping to domain.
 */
@Serializable
data class BottomTabData(
    val bottomTabs: List<BottomTab> = emptyList(),
    val version: String = "",
)

@Serializable
data class BottomTab(
    val btTabCode: String = "",
    val btTabType: String = "",
    val subTabs: List<SubTab> = emptyList(),
)

@Serializable
data class SubTab(
    val tabId: Int = 0,
    val name: String = "",
    val type: String = "",
    val tabCode: String = "",
    val url: String = "",
    val displayType: String = "",
)
