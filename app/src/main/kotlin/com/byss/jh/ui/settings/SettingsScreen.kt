package com.byss.jh.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.byss.jh.ui.gesture.service.EdgeGestureAccessibilityService
import com.byss.jh.ui.adaptive.rememberWindowSizeClass
import com.byss.jh.ui.settings.components.AppSwitchBlacklistDialog
import com.byss.jh.ui.settings.components.DonateDialog
import com.byss.jh.ui.settings.components.LanguageSelectionDialog
import com.byss.jh.ui.settings.components.SettingsClickableItem
import com.byss.jh.ui.settings.components.SettingsSection
import com.byss.jh.ui.settings.components.SettingsSwitchItem
import com.byss.jh.ui.settings.components.ThemeSelectionDialog
import com.byss.jh.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

// DataStore 扩展
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// 设置键
object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val LANGUAGE = stringPreferencesKey("language")
}

// 主题模式枚举
enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromValue(value: String): ThemeMode = entries.find { it.value == value } ?: SYSTEM
    }
}

// 保存原始系统 Locale
private var systemLocale: Locale = Locale.getDefault()

// 语言枚举
enum class AppLanguage(val value: String, val locale: Locale) {
    SYSTEM("system", Locale.getDefault()),
    CHINESE("zh", Locale.CHINESE),
    ENGLISH("en", Locale.ENGLISH);

    companion object {
        fun fromValue(value: String): AppLanguage = entries.find { it.value == value } ?: SYSTEM
    }
}

// 设置数据类
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val vibrationEnabled: Boolean = false
)

// 获取设置 Flow
fun Context.settingsFlow(): Flow<SettingsState> = settingsDataStore.data.map { preferences ->
    SettingsState(
        themeMode = ThemeMode.fromValue(preferences[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value),
        language = AppLanguage.fromValue(preferences[SettingsKeys.LANGUAGE] ?: AppLanguage.SYSTEM.value)
    )
}.combine(gestureSettingsFlow()) { settings, gestureSettings ->
    settings.copy(vibrationEnabled = gestureSettings.vibrationEnabled)
}

// 获取主题模式 Flow
fun Context.themeModeFlow(): Flow<ThemeMode> = settingsDataStore.data.map { preferences ->
    ThemeMode.fromValue(preferences[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value)
}

// 保存设置（使用 Dispatchers.IO）
suspend fun Context.saveThemeMode(mode: ThemeMode) = withContext(Dispatchers.IO) {
    settingsDataStore.edit { preferences ->
        preferences[SettingsKeys.THEME_MODE] = mode.value
    }
}

suspend fun Context.saveLanguage(language: AppLanguage) = withContext(Dispatchers.IO) {
    settingsDataStore.edit { preferences ->
        preferences[SettingsKeys.LANGUAGE] = language.value
    }
}

// 更新应用语言
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

    // 从 ViewModel 读取设置
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

    // 对话框状态
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }

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
            // 横屏/平板布局：双列
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
                    // 手势设置
                    SettingsSection(title = stringResource(R.string.settings_section_gesture)) {
                        SettingsSwitchItem(
                            icon = Icons.Default.Vibration,
                            title = stringResource(R.string.settings_vibration_title),
                            subtitle = stringResource(R.string.settings_vibration_desc),
                            checked = uiState.vibrationEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setVibrationEnabled(enabled)
                                EdgeGestureAccessibilityService.updateSettings(context)
                                Logger.i(context, "Settings", "震动反馈: $enabled")
                            }
                        )
                        SettingsClickableItem(
                            icon = Icons.Default.Block,
                            title = stringResource(R.string.settings_blacklist_title),
                            subtitle = stringResource(R.string.settings_blacklist_desc),
                            onClick = { showBlacklistDialog = true }
                        )
                    }

                    // 更多设置
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
            // 竖屏布局：单列
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

                // 手势设置
                SettingsSection(title = stringResource(R.string.settings_section_gesture)) {
                    SettingsSwitchItem(
                        icon = Icons.Default.Vibration,
                        title = stringResource(R.string.settings_vibration_title),
                        subtitle = stringResource(R.string.settings_vibration_desc),
                        checked = uiState.vibrationEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setVibrationEnabled(enabled)
                            EdgeGestureAccessibilityService.updateSettings(context)
                            Logger.i(context, "Settings", "震动反馈: $enabled")
                        }
                    )
                    SettingsClickableItem(
                        icon = Icons.Default.Block,
                        title = stringResource(R.string.settings_blacklist_title),
                        subtitle = stringResource(R.string.settings_blacklist_desc),
                        onClick = { showBlacklistDialog = true }
                    )
                }

                // 更多设置
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
    }

    // 主题选择对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.themeMode,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { themeMode ->
                viewModel.setThemeMode(themeMode)
                Logger.i(context, "Settings", "主题切换为: ${themeMode.name}")
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
                Logger.i(context, "Settings", "语言切换为: ${language.name}")
                updateAppLanguage(context, language)
                onLanguageChange(language)
                showLanguageDialog = false
            }
        )
    }

    // 应用切换黑名单对话框
    if (showBlacklistDialog) {
        AppSwitchBlacklistDialog(
            onDismiss = { showBlacklistDialog = false }
        )
    }

    // 捐赠对话框
    if (showDonateDialog) {
        DonateDialog(
            onDismiss = { showDonateDialog = false }
        )
    }
}
