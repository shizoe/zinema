package com.zinema.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt aggregation root for the whole app.
 *
 * Phase 0 keeps this minimal. Crashlytics custom keys, Firebase init, and any
 * app-wide startup wiring (blueprint Phase 10, T-059) are added later.
 */
@HiltAndroidApp
class ZinemaApplication : Application()
