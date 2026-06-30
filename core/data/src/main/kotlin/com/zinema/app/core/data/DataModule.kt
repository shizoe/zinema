package com.zinema.app.core.data

import android.content.Context
import androidx.room.Room
import com.zinema.app.core.data.db.AppDatabase
import com.zinema.app.core.data.db.daos.PlaybackPositionDao
import com.zinema.app.core.data.db.daos.RecentSearchDao
import com.zinema.app.core.data.db.daos.TabCacheDao
import com.zinema.app.core.data.db.daos.WatchlistDao
import com.zinema.app.core.data.connectivity.ConnectivityObserverImpl
import com.zinema.app.core.data.repositories.AuthRepositoryImpl
import com.zinema.app.core.data.repositories.ContentRepositoryImpl
import com.zinema.app.core.data.repositories.PlaybackRepositoryImpl
import com.zinema.app.core.data.repositories.ProfileRepositoryImpl
import com.zinema.app.core.data.repositories.SearchHistoryRepositoryImpl
import com.zinema.app.core.data.repositories.UserRepositoryImpl
import com.zinema.app.core.domain.repository.AuthRepository
import com.zinema.app.core.domain.repository.ContentRepository
import com.zinema.app.core.domain.repository.PlaybackRepository
import com.zinema.app.core.domain.repository.ProfileRepository
import com.zinema.app.core.domain.repository.SearchHistoryRepository
import com.zinema.app.core.domain.repository.UserRepository
import com.zinema.app.core.domain.util.ConnectivityObserver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the data layer (blueprint T-028): the Room database, its DAOs,
 * and the repository implementations.
 *
 * Repos are `@Binds` (impl → interface); the DB/DAOs are `@Provides` in the
 * companion object (Dagger requires static-style providers alongside abstract binds).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackRepository(impl: PlaybackRepositoryImpl): PlaybackRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(impl: ConnectivityObserverImpl): ConnectivityObserver

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "zinema.db")
                .fallbackToDestructiveMigration()
                .build()

        @Provides
        fun provideTabCacheDao(db: AppDatabase): TabCacheDao = db.tabCacheDao()

        @Provides
        fun providePlaybackPositionDao(db: AppDatabase): PlaybackPositionDao = db.playbackPositionDao()

        @Provides
        fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()

        @Provides
        fun provideRecentSearchDao(db: AppDatabase): RecentSearchDao = db.recentSearchDao()
    }
}
