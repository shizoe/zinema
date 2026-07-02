package com.zinema.app.core.data.mappers

import com.zinema.app.core.domain.model.ContentTab
import com.zinema.app.core.network.dto.BottomTabData

/**
 * Maps the server bottom-tab config to the app's content categories.
 *
 * Uses the HOME bottom tab's subTabs, keeping only real content tabs: displayType
 * REDIRECT and no external `url` (which drops Live/H5 and Novel/miniapp tabs).
 */
fun BottomTabData.toContentTabs(): List<ContentTab> {
    val home = bottomTabs.firstOrNull { it.btTabType == "BT_HOME" || it.btTabCode.equals("HOME", ignoreCase = true) }
        ?: bottomTabs.firstOrNull()
    return home?.subTabs.orEmpty()
        .filter { it.tabId > 0 && it.displayType == "REDIRECT" && it.url.isBlank() }
        .map {
            ContentTab(
                tabId = it.tabId,
                displayName = it.name,
                keyword = it.tabCode,
                isVisible = true,
            )
        }
}
