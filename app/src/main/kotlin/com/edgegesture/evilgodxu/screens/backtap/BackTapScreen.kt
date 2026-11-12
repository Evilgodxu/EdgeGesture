package com.edgegesture.evilgodxu.screens.backtap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsKeys
import com.edgegesture.evilgodxu.screens.backtap.assembly.BackTapAssembly
import com.edgegesture.evilgodxu.screens.gesture.components.ActionSelectionDialog
import com.edgegesture.evilgodxu.screens.gesture.components.AppPickerDialog
import com.edgegesture.evilgodxu.screens.gesture.components.getActionDisplayName
import com.edgegesture.evilgodxu.ui.adaptive.currentWindowSizeClass

// 页面入口：状态与事件上提，应用名映射在此准备，两个弹窗挂在骨架层
@Composable
fun BackTapScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackTapViewModel = viewModel(),
) {
    val gestureSettings by viewModel.gestureSettings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 应用名映射，用于「启动应用」动作显示绑定的目标应用
    val appRepository = AppRepository.getInstance(LocalContext.current)
    val apps by appRepository.appsFlow.collectAsStateWithLifecycle()
    val appNameByPackage = remember(apps) { apps.associate { it.packageName to it.appName } }

    val settings = gestureSettings

    BackTapAssembly(
        windowSizeClass = currentWindowSizeClass(),
        settings = settings,
        appNameByPackage = appNameByPackage,
        onBack = onNavigateBack,
        onEnabledChange = viewModel::setBackTapEnabled,
        onSensitivityChange = viewModel::setBackTapSensitivity,
        onRangeChange = viewModel::setBackTapRange,
        onModeChange = viewModel::setBackTapMode,
        onPauseOnChargingChange = viewModel::setBackTapPauseOnCharging,
        onShowActionDialog = viewModel::showActionDialog,
    )

    if (uiState.showActionDialog && settings != null) {
        ActionSelectionDialog(
            currentAction = settings.backTapAction,
            currentRemindMinutes = settings.remindMinutes[GestureSettingsKeys.BACK_TAP_ACTION.name]
                ?: GestureSettingsKeys.DEFAULT_REMIND_MINUTES,
            onDismiss = viewModel::dismissActionDialog,
            onActionSelected = { action ->
                if (action == GestureAction.LAUNCH_APP) {
                    // 启动应用需先选定目标应用，交由应用选择对话框完成保存
                    viewModel.showAppPickerFromActionDialog()
                } else {
                    viewModel.setBackTapAction(action)
                    viewModel.dismissActionDialog()
                }
            },
            onRemindConfirmed = { minutes ->
                viewModel.setBackTapAction(GestureAction.REMIND)
                viewModel.setRemindMinutes(minutes)
                viewModel.dismissActionDialog()
            },
            getActionDisplayName = { getActionDisplayName(it) },
        )
    }

    if (uiState.showAppPicker) {
        AppPickerDialog(
            onDismiss = viewModel::dismissAppPicker,
            onAppSelected = { packageName, useFreeform ->
                viewModel.setBackTapAction(GestureAction.LAUNCH_APP)
                viewModel.setLaunchAppTarget(packageName)
                viewModel.setLaunchAppFreeform(useFreeform)
                viewModel.dismissAppPicker()
            },
            initialFreeform = settings?.launchAppFreeform?.get(GestureSettingsKeys.BACK_TAP_ACTION.name) == true,
        )
    }
}
