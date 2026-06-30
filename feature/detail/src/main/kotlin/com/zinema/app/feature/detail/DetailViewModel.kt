package com.zinema.app.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zinema.app.core.domain.model.ContentDetail
import com.zinema.app.core.domain.model.Episode
import com.zinema.app.core.domain.model.PlaybackPosition
import com.zinema.app.core.domain.repository.PlaybackRepository
import com.zinema.app.core.domain.repository.UserRepository
import com.zinema.app.core.domain.usecase.GetContentDetailUseCase
import com.zinema.app.core.domain.usecase.GetEpisodesUseCase
import com.zinema.app.core.domain.usecase.ToggleWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Detail screen state holder (blueprint T-044). Loads [ContentDetail] on init,
 * exposes reactive watchlist state, the resume position, and per-season episodes.
 *
 * `subjectId` comes from the nav argument via [SavedStateHandle].
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getContentDetail: GetContentDetailUseCase,
    private val getEpisodes: GetEpisodesUseCase,
    private val toggleWatchlistUseCase: ToggleWatchlistUseCase,
    userRepository: UserRepository,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val subjectId: String = savedStateHandle.get<String>(ARG_SUBJECT_ID).orEmpty()

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    val isInWatchlist: StateFlow<Boolean> = userRepository.isInWatchlist(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes.asStateFlow()

    private val _resume = MutableStateFlow<PlaybackPosition?>(null)
    val resume: StateFlow<PlaybackPosition?> = _resume.asStateFlow()

    init {
        if (subjectId.isBlank()) {
            _uiState.value = DetailUiState.Error("Missing content id.")
        } else {
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            getContentDetail(subjectId)
                .catch { e -> _uiState.value = DetailUiState.Error(e.message ?: "Couldn't load details.") }
                .collect { detail ->
                    _uiState.value = DetailUiState.Success(detail)
                    _episodes.value = detail.episodes
                    detail.seasons.firstOrNull()?.let { _selectedSeason.value = it }
                }
        }
        viewModelScope.launch {
            _resume.value = playbackRepository.getPosition(subjectId)
        }
    }

    fun selectSeason(season: Int) {
        if (season == _selectedSeason.value) return
        _selectedSeason.value = season
        viewModelScope.launch {
            getEpisodes(subjectId, season)
                .catch { /* keep previous episodes on failure */ }
                .collect { _episodes.value = it }
        }
    }

    fun toggleWatchlist() {
        val content = (_uiState.value as? DetailUiState.Success)?.detail?.content ?: return
        viewModelScope.launch { toggleWatchlistUseCase(content) }
    }

    fun retry() = load()

    private companion object {
        const val ARG_SUBJECT_ID = "subjectId"
    }
}

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val detail: ContentDetail) : DetailUiState
    data class Error(val message: String) : DetailUiState
}
