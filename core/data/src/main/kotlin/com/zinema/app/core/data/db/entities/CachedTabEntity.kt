package com.zinema.app.core.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached tab feed (blueprint §7). [contentJson] is a serialized List of content. */
@Entity(tableName = "tab_cache")
data class CachedTabEntity(
    @PrimaryKey val tabId: Int,
    val contentJson: String,
    val fetchedAtMs: Long,
    val version: String,
)
