package com.zinema.app.core.data.db.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zinema.app.core.data.db.entities.PlaybackPositionEntity

@Dao
interface PlaybackPositionDao {

    @Upsert
    suspend fun upsert(entity: PlaybackPositionEntity)

    @Query("SELECT * FROM playback_positions WHERE subjectId = :subjectId")
    suspend fun getBySubjectId(subjectId: String): PlaybackPositionEntity?

    /**
     * Continue-watching candidates: rows whose watched fraction is within
     * (minPct, maxPct), most-recently watched first. Casts to REAL to avoid
     * integer division and guards against zero-length items.
     */
    @Query(
        """
        SELECT * FROM playback_positions
        WHERE totalDurationMs > 0
          AND (CAST(positionMs AS REAL) / totalDurationMs) > :minPct
          AND (CAST(positionMs AS REAL) / totalDurationMs) < :maxPct
        ORDER BY updatedAtMs DESC
        """,
    )
    suspend fun getAllBetweenCompletion(minPct: Float, maxPct: Float): List<PlaybackPositionEntity>

    @Query("DELETE FROM playback_positions")
    suspend fun deleteAll()
}
