package com.zinema.app.core.data.db.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zinema.app.core.data.db.entities.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Upsert
    suspend fun upsert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE subjectId = :subjectId")
    suspend fun delete(subjectId: String)

    @Query("SELECT * FROM watchlist ORDER BY addedAtMs DESC")
    fun getAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE subjectId = :subjectId)")
    fun isInWatchlist(subjectId: String): Flow<Boolean>

    /** One-shot variant used by toggle logic (no Flow collection needed). */
    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE subjectId = :subjectId)")
    suspend fun existsOnce(subjectId: String): Boolean
}
