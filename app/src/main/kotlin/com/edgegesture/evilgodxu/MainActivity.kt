package com.edgegesture.evilgodxu

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.edgegesture.evilgodxu.data.gesture.gestureSettingsFlow
import com.edgegesture.evilgodxu.data.settings.AppLanguage
import com.edgegesture.evilgodxu.navigation.NavGraph
import com.edgegesture.evilgodxu.ui.adaptive.ProvideWindowSizeClass
import com.edgegesture.evilgodxu.ui.theme.MyApplicationTheme
import com.edgegesture.evilgodxu.update.LocalUpdateViewModel
import com.edgegesture.evilgodxu.update.UpdateDialog
import com.edgegesture.evilgodxu.update.UpdateSource
import com.edgegesture.evilgodxu.update.UpdateViewModel
import com.edgegesture.evilgodxu.utils.localization.LocalizationManager
import com.edgegesture.evilgodxu.utils.localization.ProvideLocalizedContext
import com.edgegesture.evilgodxu.utils.localization.toLocale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Activity 只做入口：挂载导航图与全局副作用，状态由 MainViewModel 与 UpdateViewModel 持有
class MainActivity : ComponentActivity() {

    // 手动 DI：经 Application 容器取依赖，ViewModel 以工厂注入构造参数
    private val appContainer: AppContainer
        get() = (application as App).container

    private val localizationManager: LocalizationManager
        get() = appContainer.localizationManager

    // 首帧状态：主题与语言的持久化值在进入组合前读出，避免默认主题闪现
    private val initialAppUiState: AppUiState by lazy {
        val repository = appContainer.settingsRepository
        runBlocking {
            AppUiState(
                themeMode = repository.settings.first().themeMode,
                language = repository.appLanguage.first(),
                version = appContainer.appVersion,
            )
        }
    }

    private val mainViewModel: MainViewModel by viewModels {
        viewModelFactory {
            initializer {
                MainViewModel(
                    settingsRepository = appContainer.settingsRepository,
                    initialState = initialAppUiState,
                )
            }
        }
    }

    private val updateViewModel: UpdateViewModel by viewModels {
        viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                )
                UpdateViewModel(
                    application = application,
                    settingsRepository = appContainer.settingsRepository,
                    updateCheckRepository = appContainer.updateCheckRepository,
                    updateChecker = appContainer.updateChecker,
                    localizationManager = appContainer.localizationManager,
                )
            }
        }
    }

    // 冷启动按持久化语言创建配置上下文，进入界面即正确语言
    override fun attachBaseContext(newBase: Context) {
        val language = (newBase.applicationContext as? App)
            ?.let { app -> runBlocking { app.container.settingsRepository.appLanguage.first() } }
            ?: AppLanguage.SYSTEM
        val config = Configuration(newBase.resources.configuration).apply {
            setLocales(LocaleList(language.toLocale()))
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CompositionLocalProvider(
                LocalMainViewModel provides mainViewModel,
                LocalUpdateViewModel provides updateViewModel,
            ) {
                ProvideLocalizedContext(localizationManager) {
                    ProvideWindowSizeClass {
                        MainContent()
                    }
                }
            }
        }
    }

    @Composable
    private fun MainContent() {
        MyApplicationTheme {
            // 全局副作用：按窗口方向显隐系统栏
            SystemBarsVisibilityEffect()

            // 回前台自动检查更新（每日仅一次），是否该检查由 ViewModel 判定
            LifecycleResumeEffect(Unit) {
                updateViewModel.onForeground()
                onPauseOrDispose { }
            }

            UpdateToastEffect()

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                NavGraph()
            }

            // 更新对话框挂在应用级：各页面共用同一份更新状态
            val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
            val info = updateUiState.updateInfo
            if (updateUiState.dialogVisible && info != null) {
                UpdateDialog(
                    updateInfo = info,
                    downloadState = updateUiState.downloadState,
                    onDownload = updateViewModel::downloadAndInstall,
                    onOpenBrowser = {
                        val url = UpdateSource.REPOSITORY_URL
                        if (url.startsWith("http")) {
                            startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    url.toUri(),
                                ),
                            )
                        }
                        updateViewModel.dismissUpdateDialog()
                    },
                    onDismiss = updateViewModel::dismissUpdateDialog,
                )
            }
        }
    }

    // 手动检查结果的一次性提示；以文案为键重建收集，避免语言切换后仍提示旧语言文案
    @Composable
    private fun UpdateToastEffect() {
        val context = LocalContext.current
        val upToDateText = stringResource(R.string.update_toast_up_to_date)
        val checkErrorText = stringResource(R.string.update_toast_error)
        LaunchedEffect(upToDateText, checkErrorText) {
            updateViewModel.messages.collect { feedback ->
                val text = when (feedback) {
                    UpdateViewModel.CheckFeedback.UP_TO_DATE -> upToDateText
                    UpdateViewModel.CheckFeedback.ERROR -> checkErrorText
                }
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        }
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
}

// 按窗口方向显隐系统栏的全局副作用；横屏隐藏、竖屏显示
@Composable
private fun SystemBarsVisibilityEffect() {
    val view = LocalView.current
    if (view.isInEditMode) return
    val orientation = LocalConfiguration.current.orientation
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
