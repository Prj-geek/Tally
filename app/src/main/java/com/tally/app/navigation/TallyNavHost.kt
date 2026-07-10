package com.tally.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tally.app.ui.screens.DetailScreen
import com.tally.app.ui.screens.LibraryScreen
import com.tally.app.ui.screens.ProfileScreen
import com.tally.app.ui.screens.SearchScreen

@Composable
fun TallyNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY,
        modifier = modifier,
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen()
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onItemClick = { id, mediaType ->
                    navController.navigate(Routes.detail(mediaType, id))
                },
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen()
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
