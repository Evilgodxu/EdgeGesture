package com.edgegesture.evilgodxu.screens.settings

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
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
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.data.shizuku.ShizukuManager
import com.edgegesture.evilgodxu.data.shizuku.ShizukuState
import com.edgegesture.evilgodxu.screens.gesture.service.EdgeGestureAccessibilityService
import com.edgegesture.evilgodxu.ui.adaptive.rememberWindowSizeClass
import com.edgegesture.evilgodxu.screens.settings.components.AppSwitchBlacklistDialog
import com.edgegesture.evilgodxu.screens.settings.components.DonateDialog
import com.edgegesture.evilgodxu.screens.settings.components.LanguageSelectionDialog
import com.edgegesture.evilgodxu.screens.settings.components.LaunchBlockRuleDialog
import com.edgegesture.evilgodxu.screens.settings.components.LaunchBlockRulesList
import com.edgegesture.evilgodxu.screens.settings.components.OpenSourceLicensesDialog
import com.edgegesture.evilgodxu.screens.settings.components.SettingsClickableItem
import com.edgegesture.evilgodxu.screens.settings.components.SettingsSection
import com.edgegesture.evilgodxu.screens.settings.components.SettingsSwitchItem
import com.edgegesture.evilgodxu.screens.settings.components.ThemeSelectionDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import rikka.shizuku.Shizuku

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
    ENGLISH("en");

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
    var showOpenSourceDialog by remember { mutableStateOf(false) }
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
                            subtitle = when (currentLanguage) {
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
                        SettingsClickableItem(
                            icon = Icons.Default.Code,
                            title = stringResource(R.string.settings_open_source),
                            subtitle = stringResource(R.string.settings_open_source_desc),
                            onClick = { showOpenSourceDialog = true }
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
                        subtitle = when (currentLanguage) {
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
                    SettingsClickableItem(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.settings_open_source),
                        subtitle = stringResource(R.string.settings_open_source_desc),
                        onClick = { showOpenSourceDialog = true }
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
            currentLanguage = currentLanguage,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { language ->
                showLanguageDialog = false
                context.setAppLanguage(language)
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

    // 开源许可对话框
    if (showOpenSourceDialog) {
        OpenSourceLicensesDialog(
            onDismiss = { showOpenSourceDialog = false }
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
