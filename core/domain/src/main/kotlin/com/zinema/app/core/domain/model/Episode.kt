package com.zinema.app.core.domain.model

/** A single episode of a TV/anime series (blueprint §6). */
data class Episode(
    val seasonIndex: Int,
    val episodeIndex: Int,
    val title: String,
    val thumbnailUrl: String,
    val durationMs: Long,
)
