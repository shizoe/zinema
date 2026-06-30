package com.zinema.app.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Recent-search history (blueprint T-053). Kept as a domain abstraction so the
 * search feature doesn't depend on core:data / the Room DAO directly
 * (PHASE-8 §Deviations).
 */
interface SearchHistoryRepository {

    /** Most-recent queries first (capped). */
    fun recentSearches(): Flow<List<String>>

    /** Records a query, dedups it, and trims history to the cap (oldest evicted). */
    suspend fun addRecentSearch(query: String)

    suspend fun clearRecentSearches()
}
