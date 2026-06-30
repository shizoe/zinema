package com.zinema.app.core.data.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zinema.app.core.data.db.entities.RecentSearchEntity

@Dao
interface RecentSearchDao {

    // REPLACE on the unique `query` index dedups by search text (a re-search of an
    // existing term refreshes its row/timestamp rather than inserting a duplicate).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentSearchEntity)

    @Query("SELECT * FROM recent_searches ORDER BY searchedAtMs DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<RecentSearchEntity>

    @Query("DELETE FROM recent_searches")
    suspend fun deleteAll()
}
