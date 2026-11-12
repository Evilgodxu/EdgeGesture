package com.edgegesture.evilgodxu.screens.expandpanel.component.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.gesture.service.expandpanel.AppPickerScreen

// 快捷方式槽位的应用选择器：选中即保存，长按项可直接启动
@Composable
fun ShortcutAppPickerDialog(
    onAppSelected: (String) -> Unit,
    onLaunchApp: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.expand_panel_app_picker_title),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            AppPickerScreen(
                onAppSelected = onAppSelected,
                onLaunchApp = onLaunchApp,
                onCancel = onDismiss,
            )
        },
        confirmButton = {},
    )
}
