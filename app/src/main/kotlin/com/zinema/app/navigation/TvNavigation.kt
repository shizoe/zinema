package com.zinema.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zinema.app.feature.auth.LoginScreen
import com.zinema.app.feature.auth.ProfileSelectorScreen
import com.zinema.app.feature.detail.DetailScreen
import com.zinema.app.feature.home.TvHomeScreen
import com.zinema.app.feature.player.TvPlayerScreen
import com.zinema.app.feature.search.SearchScreen

/** Android TV navigation graph. ShortTV is mobile-only and omitted (PRD §2.4). */
@Composable
fun TvNavigation() {
    val navController = rememberNavController()

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
            TvHomeScreen(
                onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                onPlayClick = { navController.navigate(Screen.Player.createRoute(it.id)) },
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
                onShareClick = { },
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
            TvPlayerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.ProfileSelector.route) {
            ProfileSelectorScreen(
                profiles = SAMPLE_PROFILES,
                onProfileSelected = { navController.popBackStack() },
                onAddProfile = {},
                onManageProfiles = {},
            )
        }
    }
}
