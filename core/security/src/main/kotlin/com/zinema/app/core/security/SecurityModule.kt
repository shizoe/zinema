package com.zinema.app.core.security

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the security layer (blueprint T-010).
 *
 * Both stores wrap EncryptedSharedPreferences and must be process singletons so
 * the cached device id and JWT are read/written through a single instance.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideTokenStorage(
        @ApplicationContext context: Context,
    ): TokenStorage = TokenStorage(context)

    @Provides
    @Singleton
    fun provideDeviceIdProvider(
        @ApplicationContext context: Context,
    ): DeviceIdProvider = DeviceIdProvider(context)
}
