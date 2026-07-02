package com.zinema.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response model for `wefeed-mobile-bff/subject-api/play-info` (blueprint §5.4).
 * [StreamDto.signCookie] carries the CloudFront policy/signature/key-pair triple.
 */
@Serializable
data class PlayInfoData(
    val streams: List<StreamDto> = emptyList(),
    val subtitles: List<SubtitleDto>? = null,
    val vid: String = "",
    val expiresAt: String? = null,
)

@Serializable
data class StreamDto(
    val id: String = "",                 // resource id — used to fetch external subtitles
    val resolutions: String = "",        // "1080", "720", "480", "360"
    val url: String = "",
    val format: String = "",             // "MP4" | "DASH" | "HLS"
    val signCookie: String = "",         // CloudFront-Policy=...;CloudFront-Signature=...;CloudFront-Key-Pair-Id=...;
    val streamType: String = "dash",     // legacy alias; real API uses `format`
    val bandwidth: Long? = null,
)

@Serializable
data class SubtitleDto(
    val language: String = "",
    val languageCode: String = "",
    val url: String = "",
    val format: String = "vtt",          // "vtt" | "srt" | "ttml"
)

/**
 * Response for `wefeed-mobile-bff/subject-api/get-ext-captions` — external subtitle
 * tracks are NOT part of play-info; they are fetched separately, keyed by the
 * subject + the playing stream's resource id + episode.
 */
@Serializable
data class ExtCaptionsData(
    val extCaptions: List<ExtCaption> = emptyList(),
    val subjectId: String = "",
)

@Serializable
data class ExtCaption(
    val id: String = "",
    val lan: String = "",                // language code, e.g. "en"
    val lanName: String = "",            // display name, e.g. "English"
    val url: String = "",                // .vtt / .srt file on the caption CDN
    val size: String = "",
    val delay: Int = 0,                  // sync offset in ms
)
