package com.byss.jh.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.byss.jh.data.gesture.initBlacklistIfNeeded
import com.byss.jh.data.privacy.isPrivacyAgreed
import com.byss.jh.data.privacy.savePrivacyAgreed
import com.byss.jh.feature.gesture.GestureSettingsScreen
import com.byss.jh.feature.privacy.PrivacyScreen
import com.byss.jh.feature.settings.AppLanguage
import com.byss.jh.feature.settings.SettingsScreen
import com.byss.jh.feature.settings.ThemeMode
import com.byss.jh.feature.splash.SplashScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Privacy : Screen("privacy")
    data object Gesture : Screen("gesture")
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    onThemeChange: (com.byss.jh.feature.settings.ThemeMode) -> Unit = {},
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    val context = LocalContext.current
    val privacyAgreed by context.isPrivacyAgreed().collectAsState(initial = false)

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    // 首次使用需同意隐私政策，已同意则直接进入手势设置页
                    if (privacyAgreed) {
                        navController.navigate(Screen.Gesture.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Privacy.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(
                onAgree = {
                    val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                    appScope.launch {
                        context.savePrivacyAgreed(true)
                        context.initBlacklistIfNeeded()
                    }
                    navController.navigate(Screen.Gesture.route) {
                        popUpTo(Screen.Privacy.route) { inclusive = true }
                    }
                }
            )
        }

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
