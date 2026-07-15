package com.tally.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tally.app.ui.importer.LiberatorImportScreen
import com.tally.app.ui.screens.DetailScreen
import com.tally.app.ui.screens.MoviesScreen
import com.tally.app.ui.screens.ProfileScreen
import com.tally.app.ui.screens.SearchScreen
import com.tally.app.ui.screens.ShowsScreen

@Composable
fun TallyNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.SHOWS,
        modifier = modifier,
    ) {
        composable(Routes.SHOWS) {
            ShowsScreen(
                onItemClick = { id, mediaType ->
                    navController.navigate(Routes.detail(mediaType, id))
                },
            )
        }

        composable(Routes.MOVIES) {
            MoviesScreen(
                onItemClick = { id, mediaType ->
                    navController.navigate(Routes.detail(mediaType, id))
                },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onItemClick = { id, mediaType ->
                    navController.navigate(Routes.detail(mediaType, id))
                },
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onItemClick = { id, mediaType ->
                    navController.navigate(Routes.detail(mediaType, id))
                },
                onImportTvTime = { navController.navigate(Routes.LIBERATOR_IMPORT) },
            )
        }

        composable(Routes.LIBERATOR_IMPORT) {
            LiberatorImportScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument("mediaType") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType },
            ),
        ) {
            DetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
