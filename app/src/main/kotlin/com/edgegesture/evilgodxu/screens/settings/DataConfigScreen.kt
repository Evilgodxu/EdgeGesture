package com.edgegesture.evilgodxu.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.edgegesture.evilgodxu.data.app.DataConfigManager
import com.edgegesture.evilgodxu.data.app.ManagedDataItem
import com.edgegesture.evilgodxu.data.app.ManagedDataType
import com.edgegesture.evilgodxu.screens.gesture.service.musicpanel.MusicPanelStateHolder
import kotlinx.coroutines.launch
import java.util.Locale

@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.runtime.Composable
fun DataConfigScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ManagedDataItem>>(emptyList()) }
    val checked = remember { mutableStateMapOf<ManagedDataType, Boolean>() }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<ByteArray?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun refresh() = scope.launch { items = DataConfigManager.listData(context) }
    LaunchedEffect(Unit) { refresh() }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(DataConfigManager.export(context)) } }
                .onFailure { message = "导出失败：${it.message}" }
                .onSuccess { message = "配置已导出" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("无法读取文件") }
                .onFailure { message = "导入失败：${it.message}" }
                .onSuccess { pendingImport = it; showImportConfirm = true }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("数据与配置") },
            navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("数据管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                val allChecked = items.isNotEmpty() && items.all { checked[it.type] == true }
                Checkbox(checked = allChecked, onCheckedChange = { value -> items.forEach { checked[it.type] = value } })
                Text("全选")
            }
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = checked[item.type] == true, onCheckedChange = { checked[item.type] = it })
                    Text(dataName(item.type), modifier = Modifier.weight(1f))
                    Text(formatSize(item.size), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(
                onClick = { showClearConfirm = true },
                enabled = checked.values.any { it },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteSweep, null)
                Text("清理所选数据", modifier = Modifier.padding(start = 8.dp))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("手势配置", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { exportLauncher.launch("edgegesture-config.json") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FileDownload, null)
                Text("导出手势配置", modifier = Modifier.padding(start = 8.dp))
            }
            Button(onClick = { importLauncher.launch(arrayOf("application/json", "text/json")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FileUpload, null)
                Text("导入手势配置", modifier = Modifier.padding(start = 8.dp))
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp)) }
        }
    }

    if (showClearConfirm) AlertDialog(
        onDismissRequest = { showClearConfirm = false },
        title = { Text("确认清理") },
        text = { Text("清理所选数据后，相关模块将重新初始化。音乐数据清理会停止并释放播放器。") },
        confirmButton = { TextButton(onClick = {
            showClearConfirm = false
            scope.launch {
                val selected = checked.filterValues { it }.keys
                DataConfigManager.clear(context, selected) { MusicPanelStateHolder.state.release() }
                checked.clear()
                refresh()
                message = "清理完成，相关数据已重新初始化"
            }
        }) { Text("清理") } },
        dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("取消") } }
    )

    if (showImportConfirm) AlertDialog(
        onDismissRequest = { showImportConfirm = false; pendingImport = null },
        title = { Text("覆盖手势配置") },
        text = { Text("导入后将完全覆盖当前手势配置和启动拦截配置。黑名单、扩展面板快捷方式、主题和语言不会改变。") },
        confirmButton = { TextButton(onClick = {
            showImportConfirm = false
            scope.launch {
                runCatching { DataConfigManager.import(context, pendingImport ?: error("配置为空")) }
                    .onFailure { message = "导入失败：${it.message}" }
                    .onSuccess { message = "配置已导入" }
                pendingImport = null
            }
        }) { Text("继续") } },
        dismissButton = { TextButton(onClick = { showImportConfirm = false; pendingImport = null }) { Text("取消") } }
    )
}

private fun dataName(type: ManagedDataType): String = when (type) {
    ManagedDataType.APP_ICONS -> "应用图标缓存"
    ManagedDataType.MUSIC_COVERS -> "音乐封面缓存"
    ManagedDataType.MUSIC_LYRICS -> "音乐歌词缓存"
    ManagedDataType.MUSIC_PLAYLIST -> "音乐播放列表缓存"
    ManagedDataType.MUSIC_POSITION -> "音乐播放进度与当前歌曲"
    ManagedDataType.UPDATE_CACHE -> "更新下载缓存"
    ManagedDataType.STATS -> "统计信息"
    ManagedDataType.TEMP -> "其他临时缓存"
}

private fun formatSize(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", size / 1024f)
    else -> String.format(Locale.getDefault(), "%.1f MB", size / (1024f * 1024f))
}
