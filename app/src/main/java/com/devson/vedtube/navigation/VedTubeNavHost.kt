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
import com.devson.vedtube.feature.playlist.PlaylistDetailViewModel
import com.devson.vedtube.feature.playlist.ui.PlaylistDetailScreen
import com.devson.vedtube.feature.settings.SettingsViewModel
import com.devson.vedtube.feature.settings.ui.SettingsScreen
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
                    navController.navigate(Screen.VideoDetails.createRoute(video.id)) {
                        launchSingleTop = true
                    }
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetails.createRoute(playlistId)) {
                        launchSingleTop = true
                    }
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
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

        composable(
            route = Screen.PlaylistDetails.route,
            arguments = listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val playlistDetailViewModel: PlaylistDetailViewModel = hiltViewModel(backStackEntry)
            PlaylistDetailScreen(
                viewModel = playlistDetailViewModel,
                onVideoClick = { video ->
                    navController.navigate(Screen.VideoDetails.createRoute(video.id)) {
                        launchSingleTop = true
                    }
                },
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(route = Screen.Settings.route) { backStackEntry ->
            val settingsViewModel: SettingsViewModel = hiltViewModel(backStackEntry)
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
