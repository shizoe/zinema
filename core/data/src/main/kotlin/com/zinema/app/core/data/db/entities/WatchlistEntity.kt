package com.zinema.app.core.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A "My List" entry (blueprint §7). Stores enough to render a card offline. */
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val subjectId: String,
    val title: String,
    val posterUrl: String,
    val contentType: String,
    val addedAtMs: Long,
)
