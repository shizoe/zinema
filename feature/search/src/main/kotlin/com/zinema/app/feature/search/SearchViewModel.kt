package com.zinema.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentType
import com.zinema.app.core.domain.repository.SearchHistoryRepository
import com.zinema.app.core.domain.usecase.SearchContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Search state holder (blueprint T-053). The query is debounced (300ms) and mapped
 * to grouped results; recent searches come from [SearchHistoryRepository].
 *
 * NOTE: results are empty until the search API endpoint exists (§8.1 has none) —
 * the pipeline is complete and will light up once `searchContent` is wired.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchContent: SearchContentUseCase,
    private val searchHistory: SearchHistoryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val recentSearches: StateFlow<List<String>> = searchHistory.recentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: StateFlow<SearchUiState> = _query
        .debounce(DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(SearchUiState.Idle)
            } else {
                searchContent(q).map { items ->
                    if (items.isEmpty()) SearchUiState.Empty else SearchUiState.Results(items.groupBy { it.type })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Idle)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onQuerySubmit() {
        val q = _query.value.trim()
        if (q.isNotEmpty()) viewModelScope.launch { searchHistory.addRecentSearch(q) }
    }

    fun onRecentSelected(value: String) {
        _query.value = value
        onQuerySubmit()
    }

    fun clearRecentSearches() {
        viewModelScope.launch { searchHistory.clearRecentSearches() }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
    }
}

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Empty : SearchUiState
    data class Results(val grouped: Map<ContentType, List<Content>>) : SearchUiState
}
