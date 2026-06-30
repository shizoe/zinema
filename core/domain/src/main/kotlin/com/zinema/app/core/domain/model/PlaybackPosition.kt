package com.zinema.app.core.domain.model

/**
 * Resume point for a piece of content (domain mirror of the persisted
 * playback-position row).
 *
 * Not in blueprint §6, but required so the repository interface in core:domain
 * can return resume state without leaking the Room entity (PHASE-2 §Deviations).
 */
data class PlaybackPosition(
    val subjectId: String,
    val contentType: ContentType,
    val seasonIndex: Int,
    val episodeIndex: Int,
    val positionMs: Long,
    val totalDurationMs: Long,
)
