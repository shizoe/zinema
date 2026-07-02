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
import com.zinema.app.core.network.dto.ExtCaption
import com.zinema.app.core.network.dto.PlayInfoData
import com.zinema.app.core.network.dto.ResourceItem
import com.zinema.app.core.network.dto.SubjectDetail
import com.zinema.app.core.network.dto.SubjectItem
import com.zinema.app.core.network.dto.SubtitleDto

/** DTO → domain mappers (blueprint §6.1). */

private const val DEFAULT_PLACEHOLDER_COLOR = 0xFF1A1A2EL

// Real backend subjectType values (verified against captured tab-operating data):
//   1 = Movie, 2 = TV series/drama, 5 = Kids/cartoon series, 6 = Music video,
//   7 = Short drama (vertical), 9 = Sports/live replay.
// There is NO "anime" subjectType — anime is a genre, so it's derived separately.
private fun Int.toContentType(): ContentType = when (this) {
    1 -> ContentType.MOVIE
    2 -> ContentType.TV
    5 -> ContentType.TV
    6 -> ContentType.MOVIE
    7 -> ContentType.SHORT
    9 -> ContentType.SPORTS
    else -> ContentType.MOVIE
}

/**
 * Classifies a subject. Anime is a genre (not a subjectType), so a movie/series
 * whose genres explicitly include "Anime" is surfaced as [ContentType.ANIME];
 * everything else falls back to the [subjectType] mapping. Note "Animation"
 * (Pixar/DreamWorks) is deliberately NOT treated as anime.
 */
private fun classifyContent(subjectType: Int, genres: List<String>): ContentType {
    val base = subjectType.toContentType()
    val isAnime = genres.any { it.trim().equals("Anime", ignoreCase = true) }
    return if (isAnime && (base == ContentType.MOVIE || base == ContentType.TV)) ContentType.ANIME else base
}

/** True for subject types that carry multiple episodes (series/drama/cartoons). */
private fun Int.isSeriesType(): Boolean = this == 2 || this == 5

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
    type = classifyContent(subjectType, parseGenres(genre, genres)),
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
    type = classifyContent(subjectType, parseGenres(genre, genres)),
    totalSeasons = totalSeason ?: seNum,
    totalEpisodes = totalEpisode ?: 0,
    trailerUrl = trailerUrl,
    placeholderColor = DEFAULT_PLACEHOLDER_COLOR,
)

fun SubjectDetail.toContentDetail(): ContentDetail = ContentDetail(
    content = toDomain(),
    // The real detail response carries neither a seasons array nor episodes; it
    // exposes `seNum` (season count, 0 for movies). Episodes come from a separate
    // subject-api/resource call, wired in the repository.
    seasons = when {
        seNum > 0 -> (1..seNum).toList()
        subjectType.isSeriesType() -> listOf(1)
        else -> emptyList()
    },
    episodes = emptyList(),
    related = relatedSubjects?.map { it.toDomain() }?.filter { it.id.isNotBlank() } ?: emptyList(),
)

fun EpisodeInfo.toDomain(): Episode = Episode(
    seasonIndex = se,
    episodeIndex = ep,
    title = title,
    thumbnailUrl = cover?.url?.takeIf { CdnValidator.isAllowed(it) } ?: "",
    durationMs = durationMs ?: 0L,
)

/**
 * Episodes for a season, resolved from `subject-api/resource`. The resource `list`
 * carries one entry PER RESOLUTION (e.g. 360p + 480p of the same episode), so we
 * dedupe by (season, episode) and drop movie-style entries (ep == 0). Titles often
 * embed the resolution (e.g. "…-S1E1-480P"); those are blanked to a clean label.
 */
fun List<ResourceItem>.toEpisodes(): List<Episode> =
    filter { it.ep > 0 }
        .distinctBy { it.se to it.ep }
        .sortedWith(compareBy({ it.se }, { it.ep }))
        .map { it.toEpisode() }

fun ResourceItem.toEpisode(): Episode = Episode(
    seasonIndex = se,
    episodeIndex = ep,
    title = title.takeUnless { it.isBlank() || it.contains(Regex("\\d{3,4}P")) } ?: "",
    thumbnailUrl = "",
    // Resource durations are in seconds (the detail endpoint uses `durationSeconds`).
    durationMs = if (duration > 0) duration * 1000L else 0L,
)

fun SubtitleDto.toDomain(): SubtitleTrack = SubtitleTrack(
    language = language,
    languageCode = languageCode,
    url = url,
    format = format,
)

fun ExtCaption.toDomain(): SubtitleTrack = SubtitleTrack(
    language = lanName.ifBlank { lan },
    languageCode = lan,
    url = url,
    // Caption URLs look like ".../foo.vtt?sign=..." — take the extension before any query.
    format = url.substringBefore('?').substringAfterLast('.', "").lowercase().ifBlank { "vtt" },
)

fun PlayInfoData.toDomain(preferredQuality: String = "1080"): StreamInfo {
    val stream = streams.firstOrNull { it.resolutions == preferredQuality }
        ?: streams.lastOrNull()
        ?: throw IllegalStateException("No streams available")
    val cookies = stream.signCookie.parseCloudfrontCookies()
    return StreamInfo(
        streamUrl = stream.url,
        quality = stream.resolutions,
        availableQualities = streams.map { it.resolutions }.filter { it.isNotBlank() }.distinct(),
        resourceId = stream.id,
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
