package com.zinema.app.core.domain.model

/**
 * Resolved, ready-to-play stream (blueprint §6). The CloudFront triple is applied
 * to the streaming CDN via the player's cookie jar before playback.
 */
data class StreamInfo(
    val streamUrl: String,
    val quality: String,
    val availableQualities: List<String>,
    val resourceId: String,
    val streamProtocol: StreamProtocol,
    val cloudFrontPolicy: String,
    val cloudFrontSignature: String,
    val cloudFrontKeyPairId: String,
    val subtitles: List<SubtitleTrack>,
    val vid: String,
    val expiresAt: String?,
)

enum class StreamProtocol { DASH, HLS, PROGRESSIVE_MP4 }

data class SubtitleTrack(
    val language: String,
    val languageCode: String,
    val url: String,
    val format: String,
)
