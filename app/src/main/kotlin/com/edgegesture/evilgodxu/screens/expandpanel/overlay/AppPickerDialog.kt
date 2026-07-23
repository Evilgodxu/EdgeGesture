package com.edgegesture.evilgodxu.screens.expandpanel.overlay

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.gesture.service.expandpanel.AppPickerScreen

// 应用选择器对话框 — 模态覆盖层
@Composable
fun AppPickerDialog(
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.expand_panel_app_picker_title),
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            AppPickerScreen(
                onAppSelected = onAppSelected,
                onCancel = onDismiss,
            )
        },
        confirmButton = {},
    )
}
