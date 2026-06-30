package com.zinema.app.feature.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.zinema.app.core.domain.exception.StreamSecurityException
import com.zinema.app.core.domain.model.ContentType
import com.zinema.app.core.domain.model.StreamInfo
import com.zinema.app.core.domain.model.StreamProtocol
import com.zinema.app.core.domain.usecase.GetStreamInfoUseCase
import com.zinema.app.core.domain.usecase.SavePlaybackPositionUseCase
import com.zinema.app.core.network.cdn.CdnValidator
import com.zinema.app.core.network.cdn.CloudFrontCookieJar
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Player state + ExoPlayer owner (blueprint T-047).
 *
 * The player is built on the shared [OkHttpClient] so its CloudFront cookie jar
 * sends the signed `play_auth` cookies on stream requests. On each resolve we set
 * those cookies, report position every 5s, and refresh the stream shortly before
 * its credentials expire.
 */
@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    private val getStreamInfo: GetStreamInfoUseCase,
    private val savePosition: SavePlaybackPositionUseCase,
    private val subtitlePreferences: SubtitlePreferences,
    private val cookieJar: CloudFrontCookieJar,
    okHttpClient: OkHttpClient,
) : ViewModel() {

    private val subjectId: String = savedStateHandle.get<String>("subjectId").orEmpty()
    private val season: Int = savedStateHandle.get<Int>("seasonIndex") ?: 0
    private val episode: Int = savedStateHandle.get<Int>("episodeIndex") ?: 0
    private val contentType: ContentType = savedStateHandle.get<String>("contentType")
        ?.let { runCatching { ContentType.valueOf(it) }.getOrNull() } ?: ContentType.MOVIE

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val selectedSubtitleLanguage: StateFlow<String?> = subtitlePreferences.preferredLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var currentQuality: String = "1080"
    private var expiryJob: Job? = null

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context).setDataSourceFactory(OkHttpDataSource.Factory(okHttpClient)),
        )
        .build()

    init {
        loadStreamInfo(currentQuality)
        startPositionReporting()
    }

    fun loadStreamInfo(quality: String = currentQuality) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            try {
                val resumeFrom = player.currentPosition.takeIf { it > 0 }
                val streamInfo = getStreamInfo(subjectId, season, episode, quality)
                currentQuality = streamInfo.quality
                injectCookies(streamInfo)
                prepare(streamInfo)
                resumeFrom?.let { player.seekTo(it) }
                applyPersistedSubtitle()
                _uiState.value = PlayerUiState.Ready(streamInfo)
                scheduleExpiryRefresh(streamInfo.expiresAt)
            } catch (e: StreamSecurityException) {
                Log.e(TAG, "Stream blocked by CDN allowlist", e)
                // TODO(Phase 10): Crashlytics.recordException(e)
                _uiState.value = PlayerUiState.Error("This title can't be played securely.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load stream", e)
                _uiState.value = PlayerUiState.Error(e.message ?: "Playback failed.")
            }
        }
    }

    fun changeQuality(quality: String) {
        if (quality == currentQuality) return
        loadStreamInfo(quality)
    }

    /** [languageCode] == null turns subtitles off. */
    fun selectSubtitle(languageCode: String?) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setPreferredTextLanguage(languageCode)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, languageCode == null)
            .build()
        viewModelScope.launch { subtitlePreferences.setPreferredLanguage(languageCode) }
    }

    private fun prepare(streamInfo: StreamInfo) {
        val subtitleConfigs = streamInfo.subtitles
            .filter { CdnValidator.isAllowed(it.url) }
            .map { sub ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(subtitleMime(sub.format))
                    .setLanguage(sub.languageCode.ifBlank { sub.language })
                    .build()
            }
        val mediaItem = MediaItem.Builder()
            .setUri(streamInfo.streamUrl)
            .apply { protocolMime(streamInfo.streamProtocol)?.let { setMimeType(it) } }
            .setSubtitleConfigurations(subtitleConfigs)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    private fun injectCookies(streamInfo: StreamInfo) {
        val host = streamInfo.streamUrl.toHttpUrlOrNull()?.host ?: return
        runCatching {
            cookieJar.setPlayAuthCookies(
                host = host,
                policy = streamInfo.cloudFrontPolicy,
                signature = streamInfo.cloudFrontSignature,
                keyPairId = streamInfo.cloudFrontKeyPairId,
            )
        }
    }

    private suspend fun applyPersistedSubtitle() {
        val language = subtitlePreferences.preferredLanguage.first()
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setPreferredTextLanguage(language)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, language == null)
            .build()
    }

    private fun startPositionReporting() {
        viewModelScope.launch {
            while (isActive) {
                delay(REPORT_INTERVAL_MS)
                reportPositionNow()
            }
        }
    }

    private fun reportPositionNow() {
        val duration = player.duration
        val position = player.currentPosition
        if (player.isPlaying && duration > 0 && position > 0) {
            viewModelScope.launch {
                savePosition(subjectId, contentType, season, episode, position, duration)
            }
        }
    }

    private fun scheduleExpiryRefresh(expiresAt: String?) {
        expiryJob?.cancel()
        val expiresMs = expiresAt?.trim()?.toLongOrNull() ?: return
        val refreshDelay = expiresMs - System.currentTimeMillis() - EXPIRY_LEAD_MS
        if (refreshDelay <= 0) return
        expiryJob = viewModelScope.launch {
            delay(refreshDelay)
            loadStreamInfo(currentQuality)
        }
    }

    override fun onCleared() {
        reportPositionNow()
        expiryJob?.cancel()
        player.release()
        super.onCleared()
    }

    private fun protocolMime(protocol: StreamProtocol): String? = when (protocol) {
        StreamProtocol.DASH -> MimeTypes.APPLICATION_MPD
        StreamProtocol.HLS -> MimeTypes.APPLICATION_M3U8
        StreamProtocol.PROGRESSIVE_MP4 -> null
    }

    private fun subtitleMime(format: String): String = when (format.lowercase()) {
        "srt" -> MimeTypes.APPLICATION_SUBRIP
        "ttml" -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.TEXT_VTT
    }

    private companion object {
        const val TAG = "PlayerViewModel"
        const val REPORT_INTERVAL_MS = 5_000L
        const val EXPIRY_LEAD_MS = 60_000L
    }
}

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Ready(val streamInfo: StreamInfo) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}
