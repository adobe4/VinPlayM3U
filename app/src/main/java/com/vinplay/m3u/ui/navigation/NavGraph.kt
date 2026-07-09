package com.vinplay.m3u.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vinplay.m3u.ui.screens.channels.ChannelsScreen
import com.vinplay.m3u.ui.screens.import.ImportScreen
import com.vinplay.m3u.ui.screens.playlists.PlaylistsScreen
import com.vinplay.m3u.ui.screens.settings.SettingsScreen

object Routes {
    const val PLAYLISTS = "playlists"
    const val CHANNELS = "channels/{playlistId}?name={name}"
    const val IMPORT = "import/{playlistId}"
    const val SETTINGS = "settings"

    fun channels(playlistId: Long, name: String = ""): String =
        "channels/$playlistId?name=$name"

    fun import(playlistId: Long): String = "import/$playlistId"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.PLAYLISTS) {
        composable(Routes.PLAYLISTS) {
            PlaylistsScreen(
                onPlaylistClick = { id, name ->
                    navController.navigate(Routes.channels(id, name))
                },
                onImportClick = { id ->
                    navController.navigate(Routes.import(id))
                }
            )
        }

        composable(
            route = Routes.CHANNELS,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            val playlistName = backStackEntry.arguments?.getString("name") ?: ""
            ChannelsScreen(
                playlistId = playlistId,
                playlistName = playlistName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.IMPORT,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            ImportScreen(
                playlistId = playlistId,
                onBack = { navController.popBackStack() },
                onImportComplete = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
