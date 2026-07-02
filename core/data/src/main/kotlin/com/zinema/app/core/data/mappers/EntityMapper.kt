package com.zinema.app.core.data.mappers

import com.zinema.app.core.data.db.entities.PlaybackPositionEntity
import com.zinema.app.core.data.db.entities.WatchlistEntity
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentType
import com.zinema.app.core.domain.model.PlaybackPosition

/**
 * Entity → domain mappers (PHASE-2 §Deviations: not in §6.1, but needed for
 * Continue Watching and My List).
 */

private const val DEFAULT_PLACEHOLDER_COLOR = 0xFF1A1A2EL

private fun String.toContentTypeOrDefault(): ContentType =
    runCatching { ContentType.valueOf(this) }.getOrDefault(ContentType.MOVIE)

fun PlaybackPositionEntity.toDomain(): PlaybackPosition = PlaybackPosition(
    subjectId = subjectId,
    contentType = contentType.toContentTypeOrDefault(),
    seasonIndex = seasonIndex,
    episodeIndex = episodeIndex,
    positionMs = positionMs,
    totalDurationMs = totalDurationMs,
)

/**
 * Watchlist row → card-ready [Content]. Title + poster are persisted, so cards
 * render fully; richer fields (genres, description) are left empty.
 */
fun WatchlistEntity.toContent(): Content = Content(
    id = subjectId,
    title = title,
    description = "",
    posterUrl = posterUrl,
    backdropUrl = "",
    genres = emptyList(),
    year = null,
    rating = null,
    type = contentType.toContentTypeOrDefault(),
    totalSeasons = 0,
    totalEpisodes = 0,
    trailerUrl = null,
    placeholderColor = DEFAULT_PLACEHOLDER_COLOR,
)

/**
 * Continue-watching row → [Content]. NOTE: the playback entity stores no title or
 * poster, so those are empty here. Home will need to enrich these cards (join with
 * cached content or extend the entity) — see PHASE-2 §Known gaps.
 */
fun PlaybackPositionEntity.toContentStub(): Content = Content(
    id = subjectId,
    title = "",
    description = "",
    posterUrl = "",
    backdropUrl = "",
    genres = emptyList(),
    year = null,
    rating = null,
    type = contentType.toContentTypeOrDefault(),
    totalSeasons = 0,
    totalEpisodes = 0,
    trailerUrl = null,
    placeholderColor = DEFAULT_PLACEHOLDER_COLOR,
)
