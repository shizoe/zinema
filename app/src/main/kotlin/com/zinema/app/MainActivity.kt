package com.zinema.app

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zinema.app.core.domain.analytics.Analytics
import com.zinema.app.core.domain.analytics.CrashReporter
import com.zinema.app.core.ui.theme.ZinemaTheme
import com.zinema.app.navigation.AppNavigation
import com.zinema.app.navigation.TvNavigation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity + platform detector (blueprint §9.1). Routes to the TV or mobile
 * navigation graph based on the device UI mode.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var analytics: Analytics

    @Inject
    lateinit var crashReporter: CrashReporter

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isTv = (getSystemService(Context.UI_MODE_SERVICE) as UiModeManager)
            .currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        crashReporter.setCustomKey("platform", if (isTv) "tv" else "mobile")
        analytics.trackAppOpen()

        setContent {
            ZinemaTheme {
                if (isTv) TvNavigation() else AppNavigation()
            }
        }
    }
}
