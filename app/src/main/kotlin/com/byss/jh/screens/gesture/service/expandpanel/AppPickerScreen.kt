package com.byss.jh.screens.gesture.service.expandpanel

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.byss.jh.R
import com.byss.jh.data.app.AppInfo
import com.byss.jh.data.app.AppRepository
import org.koin.compose.koinInject

// 应用选择器组件
// 使用 AppRepository 缓存实现即时加载，无需等待扫描
// 使用 LazyColumn 优化大量应用列表的渲染性能
@Composable
fun AppPickerScreen(
    onAppSelected: (String) -> Unit,
    onCancel: () -> Unit,
    appRepository: AppRepository = koinInject()
) {
    val context = LocalContext.current

    // 从缓存仓库获取应用列表，实现即时显示
    val apps by appRepository.appsFlow.collectAsState()
    val isLoading by appRepository.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // 本地搜索过滤，无需重新扫描
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) {
            apps
        } else {
            val lowerQuery = searchQuery.lowercase()
            apps.filter { app ->
                app.appName.lowercase().contains(lowerQuery) ||
                app.packageName.lowercase().contains(lowerQuery)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.expand_panel_app_picker_title),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.expand_panel_cancel), color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(stringResource(R.string.expand_panel_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 使用 LazyColumn 替代 Column + verticalScroll，实现按需加载和复用
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // 显示加载指示器或应用列表
            if (isLoading && apps.isEmpty()) {
                // 首次加载时显示加载指示器
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.expand_panel_loading_apps),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName } // 使用包名作为 key 优化列表项复用
                    ) { app ->
                        AppPickerItem(
                            app = app,
                            onClick = { onAppSelected(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerItem(
    app: AppInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 优先使用扫描时缓存的图标，回退到 PackageManager 实时查询
        val iconBitmap by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(
            initialValue = null,
            key1 = app.packageName,
            key2 = app.iconPath
        ) {
            value = loadAppIconBitmap(context, app)
        }

        if (iconBitmap != null) {
            Image(
                painter = BitmapPainter(iconBitmap!!.asImageBitmap()),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = app.appName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = app.packageName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 加载应用图标：优先读取缓存文件，失败则回退到 PackageManager
private fun loadAppIconBitmap(
    context: android.content.Context,
    app: AppInfo
): android.graphics.Bitmap? {
    if (app.iconPath.isNotBlank()) {
        runCatching {
            android.graphics.BitmapFactory.decodeFile(app.iconPath)
        }.getOrNull()?.let { return it }
    }
    return runCatching {
        context.packageManager.getApplicationIcon(app.packageName).toBitmap()
    }.getOrNull()
}
