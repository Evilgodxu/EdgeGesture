package com.edgegesture.evilgodxu.screens.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.appContainer
import com.edgegesture.evilgodxu.data.app.AppRepository
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsKeys
import com.edgegesture.evilgodxu.screens.gesture.assembly.EdgeGestureConfigAssembly
import com.edgegesture.evilgodxu.screens.gesture.component.dialog.ActionSelectionDialog
import com.edgegesture.evilgodxu.screens.gesture.component.dialog.AppPickerDialog
import com.edgegesture.evilgodxu.screens.gesture.component.action.gestureActionForDialog
import com.edgegesture.evilgodxu.screens.gesture.component.action.getActionDisplayName

// 页面入口：滑杆的本地预览状态与两个弹窗留在此处，布局交给组装器
@Composable
fun EdgeGestureConfigScreen(
    edgeType: EdgeType,
    onNavigateBack: () -> Unit,
    viewModel: EdgeGestureConfigViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentSettings = settings

    var selectedSegment by remember { mutableIntStateOf(1) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var currentActionKey by remember { mutableStateOf<Preferences.Key<String>?>(null) }

    // 应用名映射，用于「启动应用」动作显示绑定的目标应用
    val appRepository = LocalContext.current.appContainer.appRepository
    val apps by appRepository.appsFlow.collectAsStateWithLifecycle()
    val appNameByPackage = remember(apps) { apps.associate { it.packageName to it.appName } }

    val title = when (edgeType) {
        EdgeType.LEFT -> stringResource(R.string.gesture_config_left)
        EdgeType.RIGHT -> stringResource(R.string.gesture_config_right)
        EdgeType.BOTTOM -> stringResource(R.string.gesture_config_bottom)
    }

    // 当前边缘的本地状态，滑块变化时立即更新预览
    val initW = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftEdgeWidth ?: 20
        EdgeType.RIGHT -> currentSettings?.rightEdgeWidth ?: 20
        EdgeType.BOTTOM -> currentSettings?.bottomEdgeHeight ?: 20
    }
    val initH = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftEdgeHeightPercent ?: 60
        EdgeType.RIGHT -> currentSettings?.rightEdgeHeightPercent ?: 60
        EdgeType.BOTTOM -> currentSettings?.bottomEdgeWidthPercent ?: 80
    }
    val initP = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftEdgePositionPercent ?: 90
        EdgeType.RIGHT -> currentSettings?.rightEdgePositionPercent ?: 90
        else -> 0
    }
    var localWidth by remember(currentSettings, edgeType) { mutableIntStateOf(initW) }
    var localHeightPercent by remember(currentSettings, edgeType) { mutableIntStateOf(initH) }
    var localPositionPercent by remember(currentSettings, edgeType) { mutableIntStateOf(initP) }

    val maxSegments = when (edgeType) {
        EdgeType.LEFT -> currentSettings?.leftSegmentCount ?: 1
        EdgeType.RIGHT -> currentSettings?.rightSegmentCount ?: 1
        EdgeType.BOTTOM -> currentSettings?.bottomSegmentCount ?: 1
    }
    // 分段数量本地状态，拖动预览用，松手时才持久化
    var localSegmentCount by remember(currentSettings, edgeType) { mutableIntStateOf(maxSegments) }

    EdgeGestureConfigAssembly(
        edgeType = edgeType,
        title = title,
        settings = currentSettings,
        selectedSegment = selectedSegment,
        localWidth = localWidth,
        localHeightPercent = localHeightPercent,
        localPositionPercent = localPositionPercent,
        localSegmentCount = localSegmentCount,
        maxSegments = maxSegments,
        appNameByPackage = appNameByPackage,
        onBack = onNavigateBack,
        onSegmentSelected = { selectedSegment = it },
        onWidthChange = { localWidth = it },
        onHeightPercentChange = { localHeightPercent = it },
        onPositionPercentChange = { localPositionPercent = it },
        onWidthChangeFinished = { viewModel.saveWidth(edgeType, localWidth) },
        onHeightPercentChangeFinished = { viewModel.saveHeightPercent(edgeType, localHeightPercent) },
        onPositionPercentChangeFinished = { viewModel.savePositionPercent(edgeType, localPositionPercent) },
        onSegmentCountChange = { count ->
            localSegmentCount = count
            if (selectedSegment > count) selectedSegment = count
        },
        onSegmentCountChangeFinished = { viewModel.saveSegmentCount(edgeType, localSegmentCount) },
        onActionClick = { key ->
            currentActionKey = key
            showActionDialog = true
        },
    )

    // 动作选择对话框
    val pendingKey = currentActionKey
    if (showActionDialog && pendingKey != null && currentSettings != null) {
        ActionSelectionDialog(
            currentAction = gestureActionForDialog(edgeType, selectedSegment, currentSettings, pendingKey),
            currentRemindMinutes = currentSettings.remindMinutes[pendingKey.name]
                ?: GestureSettingsKeys.DEFAULT_REMIND_MINUTES,
            onDismiss = { showActionDialog = false },
            onActionSelected = { action ->
                if (action == GestureAction.LAUNCH_APP) {
                    // 启动应用需先选定目标应用，交由应用选择对话框完成保存
                    showActionDialog = false
                    showAppPicker = true
                } else {
                    viewModel.saveGestureAction(pendingKey, action)
                    showActionDialog = false
                }
            },
            onRemindConfirmed = { minutes ->
                viewModel.saveGestureAction(pendingKey, GestureAction.REMIND)
                viewModel.saveRemindMinutes(pendingKey, minutes)
                showActionDialog = false
            },
            getActionDisplayName = { getActionDisplayName(it) },
        )
    }

    // 应用选择对话框
    if (showAppPicker && pendingKey != null) {
        AppPickerDialog(
            onDismiss = {
                showAppPicker = false
                currentActionKey = null
            },
            onAppSelected = { packageName, useFreeform ->
                viewModel.saveGestureAction(pendingKey, GestureAction.LAUNCH_APP)
                viewModel.saveLaunchAppTarget(pendingKey, packageName)
                viewModel.saveLaunchAppFreeform(pendingKey, useFreeform)
                showAppPicker = false
                currentActionKey = null
            },
            initialFreeform = currentSettings?.launchAppFreeform?.get(pendingKey.name) == true,
        )
    }
}
