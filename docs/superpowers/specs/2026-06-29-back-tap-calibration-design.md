# 背面双击自动校准设计

## 背景

EdgeGesture 的背面双击功能通过 BackTapDetector 识别用户双击手机背面，触发指定操作。当前灵敏度与检测范围由用户手动在 1–10 之间调节，对普通用户不够直观。新增自动校准模式，让用户双击背面约 10 次，应用根据真实采集的数据自动计算并写入合适的灵敏度与检测范围。

## 目标

- 在校准对话框内采集约 10 次真实背面双击。
- 以采集到的单击峰值幅度和双击间隔为基准，自动计算出灵敏度与检测范围。
- 计算结果写入 DataStore，无障碍服务立即生效。
- 校准期间避免触发用户配置的背面双击动作。

## 方案

采用“设置页内嵌校准对话框 + 临时独立检测器”方案。

## 架构变更

### 新增组件

1. **BackTapCalibrationManager**
   - 生命周期与校准对话框绑定。
   - 持有临时 BackTapDetector 实例。
   - 接收每次单击的峰值幅度与时间戳，配对成双击。
   - 收集满 10 组后计算参数并保存。

2. **BackTapCalibrationDialog**
   - Compose AlertDialog。
   - 显示说明、进度、结果或错误。
   - 管理超时与取消。

### 修改组件

1. **BackTapDetector**
   - 新增可选校准回调 `onCalibrationTap(timestampNs: Long, amplitude: Float)`。
   - 校准模式下每次识别到单击即上报，不执行双击判断后的动作。

2. **BackTapSettingsSection**
   - 背面双击开关打开后，显示“自动校准”按钮。

3. **DataStore（GestureSettings.kt）**
   - 新增 `BACK_TAP_CALIBRATING` boolean key。

4. **EdgeGestureAccessibilityService**
   - 监听 `backTapCalibrating` 标志。
   - 标志为 true 时，BackTapDetector 的双击回调中不执行动作。

## 数据流

1. 用户展开背面双击设置 → 点击“自动校准”。
2. 弹窗打开，ViewModel 写入 `back_tap_calibrating = true`。
3. 创建 BackTapDetector（Application Context），以灵敏度 10、范围 10 启动校准模式。
4. 用户双击背面，检测器识别单击并通过 `onCalibrationTap` 上报。
5. ViewModel 将相邻两次单击配对，记录峰值幅度与间隔。
6. 收集满 10 组有效双击后：
   - 停止检测器。
   - 计算 sensitivity 与 range。
   - 写入 DataStore。
   - 写入 `back_tap_calibrating = false`。
7. 无障碍服务检测到标志变化，恢复动作执行（新参数已生效）。

## 校准算法

- **输入**：
  - 每次单击的正峰值幅度 `amplitudes: List<Float>`
  - 每组双击的两次单击时间差 `intervalsMs: List<Float>`
- **灵敏度**：
  - 取 `amplitudes` 的 20 分位数 `p20`（偏保守下限）。
  - 目标噪声容限 `targetNoise = p20 / 2`。
  - 映射到 sensitivity：`round((0.11 - targetNoise) * 100).coerceIn(1, 10)`。
- **检测范围**：
  - 取 `intervalsMs` 的 80 分位数 `p80`。
  - 目标最大间隔 `targetMaxMs = p80 + 100`。
  - 映射到 range：`round((targetMaxMs - 300) / 30).coerceIn(1, 10)`。
- 映射仅用于把连续目标值收敛到现有 1–10 滑档，核心阈值来自实测数据。

## UI

- 标题：自动校准
- 说明：请连续双击手机背面 10 次
- 进度：X / 10
- 检测到单次/双击时给出轻微进度反馈。
- 完成后显示计算结果：灵敏度 X，检测范围 Y。
- 提供“取消”按钮；超时或数据不足时显示“未检测到足够双击，请重试”。

## 错误处理

- **超时**：30 秒未收集满 10 组，提示重试，不保存。
- **异常值**：使用分位数剔除极端值，避免一次误触影响结果。
- **取消/关闭**：停止检测器并重置 `back_tap_calibrating`。
- **服务未启用**：校准仍可运行，但提示用户需开启背面双击开关才能生效。

## 测试

- 单元测试：验证 amplitude/interval 到 sensitivity/range 的映射与边界 clamp。
- 手动测试：在不同机型上双击背面，确认参数合理、不误触发。

## 依赖文件

- app/src/main/kotlin/com/byss/jh/screens/gesture/service/BackTapDetector.kt
- app/src/main/kotlin/com/byss/jh/screens/gesture/components/BackTapSettingsSection.kt
- app/src/main/kotlin/com/byss/jh/screens/gesture/GestureSettingsViewModel.kt
- app/src/main/kotlin/com/byss/jh/data/gesture/GestureSettings.kt
- app/src/main/kotlin/com/byss/jh/screens/gesture/service/EdgeGestureAccessibilityService.kt
- app/src/main/res/values/strings.xml
- app/src/main/res/values-en/strings.xml
