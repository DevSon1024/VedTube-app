package com.devson.vedtube.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object VideoDetails : Screen("video/{videoId}") {
        fun createRoute(videoId: String): String = "video/$videoId"
    }
}
