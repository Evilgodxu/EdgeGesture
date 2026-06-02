package com.byss.jh.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.byss.jh.screens.gesture.GestureSettingsScreen
import com.byss.jh.screens.settings.AppLanguage
import com.byss.jh.screens.settings.SettingsScreen
import com.byss.jh.screens.settings.ThemeMode

sealed class Screen(val route: String) {
    data object Gesture : Screen("gesture")
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Gesture.route,
    onThemeChange: (ThemeMode) -> Unit = {},
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Gesture.route) {
            GestureSettingsScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    // 防止快速点击导致导航状态异常
                    if (navController.currentBackStackEntry?.destination?.route == Screen.Settings.route) {
                        navController.popBackStack()
                    }
                },
                onThemeChange = { themeMode ->
                    // 主题切换通过 Compose 重组实时生效，无需 Activity 重建
                    onThemeChange(themeMode)
                },
                onLanguageChange = { language ->
                    // 语言切换通过更新 Configuration 实现，无需重启 Activity
                    onLanguageChange(language)
                }
            )
        }
    }
}
