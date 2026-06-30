package com.zinema.app.core.data.db.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zinema.app.core.data.db.entities.CachedTabEntity

@Dao
interface TabCacheDao {

    @Upsert
    suspend fun upsert(entity: CachedTabEntity)

    @Query("SELECT * FROM tab_cache WHERE tabId = :tabId")
    suspend fun getByTabId(tabId: Int): CachedTabEntity?

    @Query("DELETE FROM tab_cache WHERE fetchedAtMs < :timestampMs")
    suspend fun deleteOlderThan(timestampMs: Long)
}
