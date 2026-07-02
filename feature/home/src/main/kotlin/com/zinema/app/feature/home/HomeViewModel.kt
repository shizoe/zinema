package com.zinema.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.model.ContentTab
import com.zinema.app.core.domain.usecase.GetContentTabsUseCase
import com.zinema.app.core.domain.usecase.ToggleWatchlistUseCase
import com.zinema.app.core.domain.util.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns tab selection + the visible tab list (blueprint T-040). Content loading
 * lives in [TabContentViewModel]; this also handles the hero "+ My List" action.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    getContentTabs: GetContentTabsUseCase,
    private val toggleWatchlist: ToggleWatchlistUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    /** Static fallback used offline or if the server list is empty/unavailable. */
    private val fallbackTabs: List<ContentTab> =
        CONTENT_TABS.filter { it.isVisible && it.tabId !in SUPPRESSED_TAB_IDS }

    /** Server-driven content categories (subject-api/bottom-tab), with fallback. */
    val tabs: StateFlow<List<ContentTab>> = getContentTabs()
        .map { serverTabs -> serverTabs.ifEmpty { fallbackTabs } }
        .catch { emit(fallbackTabs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), fallbackTabs)

    private val _selectedTabId = MutableStateFlow(BOTTOM_NAV_TAB_IDS.first())
    val selectedTabId: StateFlow<Int> = _selectedTabId.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun selectTab(tabId: Int) {
        _selectedTabId.value = tabId
    }

    fun onWatchlistToggle(content: Content) {
        viewModelScope.launch { toggleWatchlist(content) }
    }
}
