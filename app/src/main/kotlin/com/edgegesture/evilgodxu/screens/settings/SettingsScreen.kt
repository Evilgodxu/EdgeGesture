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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.log.CrashLogManager
import com.edgegesture.evilgodxu.update.UpdateDialog
import com.edgegesture.evilgodxu.update.UpdateManager
import com.edgegesture.evilgodxu.update.UpdateViewModel
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import com.edgegesture.evilgodxu.ui.adaptive.currentWindowSizeClass
import com.edgegesture.evilgodxu.screens.settings.components.DonateDialog
import com.edgegesture.evilgodxu.screens.settings.components.GestureConfigDialog
import com.edgegesture.evilgodxu.screens.settings.components.LanguageSelectionDialog
import com.edgegesture.evilgodxu.screens.settings.components.OpenSourceLicensesDialog
import com.edgegesture.evilgodxu.screens.settings.components.SettingsClickableItem
import com.edgegesture.evilgodxu.screens.settings.components.SettingsSection
import com.edgegesture.evilgodxu.screens.settings.components.ThemeSelectionDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    CHINESE("zh");

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
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

// 获取设置状态流，合并主题和震动设置
fun Context.settingsFlow(): Flow<SettingsState> = settingsDataStore.data.map { preferences ->
    SettingsState(
        themeMode = ThemeMode.fromValue(preferences[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value)
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDataConfig: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val currentLanguage = remember(configuration) { context.getAppLanguage() }
    // 在组合阶段解析字符串资源，LaunchedEffect 内无法调用 stringResource
    val upToDateMessage = stringResource(R.string.update_dialog_up_to_date)
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            CrashLogManager.logException("SettingsScreen", "获取版本号失败", e)
            ""
        }
    }

    // 通过 ViewModel 获取设置状态，自动响应设置变更
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = currentWindowSizeClass()

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
    var showGestureConfigDialog by remember { mutableStateOf(false) }

    // 更新检测状态由 UpdateViewModel 统一管理，与主界面共享
    val updateViewModel: UpdateViewModel = koinViewModel()
    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()
    val showUpdateDialog by updateViewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val showUpToDate by updateViewModel.showUpToDate.collectAsStateWithLifecycle()
    val showUpdateError by updateViewModel.showUpdateError.collectAsStateWithLifecycle()
    val downloadState by updateViewModel.downloadState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.initShizuku()

        val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == viewModel.shizukuPermissionCode) {
                viewModel.setShizukuPermissionResult(grantResult == PackageManager.PERMISSION_GRANTED)
            }
        }
        ShizukuManager.addPermissionListener(listener)

        onDispose {
            ShizukuManager.removePermissionListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
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
                            icon = Icons.Default.Folder,
                            title = stringResource(R.string.data_config_title),
                            subtitle = stringResource(R.string.data_config_desc),
                            onClick = onNavigateToDataConfig
                        )
                        HorizontalDivider()
                        SettingsClickableItem(
                            icon = Icons.Default.Folder,
                            title = stringResource(R.string.gesture_config_title),
                            subtitle = stringResource(R.string.gesture_config_desc),
                            onClick = { showGestureConfigDialog = true }
                        )
                        HorizontalDivider()
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
                        },
                        onClick = { showLanguageDialog = true }
                    )
                }

                // 更多设置项
                SettingsSection(title = stringResource(R.string.settings_about)) {
                    SettingsClickableItem(
                        icon = Icons.Default.Folder,
                        title = stringResource(R.string.data_config_title),
                        subtitle = stringResource(R.string.data_config_desc),
                        onClick = onNavigateToDataConfig
                    )
                    HorizontalDivider()
                    SettingsClickableItem(
                        icon = Icons.Default.Folder,
                        title = stringResource(R.string.gesture_config_title),
                        subtitle = stringResource(R.string.gesture_config_desc),
                        onClick = { showGestureConfigDialog = true }
                    )
                    HorizontalDivider()
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
                            updateViewModel.checkForUpdate(force = true)
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

    if (showGestureConfigDialog) {
        GestureConfigDialog(
            onDismiss = { showGestureConfigDialog = false }
        )
    }

    // 更新检测对话框（与主界面共用 UpdateViewModel 与 UpdateDialog）
    if (showUpdateDialog && updateInfo != null) {
        // 委托属性无法智能转换，先解包为局部变量再判空
        val info = updateInfo
        if (info != null) {
            UpdateDialog(
                updateInfo = info,
                downloadState = downloadState,
                onDownload = { updateViewModel.downloadAndInstall() },
                onOpenBrowser = {
                    updateViewModel.dismissUpdateDialog()
                    val url = UpdateManager.GITHUB_REPOSITORY_URL
                    if (url.startsWith("http")) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                onDismiss = { updateViewModel.dismissUpdateDialog() }
            )
        }
    }

    // 手动检查更新失败提示
    if (showUpdateError) {
        AlertDialog(
            onDismissRequest = { updateViewModel.clearUpdateError() },
            title = { Text(stringResource(R.string.update_dialog_error_title)) },
            text = { Text(stringResource(R.string.update_dialog_error_description)) },
            confirmButton = {
                TextButton(onClick = {
                    updateViewModel.clearUpdateError()
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.GITHUB_REPOSITORY_URL)))
                }) {
                    Text(stringResource(R.string.update_dialog_open_github))
                }
            },
            dismissButton = {
                TextButton(onClick = { updateViewModel.clearUpdateError() }) {
                    Text(stringResource(R.string.update_dialog_later))
                }
            }
        )
    }

    // 已是最新版本提示：副作用移入 LaunchedEffect，避免在组合阶段弹 Toast
    LaunchedEffect(showUpToDate) {
        if (showUpToDate) {
            android.widget.Toast.makeText(
                context,
                upToDateMessage,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            updateViewModel.clearUpToDate()
        }
    }
}


