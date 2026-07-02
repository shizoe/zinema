package com.zinema.app.core.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Resume point keyed by subjectId (blueprint §7 / OQ-07). */
@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey val subjectId: String,
    val contentType: String,
    val seasonIndex: Int,
    val episodeIndex: Int,
    val positionMs: Long,
    val totalDurationMs: Long,
    val updatedAtMs: Long,
)
