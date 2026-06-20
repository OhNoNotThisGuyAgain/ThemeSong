package com.spotzones.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spotzones.presentation.AppUiState
import com.spotzones.presentation.screens.dashboard.DashboardScreen
import com.spotzones.presentation.screens.history.HistoryScreen
import com.spotzones.presentation.screens.map.MapScreen
import com.spotzones.presentation.screens.nowplaying.NowPlayingScreen
import com.spotzones.presentation.screens.onboarding.OnboardingScreen
import com.spotzones.presentation.screens.search.SearchScreen
import com.spotzones.presentation.screens.settings.SettingsScreen
import com.spotzones.presentation.screens.zoneeditor.ZoneEditorScreen

/**
 * Root composable: gates onboarding, hosts the bottom-nav shell and wires navigation.
 * Tab switches cross-fade; forward navigation (editor, search) slides, giving the app a fluid,
 * premium feel without bespoke transition code per screen.
 */
@Composable
fun SpotZonesRoot(appState: AppUiState) {
    if (!appState.onboardingComplete) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            OnboardingScreen()
        }
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    val destinations = TopLevelDestination.entries
                    destinations.forEach { destination ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            alwaysShowLabel = false,
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.fillMaxSize().padding(inner),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenNowPlaying = { navController.navigateToTab(Routes.NOW_PLAYING) },
                    onOpenMap = { navController.navigateToTab(Routes.MAP) },
                    onEditZone = { id -> navController.navigate(Routes.zoneEditor(zoneId = id)) },
                )
            }
            composable(Routes.MAP) {
                MapScreen(
                    onCreateZoneAt = { lat, lng -> navController.navigate(Routes.zoneEditor(lat = lat, lng = lng)) },
                    onEditZone = { id -> navController.navigate(Routes.zoneEditor(zoneId = id)) },
                )
            }
            composable(Routes.NOW_PLAYING) { NowPlayingScreen() }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }

            composable(
                route = Routes.SEARCH,
                enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(260)) + fadeIn(tween(260)) },
                exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)) + fadeOut(tween(220)) },
            ) {
                SearchScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.ZONE_EDITOR_PATTERN,
                arguments = listOf(
                    navArgument(Routes.ZONE_EDITOR_ARG_ID) { type = NavType.StringType },
                    navArgument(Routes.ZONE_EDITOR_ARG_LAT) { type = NavType.StringType; defaultValue = "NaN" },
                    navArgument(Routes.ZONE_EDITOR_ARG_LNG) { type = NavType.StringType; defaultValue = "NaN" },
                ),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(260)) + fadeIn(tween(260)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(220)) + fadeOut(tween(220)) },
            ) {
                ZoneEditorScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}

private fun androidx.navigation.NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
