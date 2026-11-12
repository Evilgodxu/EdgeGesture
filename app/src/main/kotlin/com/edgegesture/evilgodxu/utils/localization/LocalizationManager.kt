package com.edgegesture.evilgodxu.utils.localization

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edgegesture.evilgodxu.LocalMainViewModel
import com.edgegesture.evilgodxu.data.settings.AppLanguage
import java.util.Locale

// 应用语言转 Locale；systemLocale 为跟随系统时的实际语言
fun AppLanguage.toLocale(systemLocale: Locale = Locale.getDefault()): Locale = when (this) {
    AppLanguage.SYSTEM -> systemLocale
    AppLanguage.CHINESE -> Locale.forLanguageTag("zh")
    AppLanguage.ENGLISH -> Locale.forLanguageTag("en")
}

// 语言管理器：负责语言转 Locale 与创建本地化上下文；语言来源由界面层经 CompositionLocal 提供
class LocalizationManager(
    private val context: Context,
) {
    // 构造时捕获系统语言。此时进程默认语言尚未被应用内切换改写，
    // 确保跟随系统始终解析为真实系统语言
    private val systemLocale: Locale = Locale.getDefault()

    fun localeFor(language: AppLanguage): Locale = language.toLocale(systemLocale)

    // 以指定语言创建本地化上下文
    fun createLocalizedContext(locale: Locale): Context {
        val config = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(config)
    }
}

// 提供本地化 Context 的 Composable，语言切换无需重建 Activity
@Composable
fun ProvideLocalizedContext(
    localizationManager: LocalizationManager,
    content: @Composable () -> Unit,
) {
    val appUiState by LocalMainViewModel.current.uiState.collectAsStateWithLifecycle()
    val localizedContext = localizationManager.createLocalizedContext(
        localizationManager.localeFor(appUiState.language),
    )
    // LocalActivity 与 LocalActivityResultRegistryOwner 的默认值沿 LocalContext 链推导；
    // 本地化 Context 基于 Application 创建，链上取不到宿主 Activity，会使依赖 Activity 的能力失效，
    // 故显式提供进入时的宿主 Activity 与其注册表能力
    val activity = checkNotNull(LocalContext.current.findActivity())
    val resultRegistryOwner = checkNotNull(activity as? ActivityResultRegistryOwner) {
        "宿主 Activity 必须实现 ActivityResultRegistryOwner"
    }
    // LocalResources 默认由 LocalContext 推导；对话框挂在独立窗口上，其组合根会用自己的宿主 Context
    // 重置 LocalContext，弹窗内 stringResource 便退回宿主 Resources（系统语言）而不跟随应用内语言。
    // 显式提供 LocalResources 的优先级高于推导默认值，可保证弹窗文案与应用内语言一致
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalResources provides localizedContext.resources,
        LocalActivity provides activity,
        LocalActivityResultRegistryOwner provides resultRegistryOwner,
    ) {
        content()
    }
}

// 沿 ContextWrapper 链向上查找宿主 Activity
private tailrec fun Context.findActivity(): Activity? = when {
    this is Activity -> this
    this is ContextWrapper -> baseContext.findActivity()
    else -> null
}
