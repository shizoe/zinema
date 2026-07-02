package com.zinema.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zinema.app.core.data.db.daos.PlaybackPositionDao
import com.zinema.app.core.data.db.daos.RecentSearchDao
import com.zinema.app.core.data.db.daos.TabCacheDao
import com.zinema.app.core.data.db.daos.WatchlistDao
import com.zinema.app.core.data.db.entities.CachedTabEntity
import com.zinema.app.core.data.db.entities.PlaybackPositionEntity
import com.zinema.app.core.data.db.entities.RecentSearchEntity
import com.zinema.app.core.data.db.entities.WatchlistEntity

/** Room database (blueprint §7). Built as "zinema.db" in DataModule. */
@Database(
    entities = [
        CachedTabEntity::class,
        PlaybackPositionEntity::class,
        WatchlistEntity::class,
        RecentSearchEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tabCacheDao(): TabCacheDao
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun recentSearchDao(): RecentSearchDao
}
