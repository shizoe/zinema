package com.zinema.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zinema.app.core.domain.analytics.Analytics
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentType
import com.zinema.app.core.domain.usecase.GetTabContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Loads one tab's feed (blueprint §9.3 pattern, T-040). The repository returns a
 * flat, deduped `List<Content>`; this groups it by [ContentType] into named rails
 * and uses the first few items as the hero carousel (PHASE-5 §Deviations).
 */
@HiltViewModel
class TabContentViewModel @Inject constructor(
    private val getTabContent: GetTabContentUseCase,
    private val analytics: Analytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TabContentUiState>(TabContentUiState.Loading)
    val uiState: StateFlow<TabContentUiState> = _uiState.asStateFlow()

    private var currentTabId: Int? = null

    fun loadTab(tabId: Int) {
        if (tabId == currentTabId && _uiState.value is TabContentUiState.Success) return
        currentTabId = tabId
        analytics.trackTabViewed(tabId, CONTENT_TABS.firstOrNull { it.tabId == tabId }?.displayName.orEmpty())
        viewModelScope.launch {
            _uiState.value = TabContentUiState.Loading
            getTabContent(tabId)
                .catch { e -> _uiState.value = TabContentUiState.Error(e.message ?: "Something went wrong.") }
                .collect { items ->
                    _uiState.value = if (items.isEmpty()) {
                        TabContentUiState.Empty
                    } else {
                        TabContentUiState.Success(hero = items.take(HERO_COUNT), rails = items.toRails())
                    }
                }
        }
    }

    fun retry() {
        val id = currentTabId ?: return
        currentTabId = null
        loadTab(id)
    }

    private companion object {
        const val HERO_COUNT = 5
    }
}

private fun List<Content>.toRails(): List<HomeRail> =
    groupBy { it.type }.map { (type, items) -> HomeRail(type.railTitle(), items) }

private fun ContentType.railTitle(): String = when (this) {
    ContentType.MOVIE -> "Movies"
    ContentType.TV -> "TV Shows"
    ContentType.ANIME -> "Anime"
    ContentType.SHORT -> "Short Clips"
    ContentType.SPORTS -> "Sports"
}

sealed interface TabContentUiState {
    data object Loading : TabContentUiState
    data class Success(val hero: List<Content>, val rails: List<HomeRail>) : TabContentUiState
    data object Empty : TabContentUiState
    data class Error(val message: String) : TabContentUiState
}

data class HomeRail(val title: String, val items: List<Content>)
