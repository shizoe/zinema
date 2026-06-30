package com.zinema.app.feature.home

import com.zinema.app.core.domain.model.ContentTab

/**
 * Static tab configuration (blueprint T-041, PRD §2.4). tabId=27 (Live) is
 * suppressed per OQ-06; it isn't in this list, but [SUPPRESSED_TAB_IDS] guards
 * against it appearing via server-provided tab metadata later.
 */
val CONTENT_TABS: List<ContentTab> = listOf(
    ContentTab(0, "Home", "popular", isVisible = true),
    ContentTab(1, "Trending", "trending", isVisible = true),
    ContentTab(2, "Movies", "movie", isVisible = true),
    ContentTab(5, "TV Shows", "tv", isVisible = true),
    ContentTab(8, "Anime", "anime", isVisible = true),
    ContentTab(13, "ShortTV", "short", isVisible = true),
    ContentTab(28, "Nollywood", "nollywood", isVisible = true),
    ContentTab(18, "Asian", "asian", isVisible = true),
    ContentTab(19, "Western", "western", isVisible = true),
    ContentTab(23, "Kids", "kids", isVisible = true),
    ContentTab(3, "Education", "education", isVisible = true),
    ContentTab(4, "Music", "music", isVisible = true),
    ContentTab(11, "Gaming", "game", isVisible = true),
    ContentTab(33, "Football", "football", isVisible = true),
    ContentTab(37, "FightZone", "fightzone", isVisible = true),
)

/** Tabs hidden from navigation entirely (OQ-06). */
val SUPPRESSED_TAB_IDS: Set<Int> = setOf(27)

/** The five tabs shown in the mobile bottom navigation bar (blueprint T-042). */
val BOTTOM_NAV_TAB_IDS: List<Int> = listOf(0, 2, 5, 1, 13) // Home, Movies, TV, Trending, ShortTV

/** A simple glyph per tab (avoids the material-icons dependency — see Phase 3). */
fun glyphForTab(tabId: Int): String = when (tabId) {
    0 -> "⌂"      // Home
    1 -> "🔥"     // Trending
    2 -> "🎬"     // Movies
    5 -> "📺"     // TV
    8 -> "★"      // Anime
    13 -> "⚡"    // ShortTV
    23 -> "🧸"    // Kids
    3 -> "🎓"     // Education
    4 -> "♪"      // Music
    11 -> "🎮"    // Gaming
    33 -> "⚽"    // Football
    37 -> "🥊"    // FightZone
    else -> "▦"
}
