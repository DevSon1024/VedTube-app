package com.devson.vedtube.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devson.vedtube.feature.home.HomeScreen
import com.devson.vedtube.feature.home.HomeViewModel

@Composable
fun VedTubeNavHost(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(viewModel = homeViewModel)
        }
    }
}
