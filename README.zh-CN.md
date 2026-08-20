# 边缘手势

<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="96" alt="边缘手势" />

**一款基于 Android 无障碍服务的边缘手势应用，支持背面双击、全局音乐面板等丰富功能。**

[English](README.md) | **简体中文**

![Release](https://img.shields.io/github/v/release/Evilgodxu/EdgeGesture?style=flat-square&color=4f46e5)
![License](https://img.shields.io/badge/license-AGPL--3.0-blue)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-purple)
![AGP](https://img.shields.io/badge/AGP-9.3.1-blue)
![Gradle](https://img.shields.io/badge/Gradle-9.7.0-blue)
![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2026.08.00-blue)
![minSdk](https://img.shields.io/badge/minSdk-34-orange)
![targetSdk](https://img.shields.io/badge/targetSdk-37-orange)

</div>

**边缘手势（EdgeGesture）** 让你用更自然的方式操作手机：通过边缘滑动或双击手机背部触发操作，从扩展面板一键启动常用应用，还能在任意应用之上呼出**悬浮音乐面板**或**任务面板**——常用功能始终触手可及。

## 特性

### 边缘手势

- **三侧触发**：支持左侧、右侧和底部边缘触发区
- **多种方向**：每侧支持 6 种滑动手势（短滑 / 长滑 × 3 个方向）
- **灵活分区**：每侧最多 3 段分区，可分别配置操作
- **自定义触发区**：调整边缘宽度、高度 / 位置百分比与段数，实时预览
- **二次滑动**：全屏或横屏模式下需连续滑动两次方可触发
- **反馈**：滑动操作触发震动
- **统计**：记录手势触发次数与启动拦截次数，支持 1 日、7 日、30 日周期

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
| 其他 | 延时提醒（1 / 3 / 5 / 10 / 15 分钟）、无 |

### 扩展面板

- **系统控制**：亮度、闹钟 / 铃声 / 媒体音量调节滑块
- **应用快捷方式**：最多 8 个常用应用，点击即可快速启动
- **小窗启动**：每个应用可单独配置是否以小窗（自由窗口）模式启动
- **图标缓存**：扫描应用时自动缓存图标，提升加载性能

### 任务面板

- 以悬浮面板展示最近使用的应用，**单击启动**、**双击小窗打开**

### 屏幕翻译

- 通过无障碍服务读取屏幕文字并就地翻译
- 在 **Microsoft Edge**、**Google** 与**免费模型**接口间自动回退，并记住上次可用的服务商

### 音乐面板

- 以悬浮窗（**SYSTEM_ALERT_WINDOW**）形式渲染的全功能播放面板，可在任意应用上层使用
- **迷你播放器**：后台播放时显示的紧凑悬浮条，点击即可展开回完整面板
- **本地曲库**：基于 MediaStore 扫描设备存储，并支持通过 `VIEW` / `SEND` 意图导入音频
- **多平台在线搜索**：聚合网易云、QQ 音乐、酷狗与 Jamendo，支持搜索历史与在线歌曲直接播放
- **同步歌词**：滚动歌词，支持在线匹配 / 刷新
- **封面管理**：内嵌封面、本地候选与在线搜索，封面与元数据可写回文件（Jaudiotagger）
- **USB 音频独占**：自动检测 USB DAC / 声卡并启用独占路由，实时展示播放链路（音频格式、源 / 输出采样率、位深、声道、DSD 模式、路由、输出策略与设备）
- **蓝牙耳机**：连接状态检测与会话级音量初始化
- **定时关闭**、播放模式与收藏

### 启动拦截

基于无障碍服务，可选结合 **Shizuku** 拦截指定应用的启动行为。

- 可配置**启动者**（可选）与**被拦截目标**应用
- **拦截时机**：立即、稍缓、延迟
- 触发时**终止启动者**进程（Shizuku 下提供防呆保护：连续最多 5 次，随后冷却 15 秒）
- 可选**终止被启动者**，并控制是否允许终止系统应用（均需 Shizuku）

### 应用切换黑名单

- 过滤切换到上一个应用时无需显示的应用
- 首次启动自动加入系统应用；权限感知，提供 `PackageManager` + `<queries>` 兜底
- 监听应用安装 / 卸载并自动更新

### 设置

- **主题模式**：浅色、深色或跟随系统
- **语言**：简体中文、English 或跟随系统；通过 `LocaleManager` API 应用内热切换，无需重建 Activity
- **自适应布局**：宽屏设备自动切换为双列布局
- **隐藏触发区**、**最近任务隐藏**、**避免输入法遮挡**
- **震动反馈**
- **屏幕翻译**目标语言 / 服务商处理
- **手势配置**：导入 / 导出完整手势与启动拦截配置
- **音乐缓存**：管理封面与歌词缓存

### 权限管理

通过独立卡片引导授权，授权完成后卡片自动隐藏，应用回到前台时自动检测权限状态。

- **无障碍服务**：实现系统级手势操作
- **悬浮窗**：手势触发区、扩展面板与悬浮面板
- **通知**：保持手势服务后台运行
- **省电优化**：忽略电池优化以保持存活
- **查询已安装应用**：完整扫描应用列表
- **媒体 / 图片**：本地曲库与专辑封面
- **全部文件访问**（可选）：写回音频元数据

### 应用内更新

WorkManager 周期检查 GitHub Releases（内部 24 小时冷却），发现新版本时弹出带更新日志的下载对话框。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 | Kotlin 2.4.10 |
| UI | Jetpack Compose（BOM 2026.08.00）+ Material 3 |
| 自适应布局 | Material3 Adaptive 1.3.0 |
| 依赖注入 | Koin 4.2.2 |
| 导航 | Navigation Compose 2.9.8（类型安全路由） |
| 状态 | DataStore + StateFlow + MutableStateFlow |
| 后台 | WorkManager 2.11.2 |
| 权限 | Shizuku 13.1.5 + 自定义 UserService |
| 音频 | Media3 ExoPlayer 1.11.0 + MediaSessionService |
| 图片加载 | Coil 3.5.0 |
| 网络 | OkHttp 5.4.0 |
| 序列化 | kotlinx.serialization 1.11.0 |
| Hidden API 绕过 | hidden-api-bypass 6.1 |
| 构建 | AGP 9.3.1、Gradle 9.7.0、refreshVersions、Jaudiotagger |

## 项目结构

```
.
├── app/
│   └── src/main/
│       ├── kotlin/com/edgegesture/evilgodxu/
│       │   ├── data/                    # 数据层（设置、应用仓库、Shizuku、翻译）
│       │   ├── di/                      # Koin 模块
│       │   ├── log/                     # CrashLogManager
│       │   ├── navigation/              # Navigation Compose 类型安全路由
│       │   ├── screens/                 # 页面
│       │   │   ├── gesture/             #   手势设置 + 边缘配置
│       │   │   │   └── service/         #   无障碍服务 + 悬浮 UI（音乐 / 扩展 / 任务 / 翻译 / 罗盘时钟）
│       │   │   ├── backtap/             #   背面双击
│       │   │   ├── expandpanel/         #   扩展面板设置
│       │   │   ├── blacklist/           #   应用切换黑名单
│       │   │   ├── launchblock/         #   启动拦截
│       │   │   └── settings/            #   应用设置 + 数据配置
│       │   ├── service/                 # Shizuku CommandUserService
│       │   ├── ui/                      # Material 3 主题 + 自适应窗口尺寸
│       │   ├── update/                  # 应用内更新
│       │   ├── utils/localization/      # 应用内多语言管理
│       │   ├── MainActivity.kt
│       │   └── MyApplication.kt
│       └── res/                         # 资源（values / values-en）
├── gradle/
│   ├── libs.versions.toml               # 版本目录（依赖管理）
│   └── wrapper/
├── LICENSE
├── NOTICE
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 架构

应用遵循 **MVVM + 单向数据流**：状态由 `ViewModel` → `UiState` → UI 自上而下流动，事件由 UI 自下而上传递；共享数据逻辑位于 `data/` 层并通过 Repository 暴露（设置、应用列表、启动拦截规则、翻译），全部由 Koin 组装。

所有手势与悬浮能力都位于 `screens/gesture/service/`。`EdgeGestureAccessibilityService` 负责检测滑动与背部双击，并通过 `AccessibilityActionExecutor` 派发操作。悬浮 UI（边缘触发区、扩展面板、音乐面板、任务面板、翻译、罗盘时钟）由各自的窗口管理器以系统窗口形式渲染，并由无障碍服务统一协调。设置通过 DataStore 持久化；可选的系统级操作（终止进程、小窗窗口）经由 Shizuku 的 `CommandUserService` 完成。

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

APK 输出为 `app/build/outputs/apk/` 下的 `EdgeGesture-<版本号>-arm64.apk`，仅构建 `arm64-v8a` ABI。

### 发布签名

Release 构建从项目根目录的 `local.properties` 读取签名凭据：

```properties
KEYSTORE_PASSWORD=你的签名库密码
KEY_ALIAS=your_key_alias
KEY_PASSWORD=你的别名密码
```

签名库文件默认位于项目根目录 `jh.keystore`（如需调整请修改 `app/build.gradle.kts` 中的 `storeFile`）。两个文件均已被 git 忽略，请勿提交。

## 免责声明

在线音乐搜索与屏幕翻译依赖第三方公共网络接口（网易云 / QQ 音乐 / 酷狗 / Jamendo / 翻译服务），其可用性与策略可能随地区与内容而异。应用与文档仅供个人学习交流使用，请支持正版版权方。

## License

[AGPL-3.0](LICENSE) © 2026 Evilgodxu