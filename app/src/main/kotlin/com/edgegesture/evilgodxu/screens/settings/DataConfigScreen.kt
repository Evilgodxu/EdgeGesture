package com.edgegesture.evilgodxu.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var message by remember { mutableStateOf<String?>(null) }

    fun refresh() = scope.launch { items = DataConfigManager.listData(context) }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("音乐缓存") },
            navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "缓存数据",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
            )
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    items.forEach { item ->
                        SelectableDataRow(
                            title = dataName(item.type),
                            subtitle = formatSize(item.size),
                            selected = checked[item.type] == true,
                            onClick = { checked[item.type] = it }
                        )
                    }
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
            message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
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
}

@androidx.compose.runtime.Composable
private fun SelectableDataRow(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: (Boolean) -> Unit
) {
    Surface(
        onClick = { onClick(!selected) },
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.weight(1f))
            subtitle?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

private fun dataName(type: ManagedDataType): String = when (type) {
    ManagedDataType.MUSIC_COVERS -> "音乐封面缓存"
    ManagedDataType.MUSIC_LYRICS -> "音乐歌词缓存"
}

private fun formatSize(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", size / 1024f)
    else -> String.format(Locale.getDefault(), "%.1f MB", size / (1024f * 1024f))
}
