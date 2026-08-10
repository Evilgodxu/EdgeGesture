package com.edgegesture.evilgodxu.screens.settings.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.app.DataConfigManager
import kotlinx.coroutines.launch

@Composable
fun GestureConfigDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<ByteArray?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    // 在组合阶段解析字符串资源，协程内无法调用 stringResource
    val exportFailedMessage = stringResource(R.string.gesture_config_export_failed)
    val exportErrorTemplate = stringResource(R.string.gesture_config_export_error)
    val exportSuccessMessage = stringResource(R.string.gesture_config_export_success)
    val importFailedReadMessage = stringResource(R.string.gesture_config_import_failed_read)
    val importErrorTemplate = stringResource(R.string.gesture_config_import_error)
    val emptyMessage = stringResource(R.string.gesture_config_empty)
    val importSuccessMessage = stringResource(R.string.gesture_config_import_success)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(DataConfigManager.export(context))
                } ?: error(exportFailedMessage)
            }.onFailure {
                message = exportErrorTemplate.format(it.message ?: "")
            }.onSuccess {
                message = exportSuccessMessage
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error(importFailedReadMessage)
            }.onFailure {
                message = importErrorTemplate.format(it.message ?: "")
            }.onSuccess {
                pendingImport = it
                showImportConfirm = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.gesture_config_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.gesture_config_desc))
                message?.let {
                    Text(it, modifier = Modifier.padding(top = 12.dp))
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = { importLauncher.launch(arrayOf("application/json", "text/json")) }) {
                    Text(stringResource(R.string.gesture_config_import))
                }
                Button(
                    onClick = { exportLauncher.launch("edgegesture-config.json") },
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(stringResource(R.string.gesture_config_export))
                }
            }
        }
    )

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false; pendingImport = null },
            title = { Text(stringResource(R.string.gesture_config_import_confirm_title)) },
            text = { Text(stringResource(R.string.gesture_config_import_confirm_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    scope.launch {
                        runCatching {
                            DataConfigManager.import(context, pendingImport ?: error(emptyMessage))
                        }.onFailure {
                            message = importErrorTemplate.format(it.message ?: "")
                        }.onSuccess {
                            message = importSuccessMessage
                        }
                        pendingImport = null
                    }
                }) { Text(stringResource(R.string.gesture_config_import_confirm_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false; pendingImport = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}
