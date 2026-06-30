package com.zinema.app.core.ui.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.staticCompositionLocalOf

/** True when running on an Android TV / Google TV device. */
fun Context.isTvDevice(): Boolean {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

/**
 * Whether the current composition is running on TV. Set by ZinemaTheme; lets
 * shared components switch between mobile (Material3) and TV (tv-material) chrome.
 */
val LocalIsTv = staticCompositionLocalOf { false }
