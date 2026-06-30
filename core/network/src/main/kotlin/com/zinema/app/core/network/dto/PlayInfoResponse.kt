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
    val resolutions: String = "",        // "1080", "720", "480", "360"
    val url: String = "",
    val signCookie: String = "",         // CloudFront-Policy=...;CloudFront-Signature=...;CloudFront-Key-Pair-Id=...;
    val streamType: String = "dash",     // "dash" | "hls" | "mp4"
    val bandwidth: Long? = null,
)

@Serializable
data class SubtitleDto(
    val language: String = "",
    val languageCode: String = "",
    val url: String = "",
    val format: String = "vtt",          // "vtt" | "srt" | "ttml"
)
