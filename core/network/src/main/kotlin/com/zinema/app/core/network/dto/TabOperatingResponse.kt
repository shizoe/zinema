package com.zinema.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response model for `wefeed-mobile-bff/tab-operating` (blueprint §5.2).
 * Also defines [SubjectItem] / [CoverImage], reused by the detail response.
 */
@Serializable
data class TabOperatingData(
    val version: String = "",
    val items: List<ContentBlock> = emptyList(),
    val hasMore: Boolean = false,
)

@Serializable
data class ContentBlock(
    val type: String = "",               // BANNER | SUBJECTS_MOVIE | CUSTOM | SPORT_LIVE | APPOINTMENT_LIST
    val banner: BannerBlock? = null,
    val subjects: List<SubjectItem> = emptyList(),
    val customData: CustomBlock? = null,
)

@Serializable
data class BannerBlock(
    val banners: List<SubjectItem> = emptyList(),
)

@Serializable
data class CustomBlock(
    val items: List<CustomItem> = emptyList(),
)

@Serializable
data class CustomItem(
    val subject: SubjectItem? = null,
)

@Serializable
data class SubjectItem(
    val subjectId: String = "",
    val title: String = "",
    val subjectType: Int = 1,            // 1=Movie, 2=Anime, 5=TV, 7=Short, 9=Sports
    val cover: CoverImage? = null,
    val preVideoCover: CoverImage? = null,
    val imdbRatingValue: String? = null,
    val genre: String? = null,           // comma-separated: "Action, Comedy"
    val genres: List<String>? = null,
    val releaseDate: String? = null,
    val year: Int? = null,
    val description: String? = null,
    val totalEpisode: Int? = null,
    val totalSeason: Int? = null,
    val averageHueLight: String? = null,
    val averageHueDark: String? = null,
)

@Serializable
data class CoverImage(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
)
