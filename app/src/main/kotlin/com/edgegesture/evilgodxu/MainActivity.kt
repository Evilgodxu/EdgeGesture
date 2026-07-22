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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.edgegesture.evilgodxu.DownloadState
import com.edgegesture.evilgodxu.UpdateInfo
import com.edgegesture.evilgodxu.UpdateManager
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.navigation.NavGraph
import com.edgegesture.evilgodxu.ui.adaptive.ProvideWindowSizeClass
import com.edgegesture.evilgodxu.ui.theme.MyApplicationTheme
import kotlinx.coroutines.cancel
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
                    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

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
                        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
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
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            scope.cancel()
                        }
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
                            downloadState = downloadState,
                            onDownload = {
                                downloadState = DownloadState.Downloading(0f)
                                lifecycleScope.launch {
                                    val success = UpdateManager.downloadAndInstall(
                                        this@MainActivity,
                                        updateInfo!!,
                                        onProgress = { progress ->
                                            downloadState = if (progress < 0f) {
                                                DownloadState.Failed("下载失败，请重试")
                                            } else {
                                                DownloadState.Downloading(progress)
                                            }
                                        }
                                    )
                                    if (success) {
                                        downloadState = DownloadState.Success
                                        showUpdateDialog = false
                                    } else if (downloadState !is DownloadState.Failed) {
                                        downloadState = DownloadState.Failed("下载失败，请重试")
                                    }
                                }
                            },
                            onOpenBrowser = {
                                val url = updateInfo!!.downloadUrl
                                if (url.startsWith("http")) {
                                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                                }
                                showUpdateDialog = false
                                downloadState = DownloadState.Idle
                            },
                            onDismiss = {
                                showUpdateDialog = false
                                downloadState = DownloadState.Idle
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
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onOpenBrowser: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDownloading = downloadState is DownloadState.Downloading
    val isFailed = downloadState is DownloadState.Failed
    val progress = (downloadState as? DownloadState.Downloading)?.progress ?: 0f

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.update_dialog_title, updateInfo.latestVersion),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (isFailed) {
                    Text(
                        text = stringResource(R.string.update_dialog_download_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (isDownloading) {
                    Text(
                        text = stringResource(R.string.update_dialog_downloading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.update_dialog_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (updateInfo.changelog.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.update_dialog_changelog_title),
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
            when {
                isFailed -> TextButton(onClick = onOpenBrowser) {
                    Text(stringResource(R.string.update_dialog_open_browser))
                }
                !isDownloading -> TextButton(onClick = onDownload) {
                    Text(stringResource(R.string.update_dialog_download))
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_dialog_later))
                }
            }
        }
    )
}
