package com.devson.vedtube.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devson.vedtube.feature.home.HomeScreen
import com.devson.vedtube.feature.home.HomeViewModel
import com.devson.vedtube.feature.video.VideoDetailsViewModel
import com.devson.vedtube.feature.video.ui.VideoDetailsScreen

@Composable
fun VedTubeNavHost(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel,
    isInPipMode: Boolean = false,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                isInPipMode = isInPipMode,
                onVideoClick = { video ->
                    if (navController.currentDestination?.route == Screen.Home.route) {
                        navController.navigate(Screen.VideoDetails.createRoute(video.id)) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.VideoDetails.route,
            arguments = listOf(
                navArgument("videoId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val videoDetailsViewModel: VideoDetailsViewModel = hiltViewModel(backStackEntry)
            VideoDetailsScreen(
                viewModel = videoDetailsViewModel,
                isInPipMode = isInPipMode,
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
