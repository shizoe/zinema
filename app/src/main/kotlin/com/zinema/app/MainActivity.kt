package com.zinema.app

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity + platform detector (blueprint §9.1).
 *
 * Phase 0 status: the real navigation graphs (TvNavigation / AppNavigation) and
 * ZinemaTheme do not exist yet. This renders a placeholder that proves the
 * scaffold builds, runs, and correctly distinguishes TV from mobile at startup.
 * Replace [PlaceholderRoot] with the themed NavHost in Phase 3+.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isTv = (getSystemService(Context.UI_MODE_SERVICE) as UiModeManager)
            .currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        setContent {
            // TODO(Phase 3): wrap in ZinemaTheme.
            // TODO(Phase 4/5): if (isTv) TvNavigation() else AppNavigation()
            PlaceholderRoot(isTv = isTv)
        }
    }
}

@Composable
private fun PlaceholderRoot(isTv: Boolean) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Zinema")
            Text(text = if (isTv) "TV scaffold ready" else "Mobile scaffold ready")
        }
    }
}
