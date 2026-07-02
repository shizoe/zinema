package com.zinema.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response for `wefeed-mobile-bff/subject-api/resource` — the per-season episode
 * list. Each [ResourceItem] is a single playable file, so a season with N episodes
 * at 2 resolutions yields 2N entries (deduped by season/episode in the mapper).
 * This is the ONLY source of episodes; `subject-api/get` carries none.
 */
@Serializable
data class ResourceData(
    val subjectId: String = "",
    val subjectType: Int = 1,
    val subjectTitle: String = "",
    val totalEpisode: Int = 0,
    val list: List<ResourceItem> = emptyList(),
    val pager: ResourcePager? = null,
)

@Serializable
data class ResourceItem(
    val episode: Int = 0,                // encoded season*100 + episode, e.g. 101
    val se: Int = 1,                     // season index
    val ep: Int = 0,                     // episode index within the season
    val title: String = "",
    val resourceId: String = "",
    val resolution: Int = 0,
    val duration: Long = 0,              // seconds
)

@Serializable
data class ResourcePager(
    val hasMore: Boolean = false,
    val page: String = "",
    val nextPage: String = "",
    val perPage: Int = 0,
    val totalCount: Int = 0,
)
