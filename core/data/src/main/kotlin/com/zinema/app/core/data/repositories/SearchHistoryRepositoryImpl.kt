package com.zinema.app.core.data.repositories

import com.zinema.app.core.data.db.daos.RecentSearchDao
import com.zinema.app.core.data.db.entities.RecentSearchEntity
import com.zinema.app.core.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Recent searches backed by Room (blueprint T-053). */
class SearchHistoryRepositoryImpl @Inject constructor(
    private val recentSearchDao: RecentSearchDao,
) : SearchHistoryRepository {

    override fun recentSearches(): Flow<List<String>> =
        recentSearchDao.observeRecent(MAX_RECENT).map { rows -> rows.map { it.query } }

    override suspend fun addRecentSearch(query: String) {
        recentSearchDao.upsert(
            RecentSearchEntity(query = query, searchedAtMs = System.currentTimeMillis()),
        )
        recentSearchDao.trimToLimit(MAX_RECENT)
    }

    override suspend fun clearRecentSearches() {
        recentSearchDao.deleteAll()
    }

    private companion object {
        const val MAX_RECENT = 10
    }
}
