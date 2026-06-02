package com.byss.jh.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.byss.jh.R
import com.byss.jh.data.gesture.gestureSettingsFlow
import com.byss.jh.data.launchblock.LaunchBlockRule
import com.byss.jh.data.shizuku.ShizukuManager
import com.byss.jh.data.shizuku.ShizukuState
import com.byss.jh.screens.gesture.service.EdgeGestureAccessibilityService
import com.byss.jh.ui.adaptive.rememberWindowSizeClass
import com.byss.jh.screens.settings.components.AppSwitchBlacklistDialog
import com.byss.jh.screens.settings.components.DonateDialog
import com.byss.jh.screens.settings.components.LanguageSelectionDialog
import com.byss.jh.screens.settings.components.LaunchBlockRuleDialog
import com.byss.jh.screens.settings.components.LaunchBlockRulesList
import com.byss.jh.screens.settings.components.SettingsClickableItem
import com.byss.jh.screens.settings.components.SettingsSection
import com.byss.jh.screens.settings.components.SettingsSwitchItem
import com.byss.jh.screens.settings.components.ThemeSelectionDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import rikka.shizuku.Shizuku
import java.util.Locale

// 应用设置 DataStore 实例
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// 设置存储键名定义
object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val LANGUAGE = stringPreferencesKey("language")
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

// 缓存系统原始 Locale，用于恢复系统默认语言
private var systemLocale: Locale = Locale.getDefault()

// 应用语言设置
enum class AppLanguage(val value: String, val locale: Locale) {
    SYSTEM("system", Locale.getDefault()),
    CHINESE("zh", Locale.CHINESE),
    ENGLISH("en", Locale.ENGLISH);

    companion object {
        fun fromValue(value: String): AppLanguage = entries.find { it.value == value } ?: SYSTEM
    }
}

// 设置状态数据类
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val vibrationEnabled: Boolean = false
)

// 获取设置状态流，合并主题、语言和震动设置
fun Context.settingsFlow(): Flow<SettingsState> = settingsDataStore.data.map { preferences ->
    SettingsState(
        themeMode = ThemeMode.fromValue(preferences[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value),
        language = AppLanguage.fromValue(preferences[SettingsKeys.LANGUAGE] ?: AppLanguage.SYSTEM.value)
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

// 保存语言设置
suspend fun Context.saveLanguage(language: AppLanguage) = withContext(Dispatchers.IO) {
    settingsDataStore.edit { preferences ->
        preferences[SettingsKeys.LANGUAGE] = language.value
    }
}

// 更新应用语言配置
// 使用 @Suppress("DEPRECATION") 因为 updateConfiguration 虽被标记废弃，
// 但在 attachBaseContext 场景下仍是唯一可行的方案
@Suppress("DEPRECATION")
fun updateAppLanguage(context: Context, language: AppLanguage) {
    val locale = when (language) {
        AppLanguage.SYSTEM -> systemLocale
        else -> language.locale
    }
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
    
    // 更新 Activity 配置
    (context as? Activity)?.let { activity ->
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit = {},
    onLanguageChange: (AppLanguage) -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current

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
    var showBlacklistDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    var showLaunchBlockRuleDialog by remember { mutableStateOf(false) }
    var editingLaunchBlockRule by remember { mutableStateOf<LaunchBlockRule?>(null) }

    // Shizuku 状态
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
            ShizukuManager.removePermissionListener()
        }
    }

    val topBarInsets = if (!windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        WindowInsets.statusBars
    } else {
        WindowInsets(0, 0, 0, 0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_content_description),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = topBarInsets,
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
                    verticalArrangement = Arrangement.spacedBy(24.dp),
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
                            subtitle = when (uiState.language) {
                                AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                                AppLanguage.CHINESE -> stringResource(R.string.settings_language_chinese)
                                AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
                            },
                            onClick = { showLanguageDialog = true }
                        )
                    }
                }

                // 手势与更多设置列
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // 手势反馈设置
                    SettingsSection(title = stringResource(R.string.settings_section_gesture)) {
                        SettingsSwitchItem(
                            icon = Icons.Default.Vibration,
                            title = stringResource(R.string.settings_vibration_title),
                            subtitle = stringResource(R.string.settings_vibration_desc),
                            checked = uiState.vibrationEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setVibrationEnabled(enabled)
                                EdgeGestureAccessibilityService.updateSettings(context)
                            }
                        )
                        SettingsClickableItem(
                            icon = Icons.Default.Block,
                            title = stringResource(R.string.settings_blacklist_title),
                            subtitle = stringResource(R.string.settings_blacklist_desc),
                            onClick = { showBlacklistDialog = true }
                        )
                    }

                    // 更多设置项
                    SettingsSection(title = stringResource(R.string.settings_more)) {
                        SettingsClickableItem(
                            icon = Icons.Default.Favorite,
                            title = stringResource(R.string.settings_donate),
                            subtitle = stringResource(R.string.settings_donate_desc),
                            onClick = { showDonateDialog = true }
                        )
                    }
                }
            }
        } else {
            // 窄屏设备使用单列布局
            Column(
                modifier = contentModifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
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
                        subtitle = when (uiState.language) {
                            AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                            AppLanguage.CHINESE -> stringResource(R.string.settings_language_chinese)
                            AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
                        },
                        onClick = { showLanguageDialog = true }
                    )
                }

                // 手势反馈设置
                SettingsSection(title = stringResource(R.string.settings_section_gesture)) {
                    SettingsSwitchItem(
                        icon = Icons.Default.Vibration,
                        title = stringResource(R.string.settings_vibration_title),
                        subtitle = stringResource(R.string.settings_vibration_desc),
                        checked = uiState.vibrationEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setVibrationEnabled(enabled)
                            EdgeGestureAccessibilityService.updateSettings(context)
                        }
                    )
                    SettingsClickableItem(
                        icon = Icons.Default.Block,
                        title = stringResource(R.string.settings_blacklist_title),
                        subtitle = stringResource(R.string.settings_blacklist_desc),
                        onClick = { showBlacklistDialog = true }
                    )
                }

                // 更多设置项
                SettingsSection(title = stringResource(R.string.settings_more)) {
                    SettingsSwitchItem(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.settings_launch_block_title),
                        subtitle = stringResource(R.string.settings_launch_block_desc),
                        checked = uiState.launchBlockEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setLaunchBlockEnabled(enabled)
                        }
                    )
                    if (uiState.launchBlockEnabled) {
                        LaunchBlockRulesList(
                            rules = uiState.launchBlockRules,
                            onAddRule = {
                                editingLaunchBlockRule = null
                                showLaunchBlockRuleDialog = true
                            },
                            onEditRule = { rule ->
                                editingLaunchBlockRule = rule
                                showLaunchBlockRuleDialog = true
                            },
                            onDeleteRule = { ruleId ->
                                viewModel.removeLaunchBlockRule(ruleId)
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    SettingsClickableItem(
                        icon = Icons.Default.Favorite,
                        title = stringResource(R.string.settings_donate),
                        subtitle = stringResource(R.string.settings_donate_desc),
                        onClick = { showDonateDialog = true }
                    )
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
            currentLanguage = uiState.language,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { language ->
                viewModel.setLanguage(language)
                updateAppLanguage(context, language)
                onLanguageChange(language)
                showLanguageDialog = false
            }
        )
    }

    // 应用切换黑名单管理对话框
    if (showBlacklistDialog) {
        AppSwitchBlacklistDialog(
            onDismiss = { showBlacklistDialog = false }
        )
    }

    // 捐赠支持对话框
    if (showDonateDialog) {
        DonateDialog(
            onDismiss = { showDonateDialog = false }
        )
    }

    // 启动拦截规则对话框
    if (showLaunchBlockRuleDialog) {
        LaunchBlockRuleDialog(
            rule = editingLaunchBlockRule,
            onDismiss = {
                showLaunchBlockRuleDialog = false
                editingLaunchBlockRule = null
            },
            onConfirm = { rule ->
                if (editingLaunchBlockRule != null) {
                    viewModel.updateLaunchBlockRule(rule)
                } else {
                    viewModel.addLaunchBlockRule(rule)
                }
                showLaunchBlockRuleDialog = false
                editingLaunchBlockRule = null
            },
            onDelete = { ruleId ->
                viewModel.removeLaunchBlockRule(ruleId)
                showLaunchBlockRuleDialog = false
                editingLaunchBlockRule = null
            }
        )
    }
}
