## 项目定位

- 单 Activity + Jetpack Compose 的无障碍工具应用：边缘手势、背面双击、扩展面板、启动拦截。`minSdk 34`、`targetSdk 37`，只发布 `arm64-v8a`。
- 真正的运行时在 `screens/gesture/service/`：无障碍服务持有触发区悬浮层、手势识别、动作执行与小窗/系统命令通道；改手势行为先读该目录，别从 UI 层倒推。
- `archive/removed-features/` 是已下线功能（音乐面板、罗盘时钟、屏幕翻译等）的留存副本，不参与构建；不要据它推断现行实现，也不要顺手把它接回构建。

## 技术栈与依赖

- 单 Activity + Compose（Material 3）+ Navigation3，状态管理为 MVVM + 单向数据流；Gradle 9.8.2 / AGP 9.5 / Kotlin 2.4.21 / JVM 21 / Compose BOM 2026.09.00。
- 依赖版本统一写在 `gradle/libs.versions.toml`（由 refreshVersions 生成），用 `./gradlew refreshVersions` 巡检；不手写单个依赖版本。
- 巡检插件从同级目录 `../refreshVersions/plugins` 的本地构建消费（`settings.gradle.kts` 的 `includeBuild`，故那里不写插件版本号）；实际版本由该目录的 `version.txt` 决定。改动该源码分支后先在其 `plugins/` 执行 `./gradlew publishToMavenLocal`，再回本仓库巡检。
- `settings.gradle.kts` 的 `rejectVersionIf` 已滤除带连字符的预发布标签；升级时不要绕过该过滤，也不要手改 toml 里的预发布版本。
- 实现顺序：目标平台原生 API ＞ 运行时环境内置能力 ＞ 语言标准库 ＞ 自行实现 ＞ 引入新依赖。新依赖须先确认，包括「只是个小工具库」。
- Release 统一走 AGP 的 `optimization { enable = true }`：同时开启代码优化、优化型资源缩减并内置等价于 `proguard-android-optimize.txt` 的默认规则，不要再写回 `isMinifyEnabled` / `isShrinkResources` / `getDefaultProguardFile`；项目自有规则仍在 `app/proguard-rules.pro`（AIDL、Shizuku、序列化 model 的 keep 都在其中）。
- `packaging` 里的 META-INF 排除、DEX 压缩、jniLibs 不压缩都是对分发体积的取舍结果，回退前需说明代价。
- 产物名由 `androidComponents.onVariants` 统一改写为 `EdgeGesture-<versionName>-arm64.apk`，不要在别处另设文件名。
- `minSdk 34` 意味着不需要低版本兼容分支：弃用 API 直接迁移到等价新接口，不写双路径、不加版本判断兜底。

## 架构与状态

```text
DataStore ⇄ Repository ⇄ ViewModel ⇄ UiState ⇄ Composable   状态向下
Composable → ViewModel 方法 → Repository → DataStore          事件向上
```

- **手动 DI**：`App` 在 `onCreate` 构造 `AppContainer`，容器按需 `lazy` 提供 `settingsRepository`、`updateCheckRepository`、`localizationManager` 与 `appVersion`。Activity 经 `viewModelFactory` + `initializer` 注入构造参数。不引入 Hilt / Koin / Dagger。
- **应用级状态**（主题 / 语言 / 版本）由 Activity 作用域的 `MainViewModel` 聚合为 `AppUiState`，经 `LocalMainViewModel` 下发；主题、本地化与各页面共用这一份，界面层不直连数据源。首帧状态由宿主在进入组合前读出持久化值注入构造，避免冷启动先渲染默认主题再切换。
- **页面级状态**由 `{Screen}ViewModel` 持有 `MutableStateFlow<{Screen}UiState>`，对外只暴露不可变 `StateFlow`；作用域由 Navigation3 的 `rememberViewModelStoreNavEntryDecorator()` 绑定到导航条目，出栈即回收。
- 更新域由 `UpdateViewModel` 持有单一 `UpdateUiState`（待更新信息 + 对话框显隐 + 下载进度），`LocalUpdateViewModel` 下发；自动检查进入新一天的判定在 ViewModel/仓库内完成，页面不参与。
- 状态变化用框架原生机制响应：`StateFlow`、`collectAsStateWithLifecycle()`、`CompositionLocal`。禁止把状态包成自定义回调层层转发，也禁止为同一份状态另存副本（含在 ViewModel 里镜像数据层状态）。
- 页面 ViewModel 只依赖仓库与纯逻辑工具，不直接触碰 DataStore 键值、文件或网络。
- 导航集中在 `navigation/`：路由键在 `Screen.kt`，`NavGraph.kt` 负责 `NavDisplay` 装配与返回分发；页面不直接操作 `NavBackStack`。
- 异步表达：一次性调用用 `suspend` 函数，持续数据用 `Flow`；已具备协程能力的调用方不额外设计回调接口。
- 现有部分页面仍是单文件直写布局（`GestureSettingsScreen` 1600+ 行）。新增或大范围重排时按「页面模块与组件归属」收敛，不做只为搬家的全量移动。

## 数据持久化

- Preferences DataStore 是唯一数据源。设置与更新检查记录共用 `AppContainer` 内的同一份 `settings` DataStore；手势设置、启动拦截各自持库（`gesture_settings`、`launch_block`）。不引入 SharedPreferences、Room 或内存副本。
- 新增持久化先定义仓库接口（`data/repository/`），再写 DataStore 实现；数据源经构造注入，保证单测可替换。
- 键集中在各 `*Keys` object：`SettingsKeys`、`GestureSettingsKeys`、`ExpandPanelSettingsKeys`、`ExpandPanelFreeformKeys`、`LaunchBlockKeys`。新增设置项先加键，再补仓库方法或 `Context` 扩展函数。
- 写入一律 `suspend` + `dataStore.edit {}`；同一次变更涉及多个键时放同一个 `edit` 内，保证原子。
- 读取字符串型枚举必须回落默认值而非抛错（范式：`ThemeMode.fromValue`、`AppLanguage.fromTag`）。
- 迁移旧存储时在仓库内部做一次性搬迁并以标记位收敛（范式：`DataStoreUpdateCheckRepository` 从 `update_prefs` 搬到 DataStore），不要让页面感知新旧差异。
- 配置导入导出走 `DataConfigManager`：`FORMAT_VERSION` 与导出键范围（正则 + 开关白名单）是同一份契约，改任一侧都要同步另一侧并升版本；导入先全量校验类型，再写入，避免中途异常清空配置。

## 无障碍服务与特权通道

- 触发区是 `TYPE_ACCESSIBILITY_OVERLAY` 悬浮视图（`AccessibilityEdgeViewManager`），扩展面板是挂在 `TYPE_ACCESSIBILITY_OVERLAY` 上的 `ComposeView` 并自建 `Lifecycle` / `ViewModelStore` / `SavedStateRegistry` owner（`ExpandPanelViewManager`），定时提醒覆盖层走 `TYPE_APPLICATION_OVERLAY`（`RemindAlarmService`）。层级选型取决于是否有无障碍服务上下文，不要随手互换。
- 悬浮视图的增删受 `Mutex` 保护，且遵循「先移除再添加」；`WindowManager.BadTokenException` 必须转成对用户的授权引导，不能只吞掉。
- 服务实例以 `WeakReference` 暴露（`getInstance()` / `isAvailable()`），对外只提供 `startGesture` / `stopGesture` / `updateSettings` 这类静态入口；外部不得持有服务强引用。
- 服务内协程统一 `SupervisorJob + Dispatchers.IO`；视图操作与 `Handler` 回调切回主线程（`Handler(Looper.getMainLooper())`）。不要在服务里新建无作用域约束的 `GlobalScope`。
- 服务读设置走 `gestureSettingsFlow()` 收集回落；写完设置必须触发 `refreshSettings()` / `updateSettings()`，否则 UI 与服务状态分叉。需要单值的应用级设置（如主题）经 `AppContainer` 的仓库取流，不要在服务里另建数据源。
- Shizuku 通道：AIDL `ICommandService` + `CommandUserService`（独立进程执行 shell）。`UserServiceArgs` 必须设置 `processNameSuffix`，否则绑定即抛 `process name suffix must not be null`。命令超时、并发读 stdout/stderr 已有处理，新增命令沿用同一封装并处理失败返回值，不要自起 `Runtime.exec`。
- Shizuku 状态由 `ShizukuManager` 单一持有（`state: StateFlow`），授权结果监听在 `App` 启动时注册；页面只读状态，不再各自维护副本。
- `HiddenApiBypass` 仅限没有公开等价 API 的能力（当前只有 `FreeformAppLauncher` 的小窗）；有公开 API 时必须用公开 API。
- 权限判定归数据层与 ViewModel：`PermissionMonitor` 是统一判定入口，ViewModel 汇总成 `UiState` 的权限标志，Composable 只读状态、不自行 `checkSelfPermission`（现状 `GestureSettingsViewModel` 仍有两处直连系统 API，新增权限一律走 `PermissionMonitor`）；缺权限时功能降级并可恢复，不是崩溃。

## UI 与自适应

- `MainActivity` 统一处理 edge-to-edge、横屏系统栏显隐、回前台自动检查更新；崩溃捕获在 `App` 里最先初始化。页面不重复做这些。
- 窗口尺寸类由 `ProvideWindowSizeClass` 在 Activity 层按窗口容器尺寸（`LocalWindowInfo.containerSize`）计算，页面用 `currentWindowSizeClass()` 读取；组件不自行读窗口尺寸。
- 形态差异优先用「行宽上限」表达：`ui/component/ContentPane` 负责单栏收窄，行宽阈值由 `WindowSizeClass.contentMaxWidth` 给出，同一份内容不维护两套布局。
- 不要改用 `Configuration.screenWidthDp` 或 `currentWindowAdaptiveInfo`：前者被取整且与 inset 行为耦合，后者在 Android 16 上要求带 display 关联的 UI Context，非 UI 上下文会抛异常。
- 用户可见文案一律进 `res/values/strings.xml`（中文为默认，`resources.properties` 声明 `unqualifiedResLocale=zh`）与 `res/values-en/strings.xml`，同一 commit 内成对补齐；代码里不出现硬编码文案。带数量的文案用 `<plurals>` + `pluralStringResource` / `getQuantityString`。`localeFilters` 只保留 `zh` / `en`，`generateLocaleConfig` 由 AGP 生成清单声明，不手写 `locale_config.xml`。
- 语言热切换：`LocalizationManager` 负责语言转 Locale 与创建本地化 Context，`ProvideLocalizedContext` 同时提供 `LocalContext`、`LocalResources`、`LocalActivity` 与 `ActivityResultRegistryOwner`；新增独立窗口（Dialog、悬浮层）须走同一组合局部，不要各自 `setLocale`。
- 主题：配色令牌在 `ui/theme/Color.kt`，主题模式由 `ThemeMode` 驱动；新增颜色走令牌，不内联 `Color(0xFF…)`。
- 图标优先用 `material-icons-extended` 已有项；需要自绘矢量时从本机仓库根 `Material Symbols/`（outlined SVG，不入版本库）取样，不手画近似图标。

## 页面模块与组件归属

```text
screens/<page>/
├── <Page>Screen.kt        # 骨架：ViewModel 连接、尺寸类、副作用、页面级弹层
├── <Page>UiState.kt       # 页面状态
├── <Page>ViewModel.kt     # 状态编排
├── assembly/              # 组装器：布局装配（区块结构差异大时改为 compact/ + expanded/）
└── component/             # 页面组件，按业务语义分子目录（如 appearance/、dialog/）
```

- 页面根包只放骨架三件套；其余文件进 `component/`（或按业务语义新建子目录）。跨页面复用上提 `ui/component/`，无复用的留在页面内。
- 骨架只做连接与副作用，布局装配下放到 `assembly/` 或同文件的 `private` Composable；组装器只消费一份 `UiState` 与一套事件回调，`compact/` 与 `expanded/` 若拆出也必须共享这两者。
- 页面级弹层挂在骨架层，不随形态重复实现；弹窗自身放在 `component/dialog/`。
- 拆分判据是「单一问题」：可独立交互 / 预览 / 测试的区块才拆组件；单行 `Text`、`Spacer` 内联。
- 设置页（`screens/settings/`）是这套结构的现行范例。

## 日志与崩溃

- `CrashLogManager` 是唯一崩溃落盘入口，已在 `App` 最先注册为默认 `UncaughtExceptionHandler`；业务里不要再建第二套日志框架。
- 临时埋点日志在验证结束后删除，不保留防御性日志。
- 失败要给出可执行路径（引导授权、重试、回退），只 `Log` 不提示视为未完成。

## 单元测试

- 位置 `app/src/test/kotlin/`，只覆盖纯逻辑（枚举回落、配置编解码、统计口径）与状态持有者（ViewModel 的写入与回流）；不测像素级布局。
- junit4 + `kotlinx-coroutines-test`：`Dispatchers.setMain(StandardTestDispatcher())` 配合 `advanceUntilIdle()`，实验性 API 显式标注 `@OptIn`。
- 用内存 fake 实现仓库接口（参照 `FakeSettingsRepository`），禁止引入 Mockito / MockK。
- 测试方法名用反引号中文描述行为（如 `` `写入主题经仓库回流更新状态` ``），断言对外可见状态，不断言私有实现。
- 新增状态持有者时同步补测试；`app/build.gradle.kts` 已通过 `HasUnitTestBuilder.enableUnitTest` 打开各变体的单元测试。

## 构建验证

- 改动后至少验证 `:app:assembleDebug`、`:app:testDebugUnitTest`、`:app:lintDebug`。
- 涉及 UI 与资源时追加 `:app:assembleRelease`（R8 与资源优化路径和 debug 不同）。
- lint 必须保持 **0 errors / 0 warnings**：`app/build.gradle.kts` 已设 `lint { abortOnError = true; checkReleaseBuilds = true }`，`app/lint.xml` 把 `Deprecated` 与 `UnsafeOptIn*` 提级为 error；新增问题当轮修掉，不积压、不用 baseline 文件兜底。
- Kotlin 已开启 `allWarningsAsErrors`，警告即构建失败；弃用 API 必须迁移到等价接口，不用抑制。
- 签名读 `local.properties`（`KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`）+ 仓库根 `jh.keystore`；口令不进仓库、不进日志。
- 版本号 `versionCode` / `versionName` 在 `app/build.gradle.kts`，发版时同步 `README.md` / `README.zh-CN.md` 顶部的版本徽章。

## Lint 抑制与未使用资源

| 场景 | 做法 |
|---|---|
| 单个 XML 元素可标注 | 该元素上 `tools:ignore="检查名"`（根元素补 `xmlns:tools`），如清单里的 `QueryAllPackagesPermission`、`ProtectedPermissions` |
| 单处 Kotlin 代码可标注 | 该声明上 `@SuppressLint("检查名")` 并写明理由，如 `AppRepository` 的 `StaticFieldLeak`、`BackTapDetector` 的 `WakelockTimeout` |
| 与源码位置无关或取决于分发方式 | `app/lint.xml` 内 `<issue id="..." severity="ignore" />` 并写明理由，如 `ChromeOsAbiSupport`、`BatteryLife` |

- 定向优先于整体：能只排除一处就不要 `disable` 整个检查；每处抑制写明「为什么这是有意为之」，二进制资源的理由写在 `app/lint.xml` 该条目的注释里。
- 能真修就不要抑制：先确认该项是否指了真实缺陷（例：`IconLocation` 指出位图放在无密度限定的 `drawable/`，正解是移入 `drawable-nodpi/`）。
- 删除 `UnusedResources` 告警的资源前，须依次确认：依赖库是否打包了同名资源（查本地 AAR）、库的 `values.xml` 是否按名引用（`<drawable name="X">@drawable/<资源名></drawable>` 这类间接引用不在应用源码里，「全仓库搜不到」不足以判定废弃）、是否为本应用的自定义覆盖（比对字节哈希）。确认误报后用定向 `lint.xml` 排除，不删资源。

## 注释

- 解释意图与设计决策，不复述代码行为。
- 代码变更时同步更新，不留过期注释。
- 不写内容溯源与背景交代（「曾经」「来自某版本」这类），历史交给 git。

## Git 与变更纪律

- 提交信息用中文，`type: 描述`（`feat` / `fix` / `refactor` / `build` / `docs` / `ci`），与现有 `git log` 保持一致。
- 删除须指定精确路径、逐条列举、分批执行；严禁通配符匹配或递归大范围删除；删除前确认存在可回退存档（下线功能移入 `archive/removed-features/` 并保留对应资源副本）。
- 实现方式不得通过以下手段掩盖真实修改范围：跨层内联到职责无关的类、以分散状态标志绕过类型系统、对既有回调接口二次包装。
- 调用链路取最短：不设只做转发的中间层；拆分以抑制碎片化为界，聚合以上帝类为界。

## 未明确决策

- 设计存在多种方案时择优；影响面超出单文件且无官方推荐时，先询问确认后再动手。
- 当前技术栈有官方推荐做法时，按官方推荐处理。
