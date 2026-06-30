package com.zinema.app.core.data.mappers

import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentDetail
import com.zinema.app.core.domain.model.ContentType
import com.zinema.app.core.domain.model.Episode
import com.zinema.app.core.domain.model.StreamInfo
import com.zinema.app.core.domain.model.StreamProtocol
import com.zinema.app.core.domain.model.SubtitleTrack
import com.zinema.app.core.network.cdn.CdnValidator
import com.zinema.app.core.network.dto.EpisodeInfo
import com.zinema.app.core.network.dto.PlayInfoData
import com.zinema.app.core.network.dto.SubjectDetail
import com.zinema.app.core.network.dto.SubjectItem
import com.zinema.app.core.network.dto.SubtitleDto

/** DTO → domain mappers (blueprint §6.1). */

private const val DEFAULT_PLACEHOLDER_COLOR = 0xFF1A1A2EL

private fun Int.toContentType(): ContentType = when (this) {
    1 -> ContentType.MOVIE
    2 -> ContentType.ANIME
    5 -> ContentType.TV
    7 -> ContentType.SHORT
    9 -> ContentType.SPORTS
    else -> ContentType.MOVIE
}

private fun parseGenres(genre: String?, genres: List<String>?): List<String> =
    genre?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        ?: genres
        ?: emptyList()

fun SubjectItem.toDomain(): Content = Content(
    id = subjectId,
    title = title,
    description = description ?: "",
    posterUrl = cover?.url?.takeIf { CdnValidator.isAllowed(it) } ?: "",
    backdropUrl = preVideoCover?.url?.takeIf { CdnValidator.isAllowed(it) } ?: "",
    genres = parseGenres(genre, genres),
    year = year,
    rating = imdbRatingValue?.takeIf { it != "0" && it != "0.0" },
    type = subjectType.toContentType(),
    totalSeasons = totalSeason ?: 0,
    totalEpisodes = totalEpisode ?: 0,
    trailerUrl = null,
    placeholderColor = averageHueDark?.parseHexColor() ?: DEFAULT_PLACEHOLDER_COLOR,
)

fun SubjectDetail.toDomain(): Content = Content(
    id = subjectId,
    title = title,
    description = description ?: "",
    posterUrl = cover?.url?.takeIf { CdnValidator.isAllowed(it) } ?: "",
    backdropUrl = preVideoCover?.url?.takeIf { CdnValidator.isAllowed(it) } ?: "",
    genres = parseGenres(genre, genres),
    year = year,
    rating = imdbRatingValue?.takeIf { it != "0" && it != "0.0" },
    type = subjectType.toContentType(),
    totalSeasons = totalSeason ?: (seasons?.size ?: 0),
    totalEpisodes = totalEpisode ?: 0,
    trailerUrl = trailerUrl,
    placeholderColor = DEFAULT_PLACEHOLDER_COLOR,
)

fun SubjectDetail.toContentDetail(): ContentDetail = ContentDetail(
    content = toDomain(),
    seasons = when {
        !seasons.isNullOrEmpty() -> seasons!!.map { it.seasonIndex }
        (totalSeason ?: 0) > 0 -> (1..totalSeason!!).toList()
        else -> emptyList()
    },
    episodes = episodes?.map { it.toDomain() } ?: emptyList(),
    related = relatedSubjects?.map { it.toDomain() }?.filter { it.id.isNotBlank() } ?: emptyList(),
)

fun EpisodeInfo.toDomain(): Episode = Episode(
    seasonIndex = se,
    episodeIndex = ep,
    title = title,
    thumbnailUrl = cover?.url?.takeIf { CdnValidator.isAllowed(it) } ?: "",
    durationMs = durationMs ?: 0L,
)

fun SubtitleDto.toDomain(): SubtitleTrack = SubtitleTrack(
    language = language,
    languageCode = languageCode,
    url = url,
    format = format,
)

fun PlayInfoData.toDomain(preferredQuality: String = "1080"): StreamInfo {
    val stream = streams.firstOrNull { it.resolutions == preferredQuality }
        ?: streams.lastOrNull()
        ?: throw IllegalStateException("No streams available")
    val cookies = stream.signCookie.parseCloudfrontCookies()
    return StreamInfo(
        streamUrl = stream.url,
        quality = stream.resolutions,
        streamProtocol = when {
            stream.url.endsWith(".mpd") -> StreamProtocol.DASH
            stream.url.endsWith(".m3u8") -> StreamProtocol.HLS
            else -> StreamProtocol.PROGRESSIVE_MP4
        },
        cloudFrontPolicy = cookies["CloudFront-Policy"] ?: "",
        cloudFrontSignature = cookies["CloudFront-Signature"] ?: "",
        cloudFrontKeyPairId = cookies["CloudFront-Key-Pair-Id"] ?: "",
        subtitles = subtitles?.map { it.toDomain() } ?: emptyList(),
        vid = vid,
        expiresAt = expiresAt,
    )
}

private fun String.parseCloudfrontCookies(): Map<String, String> =
    split(";").filter { it.contains("=") }
        .associate { pair ->
            val (k, v) = pair.trim().split("=", limit = 2)
            k.trim() to v.trim()
        }

/** Parses "#RRGGBB" / "RRGGBB" / "#AARRGGBB" into an ARGB Long, or null. */
internal fun String.parseHexColor(): Long? {
    val cleaned = trim().removePrefix("#")
    val argbHex = when (cleaned.length) {
        6 -> "FF$cleaned"
        8 -> cleaned
        else -> return null
    }
    return argbHex.toLongOrNull(16)
}
