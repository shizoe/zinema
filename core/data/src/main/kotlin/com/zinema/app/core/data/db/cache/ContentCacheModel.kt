package com.zinema.app.core.data.db.cache

import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentType
import kotlinx.serialization.Serializable

/**
 * Serializable mirror of [Content] used to persist the tab cache as JSON
 * ([CachedTabEntity.contentJson]).
 *
 * The domain [Content] is deliberately annotation-free (blueprint §6), so this
 * @Serializable twin keeps serialization concerns in the data layer
 * (PHASE-2 §Deviations). [type] is stored as the enum name.
 */
@Serializable
data class ContentCacheModel(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val genres: List<String>,
    val year: Int? = null,
    val rating: String? = null,
    val type: String,
    val totalSeasons: Int,
    val totalEpisodes: Int,
    val trailerUrl: String? = null,
    val placeholderColor: Long,
)

fun Content.toCacheModel(): ContentCacheModel = ContentCacheModel(
    id = id,
    title = title,
    description = description,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    genres = genres,
    year = year,
    rating = rating,
    type = type.name,
    totalSeasons = totalSeasons,
    totalEpisodes = totalEpisodes,
    trailerUrl = trailerUrl,
    placeholderColor = placeholderColor,
)

fun ContentCacheModel.toContent(): Content = Content(
    id = id,
    title = title,
    description = description,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    genres = genres,
    year = year,
    rating = rating,
    type = runCatching { ContentType.valueOf(type) }.getOrDefault(ContentType.MOVIE),
    totalSeasons = totalSeasons,
    totalEpisodes = totalEpisodes,
    trailerUrl = trailerUrl,
    placeholderColor = placeholderColor,
)
