package com.edgegesture.evilgodxu.screens.expandpanel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgegesture.evilgodxu.screens.expandpanel.assembly.ExpandPanelAssembly
import com.edgegesture.evilgodxu.screens.expandpanel.component.dialog.ShortcutAppPickerDialog
import com.edgegesture.evilgodxu.screens.gesture.service.FreeformAppLauncher
import com.edgegesture.evilgodxu.ui.adaptive.currentWindowSizeClass

// 页面入口：状态与事件上提，应用选择器挂在骨架层，与形态无关
@Composable
fun ExpandPanelScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExpandPanelViewModel = viewModel(),
) {
    val context = LocalContext.current
    val freeformAppLauncher = remember(context) { FreeformAppLauncher(context) }
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExpandPanelAssembly(
        windowSizeClass = currentWindowSizeClass(),
        state = shortcuts,
        onBack = onNavigateBack,
        onPickShortcut = viewModel::showAppPicker,
        onSaveShortcut = viewModel::saveShortcut,
        onFreeformToggle = viewModel::saveShortcutFreeform,
    )

    val pickerIndex = uiState.pickerIndex
    if (pickerIndex != null) {
        ShortcutAppPickerDialog(
            onAppSelected = { packageName ->
                // 先落库再关闭：关闭会重置槽位下标
                viewModel.saveShortcut(pickerIndex, packageName)
                viewModel.dismissAppPicker()
            },
            onLaunchApp = { packageName ->
                freeformAppLauncher.launch(packageName, false)
                viewModel.dismissAppPicker()
            },
            onDismiss = viewModel::dismissAppPicker,
        )
    }
}
