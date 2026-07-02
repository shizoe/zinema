package com.zinema.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hilt aggregation root for the whole app.
 *
 * Implements [ImageLoaderFactory] so Coil's singleton (used by `AsyncImage`
 * everywhere) is the Hilt-provided [ImageLoader] from NetworkModule — i.e. the
 * one with the CDN allowlist interceptor and 200MB disk cache (blueprint §8.7).
 *
 * Crashlytics custom keys / Firebase init (Phase 10, T-059) are added later.
 */
@HiltAndroidApp
class ZinemaApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun newImageLoader(): ImageLoader = imageLoader
}
