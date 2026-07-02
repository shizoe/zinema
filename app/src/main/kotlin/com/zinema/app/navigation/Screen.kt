package com.zinema.app.navigation

/** Navigation routes (blueprint §9.2). */
sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object ProfileSelector : Screen("profile_selector")
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object ShortTv : Screen("shorttv")

    data object Detail : Screen("detail/{subjectId}") {
        fun createRoute(subjectId: String) = "detail/$subjectId"
    }

    data object Player : Screen("player/{subjectId}/{seasonIndex}/{episodeIndex}") {
        fun createRoute(subjectId: String, seasonIndex: Int = 0, episodeIndex: Int = 0) =
            "player/$subjectId/$seasonIndex/$episodeIndex"
    }
}
