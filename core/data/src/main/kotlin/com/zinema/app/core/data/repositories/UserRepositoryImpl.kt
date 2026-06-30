package com.zinema.app.core.data.repositories

import com.zinema.app.core.data.db.daos.WatchlistDao
import com.zinema.app.core.data.db.entities.WatchlistEntity
import com.zinema.app.core.data.mappers.toContent
import com.zinema.app.core.domain.model.Content
import com.zinema.app.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Watchlist ("My List") backed by Room (blueprint T-028, ToggleWatchlistUseCase). */
class UserRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao,
) : UserRepository {

    override fun isInWatchlist(subjectId: String): Flow<Boolean> =
        watchlistDao.isInWatchlist(subjectId)

    override fun getWatchlist(): Flow<List<Content>> =
        watchlistDao.getAll().map { rows -> rows.map { it.toContent() } }

    override suspend fun toggleWatchlist(content: Content) {
        if (watchlistDao.existsOnce(content.id)) {
            watchlistDao.delete(content.id)
        } else {
            watchlistDao.upsert(
                WatchlistEntity(
                    subjectId = content.id,
                    title = content.title,
                    posterUrl = content.posterUrl,
                    contentType = content.type.name,
                    addedAtMs = System.currentTimeMillis(),
                ),
            )
        }
    }
}
