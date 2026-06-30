package com.zinema.app.core.domain.model

/**
 * Platform-agnostic content item (blueprint §6). Pure Kotlin — no Android or
 * serialization annotations. [placeholderColor] is an ARGB Long parsed from the
 * API's averageHueDark, used as a poster background before the image loads.
 */
data class Content(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val genres: List<String>,
    val year: Int?,
    val rating: String?,
    val type: ContentType,
    val totalSeasons: Int,
    val totalEpisodes: Int,
    val trailerUrl: String?,
    val placeholderColor: Long,
)

enum class ContentType {
    MOVIE, TV, ANIME, SHORT, SPORTS
}
