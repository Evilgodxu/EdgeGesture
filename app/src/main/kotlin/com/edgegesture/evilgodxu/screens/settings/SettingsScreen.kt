package com.edgegesture.evilgodxu.screens.settings

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.LocaleList
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.edgegesture.evilgodxu.DownloadState
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.UpdateInfo
import com.edgegesture.evilgodxu.UpdateManager
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import com.edgegesture.evilgodxu.data.shizuku.ShizukuState
import com.edgegesture.evilgodxu.ui.adaptive.rememberWindowSizeClass
import com.edgegesture.evilgodxu.screens.settings.components.DonateDialog
import com.edgegesture.evilgodxu.screens.settings.components.LanguageSelectionDialog
import com.edgegesture.evilgodxu.screens.settings.components.OpenSourceLicensesDialog
import com.edgegesture.evilgodxu.screens.settings.components.SettingsClickableItem
import com.edgegesture.evilgodxu.screens.settings.components.SettingsSection
import com.edgegesture.evilgodxu.screens.settings.components.SettingsSwitchItem
import com.edgegesture.evilgodxu.screens.settings.components.ThemeSelectionDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import rikka.shizuku.Shizuku

private const val GITHUB_URL = "https://github.com/Evilgodxu/EdgeGesture"

// 应用设置 DataStore 实例
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// 设置存储键名定义
object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
}

// 应用主题模式
enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromValue(value: String): ThemeMode = entries.find { it.value == value } ?: SYSTEM
    }
}

// 应用语言设置，通过 LocaleManager 管理应用内语言偏好
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    CHINESE("zh"),
    ENGLISH("en"),
    JAPANESE("ja"),
    KOREAN("ko"),
    RUSSIAN("ru"),
    GERMAN("de");

    companion object {
        fun fromLocaleList(localeList: LocaleList): AppLanguage {
            if (localeList.isEmpty) return SYSTEM
            val tag = localeList[0].toLanguageTag()
            return entries.find { it.languageTag == tag } ?: SYSTEM
        }
    }
}

// 读取当前应用语言
fun Context.getAppLanguage(): AppLanguage {
    val locales = getSystemService(LocaleManager::class.java).applicationLocales
    return AppLanguage.fromLocaleList(locales)
}

// 设置应用语言，系统自动持久化并触发配置变更
fun Context.setAppLanguage(language: AppLanguage) {
    val localeManager = getSystemService(LocaleManager::class.java)
    localeManager.applicationLocales = if (language.languageTag != null) {
        LocaleList.forLanguageTags(language.languageTag)
    } else {
        LocaleList.getEmptyLocaleList()
    }
}

// 设置状态数据类
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val vibrationEnabled: Boolean = false
)

// 获取设置状态流，合并主题和震动设置
fun Context.settingsFlow(): Flow<SettingsState> = settingsDataStore.data.map { preferences ->
    SettingsState(
        themeMode = ThemeMode.fromValue(preferences[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value)
    )
}.combine(gestureSettingsFlow()) { settings, gestureSettings ->
    settings.copy(vibrationEnabled = gestureSettings.vibrationEnabled)
}

// 获取主题模式流
fun Context.themeModeFlow(): Flow<ThemeMode> = settingsDataStore.data.map { preferences ->
    ThemeMode.fromValue(preferences[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value)
}

// 保存主题模式设置
suspend fun Context.saveThemeMode(mode: ThemeMode) = withContext(Dispatchers.IO) {
    settingsDataStore.edit { preferences ->
        preferences[SettingsKeys.THEME_MODE] = mode.value
    }
}

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

    // 通过 ViewModel 获取设置状态，自动响应设置变更
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

    // 各对话框显示状态管理
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    var showOpenSourceDialog by remember { mutableStateOf(false) }

    // Shizuku 状态
    var shizukuState by remember { mutableStateOf<ShizukuState>(ShizukuState.NotRunning) }
    val shizukuPermissionCode = 1001

    // 更新检测状态
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showUpToDate by remember { mutableStateOf(false) }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    val checkScope = androidx.compose.runtime.rememberCoroutineScope()

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

    Scaffold(
        topBar = {
            SettingsBackHeader(onNavigateBack = onNavigateBack)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding)
            .padding(innerPadding)
            .padding(16.dp)

        if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            // 宽屏设备使用双列布局，提高空间利用率
            Row(
                modifier = contentModifier.verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // 外观与语言设置列
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    // 主题设置
                    SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                        SettingsClickableItem(
                            icon = Icons.Default.Palette,
                            title = stringResource(R.string.settings_theme_title),
                            subtitle = when (uiState.themeMode) {
                                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                                ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                            },
                            onClick = { showThemeDialog = true }
                        )
                    }

                    // 语言设置
                    SettingsSection(title = stringResource(R.string.settings_section_language)) {
                        SettingsClickableItem(
                            icon = Icons.Default.Language,
                            title = stringResource(R.string.settings_language_title),
                            subtitle = when (currentLanguage) {
                                AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                                AppLanguage.CHINESE -> stringResource(R.string.settings_language_chinese)
                                AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
                                AppLanguage.JAPANESE -> stringResource(R.string.settings_language_japanese)
                                AppLanguage.KOREAN -> stringResource(R.string.settings_language_korean)
                                AppLanguage.RUSSIAN -> stringResource(R.string.settings_language_russian)
                                AppLanguage.GERMAN -> stringResource(R.string.settings_language_german)
                            },
                            onClick = { showLanguageDialog = true }
                        )
                    }
                }

                // 手势与更多设置列
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    // 更多设置项
                    SettingsSection(title = stringResource(R.string.settings_about)) {
                        SettingsClickableItem(
                            icon = Icons.Default.Favorite,
                            title = stringResource(R.string.settings_donate),
                            subtitle = stringResource(R.string.settings_donate_desc),
                            onClick = { showDonateDialog = true }
                        )
                        HorizontalDivider()
                        SettingsClickableItem(
                            icon = Icons.Default.Code,
                            title = stringResource(R.string.settings_open_source),
                            subtitle = stringResource(R.string.settings_open_source_desc),
                            onClick = { showOpenSourceDialog = true }
                        )
                    }
                }
            }

            // 版本信息（宽屏）
            Text(
                text = "Evilgodxu",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 2.dp)
            )
            Text(
                text = stringResource(R.string.settings_version, versionName),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            // 项目链接
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = GITHUB_URL,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        } else {
            // 窄屏设备使用单列布局
            Column(
                modifier = contentModifier.verticalScroll(rememberScrollState()),
            ) {
                // 主题设置
                SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                    SettingsClickableItem(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.settings_theme_title),
                        subtitle = when (uiState.themeMode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        },
                        onClick = { showThemeDialog = true }
                    )
                }

                // 语言设置
                SettingsSection(title = stringResource(R.string.settings_section_language)) {
                    SettingsClickableItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.settings_language_title),
                        subtitle = when (currentLanguage) {
                            AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                            AppLanguage.CHINESE -> stringResource(R.string.settings_language_chinese)
                            AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
                            AppLanguage.JAPANESE -> stringResource(R.string.settings_language_japanese)
                            AppLanguage.KOREAN -> stringResource(R.string.settings_language_korean)
                            AppLanguage.RUSSIAN -> stringResource(R.string.settings_language_russian)
                            AppLanguage.GERMAN -> stringResource(R.string.settings_language_german)
                        },
                        onClick = { showLanguageDialog = true }
                    )
                }

                // 更多设置项
                SettingsSection(title = stringResource(R.string.settings_about)) {
                    SettingsClickableItem(
                        icon = Icons.Default.Favorite,
                        title = stringResource(R.string.settings_donate),
                        subtitle = stringResource(R.string.settings_donate_desc),
                        onClick = { showDonateDialog = true }
                    )
                    HorizontalDivider()
                    SettingsClickableItem(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.settings_open_source),
                        subtitle = stringResource(R.string.settings_open_source_desc),
                        onClick = { showOpenSourceDialog = true }
                    )
                }

                // 版本信息（点击检查更新）
                Text(
                    text = "Evilgodxu",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 2.dp)
                )
                Text(
                    text = stringResource(R.string.settings_version, versionName),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            checkScope.launch {
                                val result = UpdateManager.checkForUpdate(context, force = true)
                                if (result != null) {
                                    updateInfo = result
                                    showUpdateDialog = true
                                } else {
                                    showUpToDate = true
                                }
                            }
                        }
                        .padding(bottom = 6.dp)
                )

                // 项目链接
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = GITHUB_URL,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }

    // 主题选择对话框
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

    // 语言选择对话框
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

    // 捐赠支持对话框
    if (showDonateDialog) {
        DonateDialog(
            onDismiss = { showDonateDialog = false }
        )
    }

    // 开源许可对话框
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
                    // 非下载/失败状态才显示更新日志，让下载进度更聚焦
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

@Composable
private fun SettingsBackHeader(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onNavigateBack)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back_content_description),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.settings_back_content_description),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
