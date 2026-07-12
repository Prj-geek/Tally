package com.tally.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tally.app.data.sync.SyncManager
import com.tally.app.navigation.Routes
import com.tally.app.navigation.TallyNavHost
import com.tally.app.ui.theme.TallyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private val bottomNavItems = listOf(
    BottomNavItem("Shows", Icons.Default.Tv, Routes.SHOWS),
    BottomNavItem("Movies", Icons.Default.Movie, Routes.MOVIES),
    BottomNavItem("Search", Icons.Default.Search, Routes.SEARCH),
    BottomNavItem("Profile", Icons.Default.Person, Routes.PROFILE),
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncManager.sync()
        enableEdgeToEdge()
        setContent {
            TallyTheme {
                TallyMainScreen()
            }
        }
    }
}

// Main screen scaffold containing the bottom navigation bar and the nav host
@Composable
fun TallyMainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route != Routes.DETAIL

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        TallyNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
