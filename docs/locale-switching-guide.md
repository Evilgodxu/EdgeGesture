# Android 应用内语言切换技术指南

适用于 minSdk 33+、使用 Jetpack Compose 的 Android 应用。

## 方案选型

| 方案 | API 要求 | 适用场景 |
|------|---------|---------|
| `LocaleManager`（本指南） | 33+ | minSdk 已为 33 的应用，最简洁 |
| `AppCompatDelegate.setApplicationLocales()` | 任意（需 appcompat 依赖） | 需兼容 API 32 及以下 |
| 手动 `Configuration` 包装 | 任意 | 不推荐，废弃 API，维护成本高 |

本指南聚焦 `LocaleManager` 方案。

## 核心原理

`LocaleManager` 是 Android 13（API 33）引入的系统级语言管理服务：

- **全局生效**：设置后系统更新应用内所有 Context 的 Configuration（Activity、Service、Application 均包含）
- **自动持久化**：系统保存设置，应用重启后仍生效，支持 Backup & Restore
- **自动触发配置变更**：系统处理 Configuration 变更和 Activity 生命周期事件
- **系统集成**：配合 `locale_config.xml`，系统设置中显示"应用语言"入口

## 接入步骤

### 1. 创建 locale_config.xml

在 `res/xml/` 下声明应用支持的语言：

```xml
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="zh" />
    <locale android:name="en" />
</locale-config>
```

语言标签遵循 [BCP 47](https://www.rfc-editor.org/rfc/rfc5646) 规范，如 `zh`、`zh-Hans-CN`、`en-US`。

### 2. 在 AndroidManifest.xml 中引用

```xml
<application
    android:localeConfig="@xml/locale_config"
    ...>
```

### 3. 封装语言读写

```kotlin
import android.app.LocaleManager
import android.os.LocaleList

enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),       // 跟随系统
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

fun Context.getAppLanguage(): AppLanguage {
    val locales = getSystemService(LocaleManager::class.java).applicationLocales
    return AppLanguage.fromLocaleList(locales)
}

fun Context.setAppLanguage(language: AppLanguage) {
    val localeManager = getSystemService(LocaleManager::class.java)
    localeManager.applicationLocales = if (language.languageTag != null) {
        LocaleList.forLanguageTags(language.languageTag)
    } else {
        LocaleList.getEmptyLocaleList()
    }
}
```

### 4. 在 Compose 中使用

```kotlin
@Composable
fun LanguageSettingsItem() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // 以 configuration 为 key，语言切换后自动刷新
    val currentLanguage = remember(configuration) { context.getAppLanguage() }

    SettingsClickableItem(
        title = stringResource(R.string.settings_language_title),
        subtitle = languageDisplayName(currentLanguage),
        onClick = { /* 打开语言选择对话框 */ }
    )
}
```

语言选择回调中先关闭对话框，再切换语言：

```kotlin
onLanguageSelected = { language ->
    showLanguageDialog = false       // 先关闭对话框
    context.setAppLanguage(language)  // 再切换，避免对话框闪烁新语言
}
```

## 消除切换闪烁

### 问题

`setApplicationLocales()` 默认触发 Activity 重建，整个 Compose 树从零重建，造成可见闪烁。

### 解决方案

在 `AndroidManifest.xml` 中为 Activity 声明 `configChanges`：

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="locale|layoutDirection"
    ...>
```

声明后系统不再重建 Activity，改为调用 `onConfigurationChanged()`。Compose 的 `AndroidComposeView` 自动接收配置变更，更新 `LocalConfiguration` 和 `LocalContext`，`stringResource` 取到新语言文案，重组平滑无闪烁。

### Compose 状态刷新

`configChanges` 模式下 Activity 不重建，`remember` 状态不会自动清除。依赖 locale 的状态需以 `LocalConfiguration` 为 key 主动刷新：

```kotlin
val configuration = LocalConfiguration.current
val currentLanguage = remember(configuration) { context.getAppLanguage() }
```

locale 变更 → `LocalConfiguration` 更新 → `remember` key 变化 → 重新读取。

## 常见陷阱

### 1. Service / 悬浮窗不跟随语言

手动 `Configuration` 包装方案只更新 Activity，Service、悬浮窗（WindowManager overlay）的 Context 不受影响。

`LocaleManager` 在系统级生效，所有 Context 自动跟随，无需额外处理。

### 2. 存储源不一致

手动持久化时，写入与读取必须使用同一存储。DataStore（`files/datastore/*.preferences_pb`）和 SharedPreferences（`shared_prefs/*.xml`）是不同文件，混用会导致重启后设置丢失。

`LocaleManager` 方案下系统自动持久化，不需要应用层存储。

### 3. systemLocale 缓存失效

```kotlin
// 反例：类加载时捕获，系统语言变更后不更新
private var systemLocale: Locale = Locale.getDefault()
```

如需获取系统当前语言，应实时读取，不要缓存。

### 4. configChanges 范围

仅声明需要自行处理的配置项。`locale|layoutDirection` 足矣，不要盲目添加 `orientation|screenSize` 等，除非 Activity 已正确实现对应的 `onConfigurationChanged` 逻辑。

## 旧方案迁移清单

从手动方案迁移到 `LocaleManager` 时，可删除以下代码：

- `attachBaseContext` 中语言相关的 `createConfigurationContext` 包装
- `updateConfiguration()` 调用（已废弃）
- 应用层语言持久化（DataStore / SharedPreferences）
- `systemLocale` 等缓存变量
- 为 Service / 悬浮窗单独包装 Context 的变通代码
- Activity 与设置页之间的语言变更回调传递

迁移后语言切换无需 Activity 参与设置，调用 `setAppLanguage()` 即可，系统处理其余一切。
