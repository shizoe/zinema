package com.zinema.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response model for `wefeed-mobile-bff/subject-api/get` (blueprint §5.3).
 * Reuses [CoverImage] / [SubjectItem] from TabOperatingResponse.kt.
 */
@Serializable
data class SubjectDetail(
    val subjectId: String = "",
    val title: String = "",
    val subjectType: Int = 1,
    val cover: CoverImage? = null,
    val preVideoCover: CoverImage? = null,
    val description: String? = null,
    val genre: String? = null,
    val genres: List<String>? = null,
    val releaseDate: String? = null,
    val year: Int? = null,
    val imdbRatingValue: String? = null,
    val totalEpisode: Int? = null,
    val totalSeason: Int? = null,
    val seasons: List<SeasonInfo>? = null,
    val episodes: List<EpisodeInfo>? = null,
    val trailerUrl: String? = null,
    val relatedSubjects: List<SubjectItem>? = null,
)

@Serializable
data class SeasonInfo(
    val seasonIndex: Int = 1,
    val seasonTitle: String = "",
    val totalEpisode: Int = 0,
)

@Serializable
data class EpisodeInfo(
    val ep: Int = 0,
    val se: Int = 1,
    val title: String = "",
    val cover: CoverImage? = null,
    val durationMs: Long? = null,
)
