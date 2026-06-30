package com.zinema.app.core.domain.repository

import com.zinema.app.core.domain.model.Content
import kotlinx.coroutines.flow.Flow

/**
 * User library — the watchlist ("My List"). Profiles will be added here later.
 *
 * Watchlist lives here (rather than ContentRepository) because it is per-user
 * library data; the blueprint's three repositories are Content/Playback/User
 * (PHASE-2 §Deviations).
 */
interface UserRepository {

    fun isInWatchlist(subjectId: String): Flow<Boolean>

    fun getWatchlist(): Flow<List<Content>>

    /** Adds the item if absent, removes it if present. */
    suspend fun toggleWatchlist(content: Content)
}
