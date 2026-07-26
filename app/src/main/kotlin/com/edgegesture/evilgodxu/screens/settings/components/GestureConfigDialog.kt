package com.edgegesture.evilgodxu.screens.settings.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.data.app.DataConfigManager
import kotlinx.coroutines.launch

@Composable
fun GestureConfigDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<ByteArray?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(DataConfigManager.export(context))
                } ?: error("无法写入文件")
            }.onFailure {
                message = "导出失败：${it.message}"
            }.onSuccess {
                message = "配置已导出"
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("无法读取文件")
            }.onFailure {
                message = "导入失败：${it.message}"
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
                text = "手势配置",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("导入或导出手势配置，不包含黑名单、扩展面板快捷方式、主题和语言。")
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
                    androidx.compose.material3.Icon(Icons.Default.FileUpload, null)
                    Text("导入", modifier = Modifier.padding(start = 8.dp))
                }
                Button(
                    onClick = { exportLauncher.launch("edgegesture-config.json") },
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    androidx.compose.material3.Icon(Icons.Default.FileDownload, null)
                    Text("导出", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    )

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false; pendingImport = null },
            title = { Text("覆盖手势配置") },
            text = { Text("导入后将完全覆盖当前手势配置和启动拦截配置。黑名单、扩展面板快捷方式、主题和语言不会改变。") },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    scope.launch {
                        runCatching {
                            DataConfigManager.import(context, pendingImport ?: error("配置为空"))
                        }.onFailure {
                            message = "导入失败：${it.message}"
                        }.onSuccess {
                            message = "配置已导入"
                        }
                        pendingImport = null
                    }
                }) { Text("继续") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false; pendingImport = null }) {
                    Text("取消")
                }
            }
        )
    }
}
