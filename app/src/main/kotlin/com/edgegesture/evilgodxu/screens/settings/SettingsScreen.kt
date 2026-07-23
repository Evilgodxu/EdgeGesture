package com.edgegesture.evilgodxu.screens.settings

import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.edgegesture.evilgodxu.DownloadState
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.UpdateInfo
import com.edgegesture.evilgodxu.UpdateManager
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import com.edgegesture.evilgodxu.data.shizuku.ShizukuState
import com.edgegesture.evilgodxu.screens.settings.compact.CompactAssembly
import com.edgegesture.evilgodxu.screens.settings.compact.appearance_area.theme_selector.ThemeSelectorState
import com.edgegesture.evilgodxu.screens.settings.expanded.ExpandedAssembly
import com.edgegesture.evilgodxu.screens.settings.overlay.DonateDialog
import com.edgegesture.evilgodxu.screens.settings.overlay.LanguageSelectionDialog
import com.edgegesture.evilgodxu.screens.settings.overlay.OpenSourceLicensesDialog
import com.edgegesture.evilgodxu.screens.settings.overlay.ThemeSelectionDialog
import com.edgegesture.evilgodxu.screens.settings.data.AppLanguage
import com.edgegesture.evilgodxu.screens.settings.data.ThemeMode
import com.edgegesture.evilgodxu.screens.settings.data.getAppLanguage
import com.edgegesture.evilgodxu.screens.settings.data.setAppLanguage
import com.edgegesture.evilgodxu.ui.adaptive.rememberWindowSizeClass
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import rikka.shizuku.Shizuku

// 更多设置页 — 页面入口 Composable
// 职责：通过 WindowSizeClass 判断并路由至 CompactAssembly 或 ExpandedAssembly
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val currentLanguage = remember(configuration) { context.getAppLanguage() }
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) { "" }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = rememberWindowSizeClass()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // === 对话框状态 ===
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    var showOpenSourceDialog by remember { mutableStateOf(false) }

    // === 更新检查状态 ===
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showUpToDate by remember { mutableStateOf(false) }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    val checkScope = rememberCoroutineScope()

    // === Shizuku 状态 ===
    var shizukuState by remember { mutableStateOf<ShizukuState>(ShizukuState.NotRunning) }
    val shizukuPermissionCode = 1001

    DisposableEffect(Unit) {
        ShizukuManager.init(context)
        shizukuState = ShizukuManager.state.value

        val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == shizukuPermissionCode) {
                shizukuState = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    ShizukuState.Granted
                } else {
                    ShizukuState.Denied
                }
            }
        }
        ShizukuManager.addPermissionListener(listener)

        onDispose {
            ShizukuManager.removePermissionListener(listener)
        }
    }

    val themeSelectorState = remember(uiState.themeMode) {
        ThemeSelectorState(currentTheme = uiState.themeMode)
    }

    Scaffold(
        topBar = {
            // TopArea 直接嵌入 Scaffold topBar
            com.edgegesture.evilgodxu.screens.settings.compact.top_area.TopArea(
                onNavigateBack = onNavigateBack
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding)
            .padding(innerPadding)

        if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            // 宽屏 → ExpandedAssembly
            ExpandedAssembly(
                modifier = contentModifier,
                themeSelectorState = themeSelectorState,
                currentLanguage = currentLanguage,
                versionName = versionName,
                onThemeClick = { showThemeDialog = true },
                onLanguageClick = { showLanguageDialog = true },
                onDonateClick = { showDonateDialog = true },
                onOpenSourceClick = { showOpenSourceDialog = true },
            )
        } else {
            // 窄屏 → CompactAssembly
            CompactAssembly(
                modifier = contentModifier,
                themeSelectorState = themeSelectorState,
                currentLanguage = currentLanguage,
                versionName = versionName,
                onThemeClick = { showThemeDialog = true },
                onLanguageClick = { showLanguageDialog = true },
                onDonateClick = { showDonateDialog = true },
                onOpenSourceClick = { showOpenSourceDialog = true },
                onVersionClick = {
                    checkScope.launch {
                        val result = UpdateManager.checkForUpdate(context, force = true)
                        if (result != null) {
                            updateInfo = result
                            showUpdateDialog = true
                        } else {
                            showUpToDate = true
                        }
                    }
                },
            )
        }
    }

    // === 对话框 ===
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.themeMode,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { themeMode ->
                viewModel.setThemeMode(themeMode)
                onThemeChange(themeMode)
                showThemeDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { language ->
                showLanguageDialog = false
                context.setAppLanguage(language)
            }
        )
    }

    if (showDonateDialog) {
        DonateDialog(
            onDismiss = { showDonateDialog = false }
        )
    }

    if (showOpenSourceDialog) {
        OpenSourceLicensesDialog(
            onDismiss = { showOpenSourceDialog = false }
        )
    }

    // 更新检测对话框
    if (showUpdateDialog && updateInfo != null) {
        val isDownloading = downloadState is DownloadState.Downloading
        val isFailed = downloadState is DownloadState.Failed
        val progress = (downloadState as? DownloadState.Downloading)?.progress ?: 0f

        AlertDialog(
            onDismissRequest = { if (!isDownloading) { showUpdateDialog = false; downloadState = DownloadState.Idle } },
            title = {
                Text(
                    text = stringResource(R.string.update_dialog_title, updateInfo!!.latestVersion),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
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
                            text = stringResource(R.string.update_dialog_description_alt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (!isDownloading && !isFailed && updateInfo!!.changelog.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.update_dialog_changelog_title),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = updateInfo!!.changelog,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                when {
                    isFailed -> TextButton(onClick = {
                        showUpdateDialog = false
                        downloadState = DownloadState.Idle
                        val url = updateInfo!!.downloadUrl
                        if (url.startsWith("http")) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }) {
                        Text(stringResource(R.string.update_dialog_open_browser))
                    }
                    !isDownloading -> TextButton(onClick = {
                        downloadState = DownloadState.Downloading(0f)
                        checkScope.launch {
                            val success = UpdateManager.downloadAndInstall(
                                context, updateInfo!!,
                                onProgress = { p ->
                                    downloadState = if (p < 0f) DownloadState.Failed("download_failed")
                                    else DownloadState.Downloading(p)
                                }
                            )
                            if (success) {
                                downloadState = DownloadState.Success
                                showUpdateDialog = false
                            } else if (downloadState !is DownloadState.Failed) {
                                downloadState = DownloadState.Failed("download_failed")
                            }
                        }
                    }) {
                        Text(stringResource(R.string.update_dialog_download))
                    }
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { showUpdateDialog = false; downloadState = DownloadState.Idle; UpdateManager.clearPendingUpdate(context) }) {
                        Text(stringResource(R.string.update_dialog_later))
                    }
                }
            }
        )
    }

    // 已是最新版本提示
    if (showUpToDate) {
        android.widget.Toast.makeText(context, context.getString(R.string.update_dialog_up_to_date), android.widget.Toast.LENGTH_SHORT).show()
        showUpToDate = false
    }
}
