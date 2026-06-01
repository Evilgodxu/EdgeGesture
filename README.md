# EdgeGesture

一款基于 Android 无障碍服务的边缘手势应用，支持在屏幕边缘触发多种快捷操作，提升大屏设备单手操作体验。

## 功能特性

### 边缘手势

- **左侧边缘手势**：支持 6 种滑动手势（短滑/长滑 × 右/上/下方向），最多支持 3 段分区
- **右侧边缘手势**：支持 6 种滑动手势（短滑/长滑 × 左/上/下方向），最多支持 3 段分区
- **底部边缘手势**：支持 6 种滑动手势（短滑/长滑 × 左/右/上方向），最多支持 3 段分区
- **自定义触发区域**：可调整边缘宽度、高度百分比、位置百分比和分段数量
- **手势反馈**：支持震动反馈

### 快捷操作

- **返回上一级**：模拟系统返回键
- **返回桌面**：返回系统桌面
- **最近任务**：打开最近任务列表
- **上一个应用**：快速切换到最近使用的应用
- **上一曲/下一曲**：控制媒体播放
- **手电筒**：开关闪光灯
- **语音助手**：启动系统语音助手
- **电源菜单**：显示电源选项
- **锁屏**：锁定屏幕
- **截屏**：截取屏幕
- **扩展面板**：显示快捷设置面板（亮度/音量调节/应用快捷方式）
- **无操作**

### 应用设置

- **主题切换**：浅色/深色/跟随系统
- **语言切换**：简体中文/English（运行时切换）
- **应用黑名单**：切换应用时忽略黑名单应用
- **震动反馈**：滑动操作时触发震动效果
- **隐藏显示**：隐藏手势触发区域
- **隐藏后台**：在最近任务列表中隐藏本应用
- **避免遮挡**：输入法弹出时自动禁用手势触发区

### 启动拦截

- **拦截规则**：基于 Shizuku 权限拦截指定应用的启动行为
- **高频启动检测**：短时间内高频启动时终止启动者进程
- **进程终止**：拦截后立即终止被启动应用进程

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 2.3.21 |
| UI 框架 | Jetpack Compose (BOM 2026.05.01) + Material3 |
| 架构模式 | MVVM + UDF（单向数据流） |
| 依赖注入 | Koin 4.2.1 |
| 导航框架 | Navigation 2.9.8 |
| 状态管理 | DataStore + StateFlow |
| 后台任务 | WorkManager |
| 权限框架 | Shizuku 13.1.0 |
| 序列化 | Kotlin Serialization 1.6.3 |
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
- **必要权限**：
  - 无障碍服务权限（用于检测边缘触摸事件和执行系统操作）
  - 悬浮窗权限（创建边缘手势触发区域）
  - 使用情况访问权限（获取最近使用应用信息）
  - 查询所有应用权限（管理应用黑名单）
  - Shizuku 权限（启动拦截的高级功能）
  - 相机权限（手电筒功能）

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
3. **调整触发区域**：根据使用习惯调整边缘宽度、高度、位置和分段数量
4. **应用黑名单**：在设置中添加不需要手势的应用
5. **启动拦截**（可选）：安装并启动 Shizuku 后配置拦截规则

## 项目结构

```
app/src/main/kotlin/com/byss/jh/
├── data/                    # 数据层
│   ├── app/                # 应用信息数据
│   ├── gesture/            # 手势设置数据存储
│   ├── launchblock/        # 启动拦截规则存储
│   ├── privacy/            # 隐私设置数据存储
│   └── shizuku/            # Shizuku 权限管理
├── di/                      # 依赖注入模块
├── navigation/              # 导航配置
├── screens/                 # UI 层（页面）
│   ├── gesture/            # 手势设置页面
│   │   ├── components/     # 手势设置相关组件
│   │   └── service/        # 无障碍服务及手势检测
│   ├── privacy/            # 隐私协议页面
│   ├── settings/           # 设置页面
│   │   └── components/     # 设置相关组件
│   └── splash/             # 启动页
├── ui/                      # UI 基础组件
│   ├── adaptive/           # 自适应布局
│   └── theme/              # 主题配置
├── MainActivity.kt          # 主 Activity
└── MyApplication.kt         # 应用入口
```

## 架构说明

本项目采用 **MVVM + UDF（单向数据流）** 架构：

- **UI 层**：Jetpack Compose 实现声明式 UI
- **ViewModel 层**：管理 UI 状态和业务逻辑
- **数据层**：DataStore 持久化存储用户设置
- **服务层**：AccessibilityService 监听边缘手势和执行系统操作

## 隐私说明

- 本应用使用无障碍服务仅用于检测屏幕边缘触摸事件和执行系统操作
- 不收集、不上传任何用户数据
- 所有设置数据仅存储在本地设备
- 应用切换历史仅保存在内存中，关闭后自动清除

## 开源协议

本项目采用 [GNU Affero General Public License v3.0](LICENSE) 开源协议。

```
Copyright (C) 2024-2025 Evilgodxu (江寒)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

## 打赏支持

如果这个项目对你有帮助，欢迎支持开发者。

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/Evilgodxu">Evilgodxu</a>
</p>
