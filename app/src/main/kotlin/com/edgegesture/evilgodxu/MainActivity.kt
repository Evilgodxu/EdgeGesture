package com.edgegesture.evilgodxu

import android.app.ActivityManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.navigation.NavGraph
import com.edgegesture.evilgodxu.ui.adaptive.ProvideWindowSizeClass
import com.edgegesture.evilgodxu.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 设置系统栏控制
        setupSystemBars()

        // 监听生命周期，更新前台状态
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> UpdateCheckWorker.isAppInForeground = true
                Lifecycle.Event.ON_STOP -> UpdateCheckWorker.isAppInForeground = false
                else -> {}
            }
        })

        setContent {
            ProvideWindowSizeClass {
                MyApplicationTheme {
                    val navController = rememberNavController()

                    // 更新对话框状态
                    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                    var showUpdateDialog by remember { mutableStateOf(false) }

                    // 从通知打开时检查是否携带 show_update 标记
                    LaunchedEffect(Unit) {
                        if (intent?.getBooleanExtra("show_update", false) == true) {
                            val info = UpdateManager.checkForUpdate(this@MainActivity, force = true)
                            if (info != null) {
                                updateInfo = info
                                showUpdateDialog = true
                            }
                        }
                    }

                    // 回到前台时检查是否有待更新
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
                                scope.launch {
                                    val info = UpdateManager.checkForUpdate(this@MainActivity)
                                    if (info != null) {
                                        updateInfo = info
                                        showUpdateDialog = true
                                    }
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    NavGraph(
                        navController = navController,
                        onThemeChange = { themeMode ->
                            // 主题切换通过 Compose 重组实现，无需重建
                        }
                    )

                    // 更新对话框
                    if (showUpdateDialog && updateInfo != null) {
                        UpdateDialog(
                            updateInfo = updateInfo!!,
                            onDownload = {
                                showUpdateDialog = false
                                lifecycleScope.launch {
                                    UpdateManager.downloadAndInstall(this@MainActivity, updateInfo!!)
                                }
                            },
                            onIgnore = {
                                showUpdateDialog = false
                                UpdateManager.ignoreVersion(this@MainActivity, updateInfo!!.latestVersion)
                            },
                            onDismiss = {
                                showUpdateDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun setupSystemBars() {
        windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        updateSystemBarsVisibility()
    }

    private fun updateSystemBarsVisibility() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemBarsVisibility()
    }

    override fun onResume() {
        super.onResume()
        // 每次回到前台时同步隐藏后台设置，确保设置变更即时生效
        lifecycleScope.launch {
            val settings = gestureSettingsFlow().first()
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            am.appTasks.forEach { task ->
                task.setExcludeFromRecents(settings.hideFromRecents)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
private fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDownload: () -> Unit,
    onIgnore: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "发现新版本 ${updateInfo.latestVersion}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "新版本已可用，是否前往下载？",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (updateInfo.changelog.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "更新日志：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = updateInfo.changelog,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text("下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后")
            }
        }
    )
}
