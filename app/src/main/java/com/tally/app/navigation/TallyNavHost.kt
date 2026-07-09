package com.tally.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tally.app.ui.screens.LibraryScreen
import com.tally.app.ui.screens.ProfileScreen
import com.tally.app.ui.screens.SearchScreen

// Top-level navigation graph
// Maps route strings to their corresponding Compose screens
@Composable
fun TallyNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY,
        modifier = modifier
    ) {
        // Library tab — shows user's watch lists
        composable(Routes.LIBRARY) {
            LibraryScreen()
        }

        // Search tab — search movies and shows
        composable(Routes.SEARCH) {
            SearchScreen()
        }

        // Profile tab — user settings and stats
        composable(Routes.PROFILE) {
            ProfileScreen()
        }
    }
}
