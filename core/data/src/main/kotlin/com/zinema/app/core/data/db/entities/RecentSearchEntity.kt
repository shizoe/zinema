package com.zinema.app.core.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A recent search query (blueprint §7). Unique on [query] so re-searching dedups. */
@Entity(tableName = "recent_searches", indices = [Index("query", unique = true)])
data class RecentSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val searchedAtMs: Long,
)
