package com.byss.jh.ui.settings.components

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byss.jh.R
import com.byss.jh.data.gesture.addToAppSwitchBlacklist
import com.byss.jh.data.gesture.appSwitchBlacklistFlow
import com.byss.jh.data.gesture.removeFromAppSwitchBlacklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 应用信息数据类
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)

/**
 * 应用切换黑名单对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSwitchBlacklistDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val blacklist by context.appSwitchBlacklistFlow().collectAsState(initial = emptySet())
    var searchQuery by remember { mutableStateOf("") }
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(0).map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = appInfo.loadLabel(pm).toString(),
                    isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                            appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                )
            }.filter { it.packageName != context.packageName }
                .sortedWith(compareBy({ !it.isSystemApp }, { it.appName }))
            allApps = apps
            isLoading = false
        }
    }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.settings_blacklist_dialog_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxHeight(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings_blacklist_loading),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isBlacklisted = app.packageName in blacklist
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            if (isBlacklisted) {
                                                context.removeFromAppSwitchBlacklist(setOf(app.packageName))
                                            } else {
                                                context.addToAppSwitchBlacklist(setOf(app.packageName))
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isBlacklisted,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            if (checked) {
                                                context.addToAppSwitchBlacklist(setOf(app.packageName))
                                            } else {
                                                context.removeFromAppSwitchBlacklist(setOf(app.packageName))
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = app.packageName,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                if (app.isSystemApp) {
                                    Text(
                                        text = stringResource(R.string.settings_blacklist_system_tag),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.padding(4.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.settings_blacklist_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }
        },
        confirmButton = {}
    )
}
