package com.byss.jh

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.byss.jh.data.gesture.gestureSettingsFlow
import com.byss.jh.navigation.NavGraph
import com.byss.jh.ui.adaptive.ProvideWindowSizeClass
import com.byss.jh.ui.settings.AppLanguage
import com.byss.jh.ui.settings.settingsFlow
import com.byss.jh.ui.theme.MyApplicationTheme
import com.byss.jh.util.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    override fun attachBaseContext(newBase: Context) {
        // 必须在 super.attachBaseContext 之前应用语言，确保整个 Activity 生命周期使用正确语言
        val updatedContext = applyLanguageSettings(newBase)
        super.attachBaseContext(updatedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.i(this, "AppLifecycle", "应用启动")
        enableEdgeToEdge()

        // 设置系统栏控制
        setupSystemBars()

        setContent {
            // 通过 StateFlow 监听设置变化，当设置改变时触发 Compose 重组
            val settings by settingsFlow().collectAsState(initial = null)

            // 语言设置变更时立即应用到当前 Activity，无需重启
            settings?.let {
                if (it.language != AppLanguage.SYSTEM) {
                    applyLanguageToActivity(it.language)
                }
            }

            ProvideWindowSizeClass {
                MyApplicationTheme {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        onThemeChange = { themeMode ->
                            // 主题切换通过 Compose 重组实现，无需重建
                        },
                        onLanguageChange = { language ->
                            // 语言切换通过更新资源实现，无需重建
                            applyLanguageToActivity(language)
                        }
                    )
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

    companion object {
        // 缓存系统原始 Locale，用于恢复系统默认语言设置
        private var systemLocale: Locale = Locale.getDefault()
    }

    private fun applyLanguageSettings(context: Context): Context {
        // 使用 SharedPreferences 同步读取语言设置（DataStore 在 attachBaseContext 时不可用）
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val languageValue = prefs.getString("language", AppLanguage.SYSTEM.value)
            ?: AppLanguage.SYSTEM.value
        val language = AppLanguage.fromValue(languageValue)

        return updateContextLocale(context, language)
    }

    private fun updateContextLocale(context: Context, language: AppLanguage): Context {
        val locale = when (language) {
            AppLanguage.SYSTEM -> systemLocale
            else -> language.locale
        }

        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    @Suppress("DEPRECATION")
    private fun applyLanguageToActivity(language: AppLanguage) {
        val locale = when (language) {
            AppLanguage.SYSTEM -> systemLocale
            else -> language.locale
        }

        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
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
            Logger.i(this@MainActivity, "AppLifecycle", "隐藏后台: ${settings.hideFromRecents}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.i(this, "AppLifecycle", "应用退出")
    }
}
