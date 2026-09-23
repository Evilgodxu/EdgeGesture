# 边缘手势

<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="96" alt="边缘手势" />

**一款基于 Android 无障碍服务的应用：边缘手势、背面双击、扩展 / 任务面板、悬浮音乐面板等。**

[English](README.md) | **简体中文**

![Release](https://img.shields.io/github/v/release/Evilgodxu/EdgeGesture?style=flat-square&color=4f46e5)
![License](https://img.shields.io/badge/license-AGPL--3.0-blue)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-purple)
![AGP](https://img.shields.io/badge/AGP-9.4.0-blue)
![Gradle](https://img.shields.io/badge/Gradle-9.7.1-blue)
![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2026.09.00-blue)
![minSdk](https://img.shields.io/badge/minSdk-34-orange)
![targetSdk](https://img.shields.io/badge/targetSdk-37-orange)

</div>

**边缘手势（EdgeGesture）** 让你用边缘滑动与点击、或双击手机背部来操作手机，从扩展面板一键启动应用，并在任意应用之上呼出**悬浮音乐面板**、**任务面板**或**罗盘时钟**。

## 特性

### 边缘手势

- **三侧触发**：左侧、右侧和底部边缘各自独立的触发区
- **每侧最多 3 段**：每个分段拥有独立的一套操作
- **每段 9 个操作**：三个方向的滑动与长按滑动，外加单击 / 双击 / 长按
- **自定义触发区**：边缘宽度、高度 / 位置百分比与段数，实时预览
- **二次滑动**：全屏或横屏模式下需连续滑动两次方可触发
- **震动反馈**：触发时震动
- **统计**：手势触发次数与启动拦截次数，支持 1 日、7 日、30 日周期

### 背面双击

通过加速度计传感器检测手机背面双击动作，采用启发式信号处理算法识别敲击。

- **灵敏度**（1–10）与**检测范围**（1–10）可调
- **工作模式**：始终激活、熄屏激活、亮屏激活
- **充电时暂停**检测，减少误触
- 支持与边缘手势完全一致的快捷操作

### 快捷操作

| 分类 | 支持操作 |
| --- | --- |
| 系统导航 | 返回、主页键、任务键、上一个应用 |
| 媒体 | 上一曲、下一曲 |
| 系统 | 手电筒、语音助手、电源菜单、锁屏、截屏 |
| 窗口 | 小窗模式 |
| 面板 | 扩展面板、音乐面板、任务面板、罗盘时钟 |
| 翻译 | 屏幕翻译 |
| 快捷入口 | 支付宝扫一扫、微信扫一扫 |
| 应用 | 启动应用——可绑定任意已安装应用作为目标 |
| 其他 | 延时提醒（1 / 3 / 5 / 10 / 15 分钟）、无 |

### 扩展面板

- **系统控制**：屏幕亮度（经 `WRITE_SETTINGS` 写入，换算沿用 AOSP 同款 HLG 伽马曲线）、闹钟 / 铃声 / 媒体音量滑块
- **应用快捷方式**：8 个槽位，点击即可快速启动
- **小窗启动**：每个槽位可单独开关，以小窗（自由窗口）模式启动
- **图标缓存**：扫描应用列表时缓存图标

### 任务面板

- 展示无障碍服务收集到的最近使用应用（最多 10 个，按应用切换黑名单过滤）
- **单击启动**、**双击小窗打开**、**滑动或清理移除**——移除时通过 Shizuku 结束对应任务

### 屏幕翻译

- 从无障碍树读取屏幕文本及其坐标，在原位覆盖译文；悬浮层不可触摸，下方应用交互不受影响
- 在 **Microsoft Edge**、**Google** 与**免费模型**接口间自动回退，并记住上次成功的服务商
- 开关式交互：再次触发（或切换应用）即关闭

### 音乐面板

- 以悬浮窗形式渲染的全功能播放面板，可在任意应用上层使用
- **迷你播放器**：播放时贴在状态栏下方的紧凑条，点击展开回完整面板
- **本地曲库**：基于 MediaStore 扫描，并支持通过 `VIEW` / `SEND` 意图导入音频；搜索为本地搜索并保留历史
- **歌词**：音频内嵌歌词与本地 `.lrc` 文件，支持带逐字时间戳与 `[tr]` 翻译块的增强 LRC；逐字高亮可关闭
- **封面**：系统缩略图 / 内嵌封面，外加本地图片候选（Coil）
- **元数据写回**：内置标签写入器，可重命名歌名 / 艺术家，并将封面写入 MP3（ID3v2）、MP4 / M4A、FLAC 与 Opus 文件
- **播放链路**面板：当前曲目的音频格式、源 / 输出采样率、位深、声道、输出策略、输出设备与音频路由
- 播放模式（单曲循环 / 列表循环 / 随机）、收藏、定时关闭、播放列表（长按移除曲目）与播放设置

### 启动拦截

基于无障碍服务，可选结合 **Shizuku**。

- 全局开关加逐条规则开关；每条规则包含可选的**启动者**与必填的**被启动者**应用（均填包名，按子串匹配）
- 仅在手势服务本身启用时生效
- **拦截时机**：立即、稍缓（500 ms）、延迟（1000 ms）
- 拦截时切回启动者应用
- **终止启动者**：每次触发都终止（防呆保护：连续最多 5 次，随后冷却 15 秒）
- 可选**终止被启动者**进程，并控制是否允许终止系统应用
- 进程终止依赖 Shizuku，无 Shizuku 时该能力不生效

### 应用切换黑名单

- 过滤切换到上一个应用时无需显示的应用
- 首次启动自动加入系统应用与本应用；权限感知，`QUERY_ALL_PACKAGES` 不可用时提供 `PackageManager` + `<queries>` 兜底
- 监听应用安装 / 卸载并自动更新

### 罗盘时钟

Canvas 绘制的悬浮层：中心为年份，外围七个同心环依次承载月 / 日 / 星期 / 时辰 / 时 / 分 / 秒，当前值位于 3 点钟方向。带入场动画序列，整体旋转 720° 后进入实时走时；再次触发即关闭。

### 设置

- **主题模式**：浅色、深色或跟随系统，切换带圆形揭示过渡
- **语言**：简体中文、English 或跟随系统；应用内热切换，无需重建 Activity，冷启动按持久化语言直接生效
- **手势配置**：导入 / 导出 JSON 文件，覆盖边缘手势、背面双击、触发区选项与启动拦截规则
- **捐赠打赏**、**关于**（作者 / 版本号 / 项目链接）与 Shizuku 状态

### 权限管理

权限卡片列在主页面并显示各自的授权状态，全部授权后整组自动收起。点击卡片会打开系统页面并启动 500 ms 轮询，授权后立即刷新状态并把应用带回前台。按功能划分：

- **无障碍服务**：实现系统级手势操作
- **悬浮窗**：手势触发区与全部悬浮面板
- **通知**：保持手势服务后台运行
- **省电优化**：忽略电池优化以保持存活
- **查询已安装应用**：完整扫描应用列表
- **修改系统设置**：扩展面板的亮度滑块
- **媒体音频 / 图片**：本地曲库与专辑封面
- **全部文件访问**（可选）：写回音频文件元数据

### 应用内更新

应用回到前台时检查更新（每日最多一次）；单列布局下点击版本号还可手动检查。通过 OkHttp 请求 GitHub Releases API，用 `DownloadManager` 将 APK 下载到应用私有目录，经 `FileProvider` 拉起安装；仅当 APK 的 SHA-256 与 GitHub 公布的摘要一致时才安装，否则丢弃文件。

### 崩溃日志

未捕获异常与已捕获异常按天写入应用专属外部目录下的日志文件，仅保留最近 3 天。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 | Kotlin 2.4.20 |
| UI | Jetpack Compose（BOM 2026.09.00）+ Material 3 |
| 自适应布局 | androidx.window 1.5.1（`WindowSizeClass`） |
| 依赖注入 | 原生 AndroidX ViewModel 工厂 + 应用级单例 |
| 导航 | Navigation 3 —— `navigation3-runtime` / `navigation3-ui` 1.1.7（类型安全 `NavKey`） |
| 状态 | DataStore Preferences 1.2.1 + StateFlow / MutableStateFlow |
| 权限 | Shizuku 13.1.5 + 自定义 `UserService`（AIDL） |
| 音频 | Media3 ExoPlayer 1.11.1 + MediaSessionService |
| 图片加载 | Coil 3.6.2（本地封面候选） |
| 网络 | OkHttp 5.5.0 |
| 序列化 | kotlinx.serialization 1.11.0 |
| 协程 | kotlinx.coroutines 1.11.0（含 coroutines-guava，用于 MediaController） |
| Hidden API 绕过 | hidden-api-bypass 6.1（小窗窗口模式） |
| 构建 | AGP 9.4.0、Gradle 9.7.1、refreshVersions、JDK 21 |

## 项目结构

```
.
├── app/
│   └── src/main/
│       ├── aidl/com/edgegesture/evilgodxu/service/   # ICommandService（Shizuku UserService）
│       ├── kotlin/com/edgegesture/evilgodxu/
│       │   ├── data/
│       │   │   ├── app/             # AppRepository（应用列表 + 图标缓存）、DataConfigManager
│       │   │   ├── gesture/         # 手势 / 扩展面板设置、GestureStatsManager
│       │   │   ├── launchblock/     # 启动拦截规则
│       │   │   ├── permission/      # PermissionMonitor
│       │   │   ├── shizuku/         # ShizukuManager
│       │   │   └── translate/       # TranslationService
│       │   ├── log/                 # CrashLogManager
│       │   ├── navigation/          # NavGraph（Navigation 3 类型安全键）
│       │   ├── screens/
│       │   │   ├── gesture/         #   主页 + 边缘配置
│       │   │   │   ├── components/  #   页面共用组件
│       │   │   │   └── service/     #   无障碍服务 + 悬浮 UI
│       │   │   │       ├── compassclock/
│       │   │   │       ├── expandpanel/
│       │   │   │       ├── musicpanel/
│       │   │   │       ├── taskpanel/
│       │   │   │       └── translate/
│       │   │   ├── backtap/         #   背面双击
│       │   │   ├── blacklist/       #   应用切换黑名单
│       │   │   ├── expandpanel/     #   扩展面板设置
│       │   │   ├── launchblock/     #   启动拦截
│       │   │   └── settings/components/
│       │   ├── service/             # CommandUserService
│       │   ├── ui/                  # adaptive/（窗口尺寸类）+ theme/
│       │   ├── update/              # UpdateManager / UpdateViewModel / UpdateDialog
│       │   ├── utils/localization/  # 应用内多语言管理
│       │   ├── MainActivity.kt
│       │   └── MyApplication.kt
│       └── res/                     # 资源（values / values-en）
├── gradle/
│   ├── libs.versions.toml           # 版本目录
│   └── wrapper/
├── LICENSE
├── NOTICE
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 架构

应用遵循 **MVVM + 单向数据流**：状态由 `ViewModel` → `UiState` → UI 自上而下流动，事件由 UI 自下而上传递；共享数据逻辑位于 `data/` 层，以 Repository 形式暴露（设置、应用列表、启动拦截规则、翻译、权限、Shizuku），由 AndroidX ViewModel 工厂与应用级单例统一组装。

手势与悬浮能力都位于 `screens/gesture/service/`：
`EdgeGestureAccessibilityService` 负责检测边缘滑动、边缘点击与背部敲击，并通过 `AccessibilityActionExecutor` 派发操作。各悬浮 UI（边缘触发区、扩展面板、音乐面板、迷你播放器、任务面板、翻译、罗盘时钟）由各自的窗口管理器以系统窗口形式承载，多数以 `ComposeView` 渲染，并由无障碍服务统一协调。

设置通过 DataStore 持久化（`gesture_settings`、`settings`、`launch_block`）。可选的系统级操作——移除任务 / 结束应用进程、小窗窗口——经由 Shizuku 的 `CommandUserService` 或 hidden API 绕过完成；Shizuku 不可用时相关能力不生效。

## 快速开始

### 环境要求

- JDK 21
- Android Studio（建议最新稳定版）
- 包含 API 37（`compileSdk`）的 Android SDK

### 构建

```bash
git clone https://github.com/Evilgodxu/EdgeGesture.git
cd EdgeGesture

# 调试包
./gradlew assembleDebug

# 发布包（需先配置签名，见下文）
./gradlew assembleRelease
```

APK 输出为 `app/build/outputs/apk/` 下的 `EdgeGesture-<版本号>-arm64.apk`，仅构建 `arm64-v8a` ABI。Release 构建开启代码压缩与资源压缩。

### 发布签名

Release 构建从项目根目录的 `local.properties` 读取签名凭据：

```properties
KEYSTORE_PASSWORD=你的签名库密码
KEY_ALIAS=your_key_alias
KEY_PASSWORD=你的别名密码
```

签名库文件默认位于项目根目录 `jh.keystore`（如需调整请修改 `app/build.gradle.kts` 中的 `storeFile`）。两个文件均已被 git 忽略，请勿提交。

## 免责声明

屏幕翻译依赖第三方公共网络接口（Microsoft Edge / Google / 免费模型），其可用性与策略可能随地区与内容而异。应用与文档仅供个人学习交流使用，请支持正版版权方。

## License

[AGPL-3.0](LICENSE) © 2026 Evilgodxu
