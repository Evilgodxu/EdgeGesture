# 代码注释规范

## 基本原则

- 注释应简洁明了，避免冗余
- 优先使用单行注释 `//`，减少不必要的多行注释
- 注释内容应准确描述代码功能，避免无效信息占用行数

## 注释格式规范

### 1. 单行注释

使用双斜杠 `//`，适用于：

- 简单的函数/类说明
- 单行代码的解释
- 简短的参数说明

```kotlin
// 手势设置页面 ViewModel，管理手势开关、边缘尺寸、手势动作等设置状态
class GestureSettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    // 设置主题模式
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            context.saveThemeMode(mode)
        }
    }
}
```

### 2. 多行注释（KDoc）

仅用于需要详细文档说明的公共 API，格式为 `/** */`：

- 需要详细描述参数和返回值
- 需要多行详细说明的复杂逻辑
- 对外暴露的公共接口文档

```kotlin
/**
 * 执行复杂的业务逻辑处理
 *
 * @param input 输入参数，需要满足特定格式
 * @return 处理结果，包含成功/失败状态
 * @throws IllegalArgumentException 当输入参数不合法时抛出
 */
fun processData(input: String): Result {
    // 实现逻辑
}
```

## 禁止的写法

### ❌ 单行内容使用多行注释

```kotlin
// 错误示例 - 只有一行内容却占用3行
/**
 * 设置主题模式
 */
fun setThemeMode(mode: ThemeMode) { }

// 正确写法 - 简洁的单行注释
// 设置主题模式
fun setThemeMode(mode: ThemeMode) { }
```

### ❌ 无意义的注释

```kotlin
// 错误示例 - 注释与代码重复，无额外信息
// 设置变量x的值为1
val x = 1

// 正确写法 - 说明业务含义
// 默认手势触发阈值（单位：dp）
val x = 1
```

## 文件头注释

文件顶部不需要添加文件头注释（如作者、日期等），Git 会记录这些信息。

## 类/接口注释

```kotlin
// 简单的类说明使用单行注释
// 扩展面板快捷方式存储键
object ExpandPanelSettingsKeys { }

// 只有需要详细说明时才使用 KDoc
/**
 * 手势动作枚举
 *
 * 显示名称通过 [getActionDisplayName] 函数从字符串资源获取，支持多语言
 */
enum class GestureAction { }
```

## 函数注释

```kotlin
// 简单的函数使用单行注释
// 检查无障碍服务是否已启用
fun checkAccessibilityEnabled(): Boolean { }

// 需要说明参数和返回值的复杂函数使用 KDoc
/**
 * 从 UsageStatsManager 获取最近使用的应用
 *
 * @param blacklist 应用包名黑名单
 * @return 最近使用的应用包名，如果没有则返回 null
 */
private fun getLastAppFromUsageStats(blacklist: Set<String>): String? { }
```

## 代码块注释

对于复杂逻辑块，使用单行注释分段说明：

```kotlin
fun processGesture() {
    // 步骤1：验证手势有效性
    if (!isValidGesture()) return
    
    // 步骤2：执行对应动作
    executeAction()
    
    // 步骤3：触发震动反馈
    if (vibrationEnabled) vibrate()
}
```

## 总结

| 场景 | 推荐格式 | 示例 |
|------|----------|------|
| 简单类/函数说明 | `//` | `// 设置主题模式` |
| 复杂 API 文档 | `/** */` | 带 @param @return 的 KDoc |
| 代码逻辑解释 | `//` | `// 延迟确保系统初始化完成` |
| 多行详细说明 | `/** */` | 需要多段落说明的场景 |

---

**核心原则：能写一行就不写三行，保持代码简洁。**
