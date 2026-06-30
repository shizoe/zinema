package com.zinema.app.core.domain.model

/** A navigable content tab (Home, Movies, TV, …) — blueprint §6. */
data class ContentTab(
    val tabId: Int,
    val displayName: String,
    val keyword: String,
    val isVisible: Boolean,
)
