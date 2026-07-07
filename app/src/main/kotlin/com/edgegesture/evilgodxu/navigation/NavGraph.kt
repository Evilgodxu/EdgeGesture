package com.edgegesture.evilgodxu.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.edgegesture.evilgodxu.screens.gesture.GestureSettingsScreen
import com.edgegesture.evilgodxu.screens.settings.SettingsScreen
import com.edgegesture.evilgodxu.screens.settings.ThemeMode
import kotlinx.serialization.Serializable

@Serializable
data object GestureRoute

@Serializable
data object SettingsRoute

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
    }
}
