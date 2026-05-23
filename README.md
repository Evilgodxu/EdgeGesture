# EdgeGesture

一款基于 Android 无障碍服务的边缘手势应用，支持在屏幕边缘触发多种快捷操作，提升大屏设备单手操作体验。

## 功能特性

### 边缘手势

- **左侧边缘手势**：支持 6 种滑动手势（短滑/长滑 × 右/上/下方向）
- **右侧边缘手势**：支持 6 种滑动手势（短滑/长滑 × 左/上/下方向）
- **底部边缘手势**：支持 6 种滑动手势（短滑/长滑 × 左/右/上方向）
- **自定义触发区域**：可调整边缘宽度和分段数量
- **手势反馈**：支持震动反馈和视觉提示开关

### 快捷操作

- **返回上一级**：模拟系统返回键
- **返回桌面**：返回系统桌面
- **最近任务**：打开最近任务列表
- **上一个应用**：快速切换到最近使用的应用
- **展开面板**：显示快捷设置面板（亮度/音量调节等）
- **无操作**：禁用该手势

### 应用设置

- **主题切换**：浅色/深色/跟随系统
- **语言切换**：简体中文/English（运行时切换）
- **应用黑名单**：指定应用内禁用手势
- **捐赠支持**：支持应用开发

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 2.3.21 |
| UI 框架 | Jetpack Compose (BOM 2026.05.00) + Material3 |
| 架构模式 | MVVM + UDF（单向数据流） |
| 依赖注入 | Koin 4.2.1 |
| 导航框架 | Navigation 2.9.8 |
| 状态管理 | DataStore + StateFlow |
| 后台任务 | WorkManager |
| 构建工具 | AGP 9.2.1, Gradle |
| 许可证 | AGPL-3.0 |

## 运行环境

| 属性 | 值 |
|------|-----|
| applicationId | com.byss.jh |
| versionName | 1.0.0 |
| compileSdk | 36 |
| minSdk | 32 (Android 12L) |
| targetSdk | 36 |
| NDK | arm64-v8a |
| Java | 21 |

## 系统要求

- **Android 版本**：Android 12L (API 32) 及以上
- **必要权限**：无障碍服务权限（用于检测边缘触摸事件）
- **可选权限**：修改系统设置（用于扩展面板的亮度/音量调节）

## 安装说明

### 从源码构建

```bash
# 克隆仓库
git clone https://github.com/Evilgodxu/kotlin-android-EdgeGesture.git

# 进入项目目录
cd kotlin-android-EdgeGesture

# 构建 Release 版本
./gradlew assembleRelease
```

构建完成后，APK 文件位于 `app/build/outputs/apk/release/` 目录。

### 签名配置

在 `local.properties` 文件中配置签名信息：

```properties
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

## 使用说明

1. **首次启动**：授予无障碍服务权限
2. **手势设置**：进入手势设置页，为各边缘配置想要的快捷操作
3. **调整触发区域**：根据使用习惯调整边缘宽度和分段
4. **应用黑名单**：在设置中添加不需要手势的应用

## 项目结构

```
app/src/main/kotlin/com/byss/jh/
├── data/                    # 数据层
│   ├── gesture/            # 手势设置数据存储
│   └── privacy/            # 隐私设置数据存储
├── di/                      # 依赖注入模块
├── navigation/              # 导航配置
├── ui/                      # UI 层
│   ├── adaptive/           # 自适应布局
│   ├── gesture/            # 手势设置页面
│   ├── privacy/            # 隐私协议页面
│   ├── settings/           # 设置页面
│   ├── splash/             # 启动页
│   └── theme/              # 主题配置
├── util/                    # 工具类
├── MainActivity.kt          # 主 Activity
└── MyApplication.kt         # 应用入口
```

## 架构说明

本项目采用 **MVVM + UDF（单向数据流）** 架构：

- **UI 层**：Jetpack Compose 实现声明式 UI
- **ViewModel 层**：管理 UI 状态和业务逻辑
- **数据层**：DataStore 持久化存储用户设置
- **服务层**：AccessibilityService 监听边缘手势

## 隐私说明

- 本应用使用无障碍服务仅用于检测屏幕边缘触摸事件
- 不收集、不上传任何用户数据
- 所有设置数据仅存储在本地设备

## 开源协议

本项目采用 [GNU Affero General Public License v3.0](LICENSE) 开源协议。

## 打赏支持

如果这个项目对你有帮助，欢迎支持开发者。

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/Evilgodxu">Evilgodxu</a>
</p>
