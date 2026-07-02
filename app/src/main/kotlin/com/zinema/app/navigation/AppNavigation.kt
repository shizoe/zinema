package com.zinema.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import com.zinema.app.core.domain.session.AuthState
import com.zinema.app.core.ui.theme.ZinemaColors
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zinema.app.feature.auth.LoginScreen
import com.zinema.app.feature.auth.ProfileSelectorScreen
import com.zinema.app.feature.auth.ProfileViewModel
import com.zinema.app.feature.detail.DetailScreen
import com.zinema.app.feature.home.HomeScreen
import com.zinema.app.feature.player.PlayerScreen
import com.zinema.app.feature.search.SearchScreen
import com.zinema.app.feature.shorttv.ShortTvScreen

/** Mobile navigation graph (blueprint §4 `navigation/`). */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val authState by appViewModel.authState.collectAsStateWithLifecycle()

    // Force re-auth when the token is rejected/expired mid-session.
    LaunchedEffect(authState) {
        if (authState == AuthState.EXPIRED) {
            navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (authState == AuthState.GUEST_EXPIRING) {
            GuestExpiringBanner(onClick = { navController.navigate(Screen.Auth.route) })
        }
        NavHost(navController = navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                onPlayClick = { navController.navigate(Screen.Player.createRoute(it.id)) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onProfileClick = { navController.navigate(Screen.ProfileSelector.route) },
                onShortTvClick = { navController.navigate(Screen.ShortTv.route) },
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType }),
        ) {
            DetailScreen(
                onPlayClick = { id, season, episode ->
                    navController.navigate(Screen.Player.createRoute(id, season, episode))
                },
                onBackClick = { navController.popBackStack() },
                onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                onShareClick = { /* TODO: share Intent */ },
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("seasonIndex") { type = NavType.IntType },
                navArgument("episodeIndex") { type = NavType.IntType },
            ),
        ) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.ShortTv.route) {
            ShortTvScreen(
                onWatchSeries = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.ProfileSelector.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            val profiles by profileViewModel.profiles.collectAsStateWithLifecycle()
            ProfileSelectorScreen(
                profiles = profiles,
                onProfileSelected = {
                    profileViewModel.selectProfile(it)
                    navController.popBackStack()
                },
                onAddProfile = { profileViewModel.addProfile("New Profile", 0, false, null) },
                onManageProfiles = {},
                onVerifyPin = profileViewModel::verifyPin,
            )
        }
        }
    }
}

@Composable
private fun GuestExpiringBanner(onClick: () -> Unit) {
    Text(
        text = "Guest access is expiring soon. Tap to log in and keep watching.",
        color = ZinemaColors.OnBackground,
        textAlign = TextAlign.Center,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(ZinemaColors.PrimaryVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp),
    )
}
