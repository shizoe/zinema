package com.zinema.app.core.data.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zinema.app.core.data.db.entities.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {

    // REPLACE on the unique `query` index dedups by search text (a re-search of an
    // existing term refreshes its row/timestamp rather than inserting a duplicate).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentSearchEntity)

    @Query("SELECT * FROM recent_searches ORDER BY searchedAtMs DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<RecentSearchEntity>

    /** Reactive recent list (blueprint T-053). */
    @Query("SELECT * FROM recent_searches ORDER BY searchedAtMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<RecentSearchEntity>>

    /** Keeps only the [keep] most recent rows; evicts the rest (oldest first). */
    @Query(
        "DELETE FROM recent_searches WHERE id NOT IN " +
            "(SELECT id FROM recent_searches ORDER BY searchedAtMs DESC LIMIT :keep)",
    )
    suspend fun trimToLimit(keep: Int)

    @Query("DELETE FROM recent_searches")
    suspend fun deleteAll()
}
