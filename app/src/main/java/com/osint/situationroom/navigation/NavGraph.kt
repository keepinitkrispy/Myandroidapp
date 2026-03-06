package com.osint.situationroom.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.osint.situationroom.ui.analysis.AnalysisScreen
import com.osint.situationroom.ui.analysis.AnalysisViewModel
import com.osint.situationroom.ui.dashboard.DashboardScreen
import com.osint.situationroom.ui.dashboard.DashboardViewModel
import com.osint.situationroom.ui.feed.FeedViewModel
import com.osint.situationroom.ui.feed.IntelFeedScreen
import com.osint.situationroom.ui.map.ConflictMapScreen
import com.osint.situationroom.ui.map.MapViewModel
import com.osint.situationroom.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Situation", Icons.Default.Dashboard)
    object Map       : Screen("map",       "Map",       Icons.Default.Map)
    object Feed      : Screen("feed",      "Intel",     Icons.Default.Feed)
    object Analysis  : Screen("analysis",  "Analysis",  Icons.Default.Analytics)
}

val screens = listOf(Screen.Dashboard, Screen.Map, Screen.Feed, Screen.Analysis)

@Composable
fun SituationRoomNavGraph() {
    val navController = rememberNavController()

    // Shared ViewModels (persist across navigation)
    val dashboardVm: DashboardViewModel = viewModel()
    val mapVm: MapViewModel = viewModel()
    val feedVm: FeedViewModel = viewModel()
    val analysisVm: AnalysisViewModel = viewModel()

    Scaffold(
        containerColor = BgPrimary,
        bottomBar = {
            NavigationBar(
                containerColor = BgDeep,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.label,
                                tint = if (selected) CyanPrimary else TextMuted
                            )
                        },
                        label = {
                            Text(
                                screen.label.uppercase(),
                                fontSize = 9.sp,
                                fontFamily = MonoFamily,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) CyanPrimary else TextMuted,
                                letterSpacing = 0.5.sp
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanPrimary,
                            unselectedIconColor = TextMuted,
                            indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardVm,
                    onViewAllEvents = { navController.navigate(Screen.Feed.route) },
                    onViewMap = { navController.navigate(Screen.Map.route) }
                )
            }
            composable(Screen.Map.route) {
                ConflictMapScreen(viewModel = mapVm)
            }
            composable(Screen.Feed.route) {
                IntelFeedScreen(viewModel = feedVm)
            }
            composable(Screen.Analysis.route) {
                AnalysisScreen(viewModel = analysisVm)
            }
        }
    }
}
