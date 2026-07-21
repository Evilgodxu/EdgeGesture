package com.edgegesture.evilgodxu.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.edgegesture.evilgodxu.screens.backtap.BackTapScreen
import com.edgegesture.evilgodxu.screens.blacklist.AppBlacklistScreen
import com.edgegesture.evilgodxu.screens.gesture.EdgeGestureConfigScreen
import com.edgegesture.evilgodxu.screens.gesture.EdgeType
import com.edgegesture.evilgodxu.screens.gesture.GestureSettingsScreen
import com.edgegesture.evilgodxu.screens.launchblock.LaunchBlockScreen
import com.edgegesture.evilgodxu.screens.settings.SettingsScreen
import com.edgegesture.evilgodxu.screens.settings.ThemeMode
import kotlinx.serialization.Serializable

@Serializable
data object GestureRoute

@Serializable
data object SettingsRoute

@Serializable
data object BlacklistRoute

@Serializable
data object LaunchBlockRoute

@Serializable
data object BackTapRoute

@Serializable
data object LeftEdgeConfigRoute

@Serializable
data object RightEdgeConfigRoute

@Serializable
data object BottomEdgeConfigRoute

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: GestureRoute = GestureRoute,
    onThemeChange: (ThemeMode) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<GestureRoute> {
            GestureSettingsScreen(
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute)
                },
                onNavigateToBlacklist = {
                    navController.navigate(BlacklistRoute)
                },
                onNavigateToLaunchBlock = {
                    navController.navigate(LaunchBlockRoute)
                },
                onNavigateToBackTap = {
                    navController.navigate(BackTapRoute)
                },
                onNavigateToLeftEdge = {
                    navController.navigate(LeftEdgeConfigRoute)
                },
                onNavigateToRightEdge = {
                    navController.navigate(RightEdgeConfigRoute)
                },
                onNavigateToBottomEdge = {
                    navController.navigate(BottomEdgeConfigRoute)
                }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onThemeChange = { themeMode ->
                    onThemeChange(themeMode)
                }
            )
        }

        composable<BlacklistRoute> {
            AppBlacklistScreen(
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<LaunchBlockRoute> {
            LaunchBlockScreen(
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<BackTapRoute> {
            BackTapScreen(
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<LeftEdgeConfigRoute> {
            EdgeGestureConfigScreen(
                edgeType = EdgeType.LEFT,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<RightEdgeConfigRoute> {
            EdgeGestureConfigScreen(
                edgeType = EdgeType.RIGHT,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<BottomEdgeConfigRoute> {
            EdgeGestureConfigScreen(
                edgeType = EdgeType.BOTTOM,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
