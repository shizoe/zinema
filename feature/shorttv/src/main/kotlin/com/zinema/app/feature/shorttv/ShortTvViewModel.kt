package com.zinema.app.feature.shorttv

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.StreamInfo
import com.zinema.app.core.domain.usecase.GetStreamInfoUseCase
import com.zinema.app.core.domain.usecase.GetTabContentUseCase
import com.zinema.app.core.network.cdn.CloudFrontCookieJar
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Vertical ShortTV feed (blueprint T-055). Loads the ShortTV tab, owns a single
 * muted, looping [ExoPlayer] (on the shared OkHttp client so CloudFront cookies
 * flow), and resolves the visible item's stream on the fly, preloading the next two.
 */
@UnstableApi
@HiltViewModel
class ShortTvViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val getTabContent: GetTabContentUseCase,
    private val getStreamInfo: GetStreamInfoUseCase,
    private val cookieJar: CloudFrontCookieJar,
    okHttpClient: OkHttpClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShortTvUiState>(ShortTvUiState.Loading)
    val uiState: StateFlow<ShortTvUiState> = _uiState.asStateFlow()

    private val _streamUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val streamUrls: StateFlow<Map<String, String>> = _streamUrls.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context).setDataSourceFactory(OkHttpDataSource.Factory(okHttpClient)),
        )
        .build()
        .apply {
            volume = 0f // muted by default (blueprint T-056)
            repeatMode = Player.REPEAT_MODE_ONE
        }

    private var currentIndex = 0

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ShortTvUiState.Loading
            getTabContent(SHORTTV_TAB_ID)
                .catch { e -> _uiState.value = ShortTvUiState.Error(e.message ?: "Couldn't load.") }
                .collect { items ->
                    _uiState.value = if (items.isEmpty()) ShortTvUiState.Empty else ShortTvUiState.Success(items)
                    if (items.isNotEmpty()) onPageVisible(0)
                }
        }
    }

    fun onPageVisible(index: Int) {
        currentIndex = index
        val items = (uiState.value as? ShortTvUiState.Success)?.items ?: return
        items.getOrNull(index)?.let { ensureStreamThenPlay(it, forIndex = index) }
        // Preload the next two (blueprint T-056).
        for (i in (index + 1)..(index + 2)) {
            items.getOrNull(i)?.let { resolveStream(it.id) }
        }
    }

    fun toggleMute() {
        player.volume = if (player.volume > 0f) 0f else 1f
    }

    private fun ensureStreamThenPlay(content: Content, forIndex: Int) {
        _streamUrls.value[content.id]?.let {
            if (currentIndex == forIndex) playUrl(it)
            return
        }
        viewModelScope.launch {
            val info = runCatching { getStreamInfo(content.id) }.getOrNull() ?: return@launch
            cacheAndCookie(content.id, info)
            if (currentIndex == forIndex) playUrl(info.streamUrl)
        }
    }

    private fun resolveStream(subjectId: String) {
        if (_streamUrls.value.containsKey(subjectId)) return
        viewModelScope.launch {
            val info = runCatching { getStreamInfo(subjectId) }.getOrNull() ?: return@launch
            cacheAndCookie(subjectId, info)
        }
    }

    private fun cacheAndCookie(subjectId: String, info: StreamInfo) {
        injectCookies(info)
        _streamUrls.update { it + (subjectId to info.streamUrl) }
    }

    private fun playUrl(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    private fun injectCookies(info: StreamInfo) {
        val host = info.streamUrl.toHttpUrlOrNull()?.host ?: return
        runCatching {
            cookieJar.setPlayAuthCookies(
                host = host,
                policy = info.cloudFrontPolicy,
                signature = info.cloudFrontSignature,
                keyPairId = info.cloudFrontKeyPairId,
            )
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    private companion object {
        const val SHORTTV_TAB_ID = 13
    }
}

sealed interface ShortTvUiState {
    data object Loading : ShortTvUiState
    data class Success(val items: List<Content>) : ShortTvUiState
    data object Empty : ShortTvUiState
    data class Error(val message: String) : ShortTvUiState
}
