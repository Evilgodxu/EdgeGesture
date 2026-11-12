package com.edgegesture.evilgodxu.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.edgegesture.evilgodxu.screens.backtap.BackTapScreen
import com.edgegesture.evilgodxu.screens.blacklist.AppBlacklistScreen
import com.edgegesture.evilgodxu.screens.expandpanel.ExpandPanelScreen
import com.edgegesture.evilgodxu.screens.gesture.EdgeGestureConfigScreen
import com.edgegesture.evilgodxu.screens.gesture.EdgeType
import com.edgegesture.evilgodxu.screens.gesture.GestureSettingsScreen
import com.edgegesture.evilgodxu.screens.launchblock.LaunchBlockScreen
import com.edgegesture.evilgodxu.screens.settings.SettingsScreen

// 导航宿主：统一走路由栈；页面返回交给 NavDisplay 分发，
// 页面级 ViewModel 的生命周期经装饰器绑定到各自的导航条目，出栈即回收
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    startDestination: GestureRoute = GestureRoute,
) {
    val backStack = rememberNavBackStack(startDestination)

    val goBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    NavDisplay(
        backStack = backStack,
        onBack = goBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<GestureRoute> {
                GestureSettingsScreen(
                    onNavigateToSettings = { backStack.add(SettingsRoute) },
                    onNavigateToBlacklist = { backStack.add(BlacklistRoute) },
                    onNavigateToLaunchBlock = { backStack.add(LaunchBlockRoute) },
                    onNavigateToBackTap = { backStack.add(BackTapRoute) },
                    onNavigateToLeftEdge = { backStack.add(LeftEdgeConfigRoute) },
                    onNavigateToRightEdge = { backStack.add(RightEdgeConfigRoute) },
                    onNavigateToBottomEdge = { backStack.add(BottomEdgeConfigRoute) },
                    onNavigateToExpandPanel = { backStack.add(ExpandPanelRoute) },
                )
            }

            entry<SettingsRoute> {
                SettingsScreen(onNavigateBack = goBack)
            }

            entry<BlacklistRoute> {
                AppBlacklistScreen(onNavigateBack = goBack)
            }

            entry<LaunchBlockRoute> {
                LaunchBlockScreen(onNavigateBack = goBack)
            }

            entry<BackTapRoute> {
                BackTapScreen(onNavigateBack = goBack)
            }

            entry<LeftEdgeConfigRoute> {
                EdgeGestureConfigScreen(edgeType = EdgeType.LEFT, onNavigateBack = goBack)
            }

            entry<RightEdgeConfigRoute> {
                EdgeGestureConfigScreen(edgeType = EdgeType.RIGHT, onNavigateBack = goBack)
            }

            entry<BottomEdgeConfigRoute> {
                EdgeGestureConfigScreen(edgeType = EdgeType.BOTTOM, onNavigateBack = goBack)
            }

            entry<ExpandPanelRoute> {
                ExpandPanelScreen(onNavigateBack = goBack)
            }
        },
    )
}
