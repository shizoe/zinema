package com.zinema.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentTab
import com.zinema.app.core.domain.usecase.ToggleWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns tab selection + the visible tab list (blueprint T-040). Content loading
 * lives in [TabContentViewModel]; this also handles the hero "+ My List" action.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val toggleWatchlist: ToggleWatchlistUseCase,
) : ViewModel() {

    val tabs: List<ContentTab> = CONTENT_TABS.filter { it.isVisible && it.tabId !in SUPPRESSED_TAB_IDS }

    private val _selectedTabId = MutableStateFlow(tabs.first().tabId)
    val selectedTabId: StateFlow<Int> = _selectedTabId.asStateFlow()

    fun selectTab(tabId: Int) {
        _selectedTabId.value = tabId
    }

    fun onWatchlistToggle(content: Content) {
        viewModelScope.launch { toggleWatchlist(content) }
    }
}
