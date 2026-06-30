package com.zinema.app.core.domain.model

/**
 * Rich detail bundle for one subject (Phase 6). The lean [Content] model (§6)
 * carries no episodes/seasons/related, so the detail screen uses this wrapper,
 * populated from a single `subject-api/get` call (PHASE-6 §Deviations).
 *
 * [episodes] are for the season returned by the initial request; switching seasons
 * fetches fresh episodes via the repository.
 */
data class ContentDetail(
    val content: Content,
    val seasons: List<Int>,
    val episodes: List<Episode>,
    val related: List<Content>,
)
