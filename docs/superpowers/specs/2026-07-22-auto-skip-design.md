# 一键跳过（Auto Skip）功能设计文档

## 概述

"一键跳过"功能通过无障碍服务自动识别当前窗口中的"跳过/关闭/Skip/Close"等按钮并执行点击，同时支持用户手动标记位置保存为规则。功能由后台自动监测和手势动作触发两条路径驱动。

## 数据模型

### SkipRule（Room 实体）

```kotlin
@Entity(tableName = "skip_rules")
data class SkipRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,      // 应用包名，如 "com.example.app"
    val activityName: String,     // 活动名（窗口ID），如 "com.example.MainActivity"
    val clickX: Float,            // 点击坐标 X（相对屏幕）
    val clickY: Float,            // 点击坐标 Y（相对屏幕）
    val note: String = "",        // 用户备注
    val state: String = "ACTIVE", // ACTIVE | DISMISSED
    val createdAt: Long = System.currentTimeMillis()
)
```

### 状态说明

| 状态 | 含义 | 自动行为 |
|------|------|----------|
| `ACTIVE` | 已保存的跳过规则 | 窗口匹配时自动点击 |
| `DISMISSED` | 用户拒绝保存 | 永不自动执行/询问，除非用户手势主动触发 |

## 文件清单及变更

### 新增文件

| 文件路径 | 职责 |
|----------|------|
| `data/skip/SkipRule.kt` | Room 实体类 |
| `data/skip/SkipDatabase.kt` | Room 数据库 + DAO (增删查改) |
| `data/skip/SkipRepository.kt` | 数据仓库封装 |
| `screens/gesture/service/AutoSkipManager.kt` | 后台自动跳过管理器（窗口监听 + 规则匹配 + 模拟点击 + 标记模式） |
| `screens/skip/SkipSettingsScreen.kt` | 管理页面（列表、删除、编辑备注、手动添加） |

### 修改文件

| 文件路径 | 变更内容 |
|----------|----------|
| `data/gesture/GestureSettings.kt` | `GestureAction` 枚举新增 `AUTO_SKIP("auto_skip")` |
| `screens/gesture/components/ActionDisplayName.kt` | 新增 `AUTO_SKIP` 的显示名映射 |
| `screens/gesture/service/EdgeGestureAccessibilityService.kt` | `onCreate` 中启动 `AutoSkipManager`，`onDestroy` 中关闭 |
| `navigation/NavGraph.kt` | 新增 `SkipRoute` 路由 |
| `screens/gesture/GestureSettingsScreen.kt` | 更多功能网格中新增"一键跳过"入口 |
| `res/values/strings.xml` | 新增相关字符串资源 |
| `res/values-en/strings.xml` | 新增对应英文翻译 |

## 行为流程

### 1. 后台自动模式（服务运行即启动）

```
AutoSkipManager.onCreate()
  │
  ▼
监听 AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
  │
  ▼
窗口 activityName 变化
  │
  ├── 查询 Room 匹配 (packageName + activityName)
  │
  ├── 命中 ACTIVE ──► 执行 dispatchGesture 点击 (clickX, clickY)
  │
  ├── 命中 DISMISSED ──► 跳过，不做任何事
  │
  └── 无记录 ──► 遍历无障碍节点树
                   │
                   ├── 找到文本匹配按钮 ──► ① 获取按钮坐标
                   │                          ② dispatchGesture 点击
                   │                          ③ 弹出悬浮窗询问"是否保存该跳过配置？"
                   │                           ├── 同意 → 保存为 ACTIVE
                   │                           └── 拒绝 → 保存为 DISMISSED
                   │
                   └── 未找到 ──► 静默，不做任何事
```

### 2. 手势动作触发（用户滑动触发 `AUTO_SKIP`）

```
用户执行手势 → performAction(AUTO_SKIP)
  │
  ▼
查 Room 匹配当前窗口 (packageName + activityName)
  │
  ├── 命中 ACTIVE ──► 执行点击
  │
  ├── 命中 DISMISSED ──► 进入标记模式（覆盖拒绝状态）
  │
  └── 无记录 ──► 进入标记模式（后台自动已确认当前窗口无可识别按钮）
```

### 3. 标记模式

```
覆盖半透明遮罩 + 提示文本"请点击要跳过的位置"
  │
  ▼
用户点击屏幕
  │
  ▼
记录坐标 (clickX, clickY) + 当前窗口 (packageName, activityName)
  │
  ▼
弹出确认对话框（显示包名、活动名、坐标信息）
  ├── 确认保存
  │     ├── 保存为 ACTIVE
  │     └── 执行 dispatchGesture 点击该坐标
  │
  └── 取消 → 退出标记模式，不保存
```

### 4. 管理页面（从更多功能网格进入）

```
SkipSettingsScreen
  │
  ├── 列表展示所有 SkipRule（按创建时间倒序）
  │     ├── 每行显示：
  │     │   - 应用名称/图标
  │     │   - 活动名
  │     │   - 坐标 (x, y)
  │     │   - 备注文本
  │     │   - 状态标签 (ACTIVE/DISMISSED)
  │     │   - 删除按钮
  │     ├── 点击行 → 编辑备注（弹窗输入框）
  │     └── 左滑/长按 → 删除确认
  │
  └── 悬浮按钮「+ 添加规则」
        └── 进入标记模式
```

## 窗口匹配机制

使用 `packageName` + `activityName`（从 `AccessibilityEvent.className` 获取）作为窗口的唯一标识。当新窗口打开时，查询 Room 中 `packageName` 和 `activityName` 均匹配的规则。

`AutoSkipManager` 维护两个缓存字段 `currentPackageName` / `currentActivityName`，在每次 `TYPE_WINDOW_STATE_CHANGED` 事件中更新。手势动作触发时从缓存中读取当前窗口信息用于 Room 查询，**不重新扫描**无障碍节点树。

## 无障碍节点扫描

遍历当前窗口的根节点（`rootInActiveWindow`）的所有子节点，递归查找满足以下条件的节点：
- `text` 或 `contentDescription` 匹配关键词：跳过、关闭、Skip、Close
- `isClickable == true` 或父节点可点击
- 优先选择最匹配文本的叶子节点

获取匹配节点的屏幕坐标（通过 `getBoundsInScreen()`），用于后续自动点击。

## 自动点击

使用 `AccessibilityService.dispatchGesture()` 在目标坐标执行点击：

```kotlin
val click = GestureDescription.Builder()
    .addStroke(GestureDescription.StrokeDescription(
        Path().apply { moveTo(x, y) }, 0, 100
    ))
    .build()
service.dispatchGesture(click, null, null)
```

## 字符串资源

### values/strings.xml（新增）

```xml
<!-- 一键跳过 -->
<string name="gesture_action_auto_skip">一键跳过</string>
<string name="skip_settings_title">一键跳过</string>
<string name="skip_add_rule">添加跳过规则</string>
<string name="skip_delete_rule">删除</string>
<string name="skip_edit_note">编辑备注</string>
<string name="skip_note_hint">输入备注说明</string>
<string name="skip_no_rules">暂无跳过规则</string>
<string name="skip_mark_mode_hint">请点击要跳过的位置</string>
<string name="skip_save_confirm_title">保存跳过配置</string>
<string name="skip_save_confirm_body">是否保存此页面的跳过位置？\n%s\n坐标：(%d, %d)</string>
<string name="skip_save">保存</string>
<string name="skip_discard">不保存</string>
<string name="skip_state_active">已启用</string>
<string name="skip_state_dismissed">已忽略</string>
<string name="skip_feedback_saved">已保存跳过规则</string>
```

### 英文翻译对应补充至 values-en/strings.xml

## 注意事项

1. 自动保存规则时，必须先执行点击再弹窗，确保用户体验流畅
2. 用户拒绝保存后标记为 DISMISSED，避免同一窗口反复询问
3. 手势触发可覆盖 DISMISSED 状态，提供手动补救通道
4. 点击使用 dispatchGesture 而非 performAction(ACTION_CLICK)，兼容性更好
5. 使用 Room 数据库存储规则，支持结构化查询和未来扩展
