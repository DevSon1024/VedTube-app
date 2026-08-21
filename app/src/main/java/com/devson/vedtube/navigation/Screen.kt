package com.devson.vedtube.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
}
