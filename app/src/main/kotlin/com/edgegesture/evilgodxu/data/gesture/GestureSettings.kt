package com.edgegesture.evilgodxu.data.gesture

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// 从 Preferences 读取手势动作，统一默认值处理
private fun Preferences.readAction(key: Preferences.Key<String>, default: GestureAction): GestureAction =
    GestureAction.fromValue(this[key] ?: default.value)

// 扫描带目标包名后缀的存储键，构建「动作键名 → 包名」映射
private fun Preferences.readLaunchAppTargets(): Map<String, String> =
    asMap().entries.mapNotNull { (key, value) ->
        val name = key.name
        if (!name.endsWith(GestureSettingsKeys.LAUNCH_APP_TARGET_SUFFIX)) return@mapNotNull null
        (value as? String)?.takeIf { it.isNotBlank() }
            ?.let { name.removeSuffix(GestureSettingsKeys.LAUNCH_APP_TARGET_SUFFIX) to it }
    }.toMap()

// 扫描带小窗标记后缀的存储键，构建「动作键名 → 是否小窗启动」映射
private fun Preferences.readLaunchAppFreeform(): Map<String, Boolean> =
    asMap().entries.mapNotNull { (key, value) ->
        val name = key.name
        if (!name.endsWith(GestureSettingsKeys.LAUNCH_APP_FREEFORM_SUFFIX)) return@mapNotNull null
        (value as? Boolean)?.let { name.removeSuffix(GestureSettingsKeys.LAUNCH_APP_FREEFORM_SUFFIX) to it }
    }.toMap()

// 扫描带定时提醒分钟数后缀的存储键，构建「动作键名 → 分钟数」映射
private fun Preferences.readRemindMinutes(): Map<String, Int> =
    asMap().entries.mapNotNull { (key, value) ->
        val name = key.name
        if (!name.endsWith(GestureSettingsKeys.REMIND_MINUTES_SUFFIX)) return@mapNotNull null
        (value as? Int)?.let { name.removeSuffix(GestureSettingsKeys.REMIND_MINUTES_SUFFIX) to it }
    }.toMap()

// 从 Preferences 构建 GestureSettingsState，统一默认值处理
fun Preferences.toGestureSettingsState(): GestureSettingsState {
    return GestureSettingsState(
        gestureEnabled = this[GestureSettingsKeys.GESTURE_ENABLED] ?: false,
        hideOverlay = this[GestureSettingsKeys.HIDE_OVERLAY] ?: false,
        hideFromRecents = this[GestureSettingsKeys.HIDE_FROM_RECENTS] ?: false,
        avoidKeyboardOverlap = this[GestureSettingsKeys.AVOID_KEYBOARD_OVERLAP] ?: false,
        vibrationEnabled = this[GestureSettingsKeys.VIBRATION_ENABLED] ?: false,
        doubleSwipeEnabled = this[GestureSettingsKeys.DOUBLE_SWIPE_ENABLED] ?: false,
        // 背面双击
        backTapEnabled = this[GestureSettingsKeys.BACK_TAP_ENABLED] ?: false,
        backTapSensitivity = this[GestureSettingsKeys.BACK_TAP_SENSITIVITY] ?: 5,
        backTapRange = this[GestureSettingsKeys.BACK_TAP_RANGE] ?: 5,
        backTapAction = readAction(GestureSettingsKeys.BACK_TAP_ACTION, GestureAction.HOME),
        backTapMode = BackTapMode.fromValue(this[GestureSettingsKeys.BACK_TAP_MODE] ?: BackTapMode.ALWAYS.value),
        backTapPauseOnCharging = this[GestureSettingsKeys.BACK_TAP_PAUSE_ON_CHARGING] ?: false,
        // 左侧边缘尺寸
        leftEdgeWidth = this[GestureSettingsKeys.LEFT_EDGE_WIDTH] ?: 20,
        leftEdgeHeightPercent = this[GestureSettingsKeys.LEFT_EDGE_HEIGHT_PERCENT] ?: 60,
        leftEdgePositionPercent = this[GestureSettingsKeys.LEFT_EDGE_POSITION_PERCENT] ?: 90,
        leftSegmentCount = this[GestureSettingsKeys.LEFT_SEGMENT_COUNT] ?: 1,
        // 右侧边缘尺寸
        rightEdgeWidth = this[GestureSettingsKeys.RIGHT_EDGE_WIDTH] ?: 20,
        rightEdgeHeightPercent = this[GestureSettingsKeys.RIGHT_EDGE_HEIGHT_PERCENT] ?: 60,
        rightEdgePositionPercent = this[GestureSettingsKeys.RIGHT_EDGE_POSITION_PERCENT] ?: 90,
        rightSegmentCount = this[GestureSettingsKeys.RIGHT_SEGMENT_COUNT] ?: 1,
        // 底部边缘尺寸
        bottomEdgeHeight = this[GestureSettingsKeys.BOTTOM_EDGE_HEIGHT] ?: 20,
        bottomEdgeWidthPercent = this[GestureSettingsKeys.BOTTOM_EDGE_WIDTH_PERCENT] ?: 80,
        bottomSegmentCount = this[GestureSettingsKeys.BOTTOM_SEGMENT_COUNT] ?: 1,
        // 手势配置（第1段）
        leftEdge = LeftEdgeConfig(
            swipeRight = readAction(GestureSettingsKeys.LEFT_SWIPE_RIGHT, GestureAction.BACK),
            swipeRightLong = readAction(GestureSettingsKeys.LEFT_SWIPE_RIGHT_LONG, GestureAction.LAST_APP),
            swipeUp = readAction(GestureSettingsKeys.LEFT_SWIPE_UP, GestureAction.PREVIOUS_TRACK),
            swipeUpLong = readAction(GestureSettingsKeys.LEFT_SWIPE_UP_LONG, GestureAction.POWER_MENU),
            swipeDown = readAction(GestureSettingsKeys.LEFT_SWIPE_DOWN, GestureAction.SCREENSHOT),
            swipeDownLong = readAction(GestureSettingsKeys.LEFT_SWIPE_DOWN_LONG, GestureAction.LOCK_SCREEN),
            tap = readAction(GestureSettingsKeys.LEFT_TAP, GestureAction.NONE),
            doubleTap = readAction(GestureSettingsKeys.LEFT_DOUBLE_TAP, GestureAction.NONE),
            longPress = readAction(GestureSettingsKeys.LEFT_LONG_PRESS, GestureAction.NONE)
        ),
        rightEdge = RightEdgeConfig(
            swipeLeft = readAction(GestureSettingsKeys.RIGHT_SWIPE_LEFT, GestureAction.BACK),
            swipeLeftLong = readAction(GestureSettingsKeys.RIGHT_SWIPE_LEFT_LONG, GestureAction.LAST_APP),
            swipeUp = readAction(GestureSettingsKeys.RIGHT_SWIPE_UP, GestureAction.NEXT_TRACK),
            swipeUpLong = readAction(GestureSettingsKeys.RIGHT_SWIPE_UP_LONG, GestureAction.FLASHLIGHT),
            swipeDown = readAction(GestureSettingsKeys.RIGHT_SWIPE_DOWN, GestureAction.VOICE_ASSISTANT),
            swipeDownLong = readAction(GestureSettingsKeys.RIGHT_SWIPE_DOWN_LONG, GestureAction.EXPAND_PANEL),
            tap = readAction(GestureSettingsKeys.RIGHT_TAP, GestureAction.NONE),
            doubleTap = readAction(GestureSettingsKeys.RIGHT_DOUBLE_TAP, GestureAction.NONE),
            longPress = readAction(GestureSettingsKeys.RIGHT_LONG_PRESS, GestureAction.NONE)
        ),
        bottomEdge = BottomEdgeConfig(
            swipeUp = readAction(GestureSettingsKeys.BOTTOM_SWIPE_UP, GestureAction.HOME),
            swipeUpLong = readAction(GestureSettingsKeys.BOTTOM_SWIPE_UP_LONG, GestureAction.RECENT),
            swipeLeft = readAction(GestureSettingsKeys.BOTTOM_SWIPE_LEFT, GestureAction.LAST_APP),
            swipeLeftLong = readAction(GestureSettingsKeys.BOTTOM_SWIPE_LEFT_LONG, GestureAction.NONE),
            swipeRight = readAction(GestureSettingsKeys.BOTTOM_SWIPE_RIGHT, GestureAction.LAST_APP),
            swipeRightLong = readAction(GestureSettingsKeys.BOTTOM_SWIPE_RIGHT_LONG, GestureAction.NONE),
            tap = readAction(GestureSettingsKeys.BOTTOM_TAP, GestureAction.NONE),
            doubleTap = readAction(GestureSettingsKeys.BOTTOM_DOUBLE_TAP, GestureAction.NONE),
            longPress = readAction(GestureSettingsKeys.BOTTOM_LONG_PRESS, GestureAction.NONE)
        ),
        // 手势配置（第2段）
        leftEdgeSegment2 = LeftEdgeConfig(
            swipeRight = readAction(GestureSettingsKeys.LEFT_2_SWIPE_RIGHT, GestureAction.NONE),
            swipeRightLong = readAction(GestureSettingsKeys.LEFT_2_SWIPE_RIGHT_LONG, GestureAction.NONE),
            swipeUp = readAction(GestureSettingsKeys.LEFT_2_SWIPE_UP, GestureAction.NONE),
            swipeUpLong = readAction(GestureSettingsKeys.LEFT_2_SWIPE_UP_LONG, GestureAction.NONE),
            swipeDown = readAction(GestureSettingsKeys.LEFT_2_SWIPE_DOWN, GestureAction.NONE),
            swipeDownLong = readAction(GestureSettingsKeys.LEFT_2_SWIPE_DOWN_LONG, GestureAction.NONE),
            tap = readAction(GestureSettingsKeys.LEFT_2_TAP, GestureAction.NONE),
            doubleTap = readAction(GestureSettingsKeys.LEFT_2_DOUBLE_TAP, GestureAction.NONE),
            longPress = readAction(GestureSettingsKeys.LEFT_2_LONG_PRESS, GestureAction.NONE)
        ),
        rightEdgeSegment2 = RightEdgeConfig(
            swipeLeft = readAction(GestureSettingsKeys.RIGHT_2_SWIPE_LEFT, GestureAction.NONE),
            swipeLeftLong = readAction(GestureSettingsKeys.RIGHT_2_SWIPE_LEFT_LONG, GestureAction.NONE),
            swipeUp = readAction(GestureSettingsKeys.RIGHT_2_SWIPE_UP, GestureAction.NONE),
            swipeUpLong = readAction(GestureSettingsKeys.RIGHT_2_SWIPE_UP_LONG, GestureAction.NONE),
            swipeDown = readAction(GestureSettingsKeys.RIGHT_2_SWIPE_DOWN, GestureAction.NONE),
            swipeDownLong = readAction(GestureSettingsKeys.RIGHT_2_SWIPE_DOWN_LONG, GestureAction.NONE),
            tap = readAction(GestureSettingsKeys.RIGHT_2_TAP, GestureAction.NONE),
            doubleTap = readAction(GestureSettingsKeys.RIGHT_2_DOUBLE_TAP, GestureAction.NONE),
            longPress = readAction(GestureSettingsKeys.RIGHT_2_LONG_PRESS, GestureAction.NONE)
        ),
        bottomEdgeSegment2 = BottomEdgeConfig(
            swipeUp = readAction(GestureSettingsKeys.BOTTOM_2_SWIPE_UP, GestureAction.NONE),
            swipeUpLong = readAction(GestureSettingsKeys.BOTTOM_2_SWIPE_UP_LONG, GestureAction.NONE),
            swipeLeft = readAction(GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT, GestureAction.NONE),
            swipeLeftLong = readAction(GestureSettingsKeys.BOTTOM_2_SWIPE_LEFT_LONG, GestureAction.NONE),
            swipeRight = readAction(GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT, GestureAction.NONE),
            swipeRightLong = readAction(GestureSettingsKeys.BOTTOM_2_SWIPE_RIGHT_LONG, GestureAction.NONE),
            tap = readAction(GestureSettingsKeys.BOTTOM_2_TAP, GestureAction.NONE),
            doubleTap = readAction(GestureSettingsKeys.BOTTOM_2_DOUBLE_TAP, GestureAction.NONE),
            longPress = readAction(GestureSettingsKeys.BOTTOM_2_LONG_PRESS, GestureAction.NONE)
        ),
        // 手势配置（第3段）
        leftEdgeSegment3 = LeftEdgeConfig(
            swipeRight = readAction(GestureSettingsKeys.LEFT_3_SWIPE_RIGHT, GestureAction.NONE),
            swipeRightLong = readAction(GestureSettingsKeys.LEFT_3_SWIPE_RIGHT_LONG, GestureAction.NONE),
            swipeUp = readAction(GestureSettingsKeys.LEFT_3_SWIPE_UP, GestureAction.NONE),
            swipeUpLong = readAction(GestureSettingsKeys.LEFT_3_SWIPE_UP_LONG, GestureAction.NONE),
            swipeDown = readAction(GestureSettingsKeys.LEFT_3_SWIPE_DOWN, GestureAction.NONE),
            swipeDownLong = readAction(GestureSettingsKeys.LEFT_3_SWIPE_DOWN_LONG, GestureAction.NONE),
            tap = readAction(GestureSettingsKeys.LEFT_3_TAP, GestureAction.NONE),
            doubleTap = readAction(GestureSettingsKeys.LEFT_3_DOUBLE_TAP, GestureAction.NONE),
            longPress = readAction(GestureSettingsKeys.LEFT_3_LONG_PRESS, GestureAction.NONE)
        ),
        rightEdgeSegment3 = RightEdgeConfig(
            swipeLeft = readAction(GestureSettingsKeys.RIGHT_3_SWIPE_LEFT, GestureAction.NONE),
            swipeLeftLong = readAction(GestureSettingsKeys.RIGHT_3_SWIPE_LEFT_LONG, GestureAction.NONE),
            swipeUp = readAction(GestureSettingsKeys.RIGHT_3_SWIPE_UP, GestureAction.NONE),
            swipeUpLong = readAction(GestureSettingsKeys.RIGHT_3_SWIPE_UP_LONG, GestureAction.NONE),
            swipeDown = readAction(GestureSettingsKeys.RIGHT_3_SWIPE_DOWN, GestureAction.NONE),
            swipeDownLong = readAction(GestureSettingsKeys.RIGHT_3_SWIPE_DOWN_LONG, GestureAction.NONE),
            tap = readAction(GestureSettingsKeys.RIGHT_3_TAP, GestureAction.NONE),
            doubleTap = readAction(GestureSettingsKeys.RIGHT_3_DOUBLE_TAP, GestureAction.NONE),
            longPress = readAction(GestureSettingsKeys.RIGHT_3_LONG_PRESS, GestureAction.NONE)
        ),
        bottomEdgeSegment3 = BottomEdgeConfig(
            swipeUp = readAction(GestureSettingsKeys.BOTTOM_3_SWIPE_UP, GestureAction.NONE),
            swipeUpLong = readAction(GestureSettingsKeys.BOTTOM_3_SWIPE_UP_LONG, GestureAction.NONE),
            swipeLeft = readAction(GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT, GestureAction.NONE),
            swipeLeftLong = readAction(GestureSettingsKeys.BOTTOM_3_SWIPE_LEFT_LONG, GestureAction.NONE),
            swipeRight = readAction(GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT, GestureAction.NONE),
            swipeRightLong = readAction(GestureSettingsKeys.BOTTOM_3_SWIPE_RIGHT_LONG, GestureAction.NONE),
            tap = readAction(GestureSettingsKeys.BOTTOM_3_TAP, GestureAction.NONE),
            doubleTap = readAction(GestureSettingsKeys.BOTTOM_3_DOUBLE_TAP, GestureAction.NONE),
            longPress = readAction(GestureSettingsKeys.BOTTOM_3_LONG_PRESS, GestureAction.NONE)
        ),
        launchAppTargets = readLaunchAppTargets(),
        launchAppFreeform = readLaunchAppFreeform(),
        remindMinutes = readRemindMinutes()
    )
}

fun Context.gestureSettingsFlow(): Flow<GestureSettingsState> = gestureDataStore.data.map { prefs ->
    prefs.toGestureSettingsState()
}

suspend fun Context.saveGestureEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.GESTURE_ENABLED] = enabled
    }
}

suspend fun Context.saveHideOverlay(hide: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.HIDE_OVERLAY] = hide
    }
}

suspend fun Context.saveHideFromRecents(hide: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.HIDE_FROM_RECENTS] = hide
    }
}

suspend fun Context.saveAvoidKeyboardOverlap(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.AVOID_KEYBOARD_OVERLAP] = enabled
    }
}

// 左侧边缘尺寸设置
suspend fun Context.saveLeftEdgeWidth(width: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.LEFT_EDGE_WIDTH] = width.coerceIn(10, 50)
    }
}

suspend fun Context.saveLeftEdgeHeightPercent(percent: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.LEFT_EDGE_HEIGHT_PERCENT] = percent.coerceIn(20, 100)
    }
}

suspend fun Context.saveLeftEdgePositionPercent(percent: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.LEFT_EDGE_POSITION_PERCENT] = percent.coerceIn(0, 100)
    }
}

suspend fun Context.saveLeftSegmentCount(count: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.LEFT_SEGMENT_COUNT] = count.coerceIn(1, 3)
    }
}

// 右侧边缘尺寸设置
suspend fun Context.saveRightEdgeWidth(width: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.RIGHT_EDGE_WIDTH] = width.coerceIn(10, 50)
    }
}

suspend fun Context.saveRightEdgeHeightPercent(percent: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.RIGHT_EDGE_HEIGHT_PERCENT] = percent.coerceIn(20, 100)
    }
}

suspend fun Context.saveRightEdgePositionPercent(percent: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.RIGHT_EDGE_POSITION_PERCENT] = percent.coerceIn(0, 100)
    }
}

suspend fun Context.saveRightSegmentCount(count: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.RIGHT_SEGMENT_COUNT] = count.coerceIn(1, 3)
    }
}

// 底部边缘尺寸设置
suspend fun Context.saveBottomEdgeHeight(height: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BOTTOM_EDGE_HEIGHT] = height.coerceIn(10, 50)
    }
}

suspend fun Context.saveBottomEdgeWidthPercent(percent: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BOTTOM_EDGE_WIDTH_PERCENT] = percent.coerceIn(20, 100)
    }
}

suspend fun Context.saveBottomSegmentCount(count: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BOTTOM_SEGMENT_COUNT] = count.coerceIn(1, 3)
    }
}

// 保存单个手势动作
suspend fun Context.saveEdgeGesture(key: Preferences.Key<String>, action: GestureAction) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[key] = action.value
    }
}

// 保存启动应用动作绑定的目标包名，传 null 或空白清除绑定
suspend fun Context.saveLaunchAppTarget(actionKey: Preferences.Key<String>, packageName: String?) = withContext(Dispatchers.IO) {
    val targetKey = stringPreferencesKey(actionKey.name + GestureSettingsKeys.LAUNCH_APP_TARGET_SUFFIX)
    gestureDataStore.edit { prefs ->
        if (packageName.isNullOrBlank()) {
            prefs.minusAssign(targetKey)
        } else {
            prefs[targetKey] = packageName
        }
    }
}

// 保存启动应用动作的小窗启动标记，关闭时清除存储避免冗余
suspend fun Context.saveLaunchAppFreeform(actionKey: Preferences.Key<String>, enabled: Boolean) = withContext(Dispatchers.IO) {
    val flagKey = booleanPreferencesKey(actionKey.name + GestureSettingsKeys.LAUNCH_APP_FREEFORM_SUFFIX)
    gestureDataStore.edit { prefs ->
        if (enabled) {
            prefs[flagKey] = true
        } else {
            prefs.minusAssign(flagKey)
        }
    }
}

// 保存定时提醒动作的分钟数
suspend fun Context.saveRemindMinutes(actionKey: Preferences.Key<String>, minutes: Int) = withContext(Dispatchers.IO) {
    val minutesKey = intPreferencesKey(actionKey.name + GestureSettingsKeys.REMIND_MINUTES_SUFFIX)
    gestureDataStore.edit { prefs ->
        prefs[minutesKey] = minutes
    }
}

fun Context.appSwitchBlacklistFlow(): Flow<Set<String>> = gestureDataStore.data.map { prefs ->
    prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] ?: emptySet()
}

suspend fun Context.saveAppSwitchBlacklist(blacklist: Set<String>) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] = blacklist
    }
}

suspend fun Context.addToAppSwitchBlacklist(packageNames: Set<String>) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        val current = prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] ?: emptySet()
        prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] = current + packageNames
    }
}

suspend fun Context.removeFromAppSwitchBlacklist(packageNames: Set<String>) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        val current = prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] ?: emptySet()
        prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] = current - packageNames
    }
}

// 初始化黑名单，将所有系统应用（包括无入口的）和本应用加入黑名单
// 有 QUERY_ALL_PACKAGES 权限时通过 getSystemAppPackages() 获取全部系统应用；
// 无权限或调用失败时使用已扫描的可启动系统应用兜底。
//
// 采用"合并"策略而非"回退"策略：
// - launcherApps 作为基础集合（来自已扫描的系统应用，总是由 AppRepository 传入）
// - getSystemAppPackages() 补充获取无入口的系统应用（可能因权限或瞬态异常失败）
// - 两者取并集，确保黑名单始终包含所有已知系统应用
suspend fun Context.initBlacklistIfNeeded(launcherApps: Set<String>? = null) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        // 自愈检测：如果已初始化但黑名单中只有本应用（竞态导致的损坏状态），则重新初始化
        val currentBlacklist = prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] ?: emptySet()
        val onlySelf = currentBlacklist.size == 1 && currentBlacklist.contains(packageName)

        if (prefs[GestureSettingsKeys.BLACKLIST_INITIALIZED] != true || onlySelf) {
            // 以 launcherApps 为基础（来自已扫描的应用列表，最可靠）
            val apps = launcherApps?.toMutableSet() ?: mutableSetOf()
            // 补充获取系统应用（含无入口的），可能因权限不足或瞬态异常失败
            runCatching { getSystemAppPackages() }.getOrNull()?.let { systemApps ->
                apps.addAll(systemApps)
            }
            // 确保本应用始终在黑名单中
            apps.add(packageName)
            prefs[GestureSettingsKeys.APP_SWITCH_BLACKLIST] = apps
            prefs[GestureSettingsKeys.BLACKLIST_INITIALIZED] = true
        }
    }
}

// 重置黑名单初始化标志，用于用户重新授予 QUERY_ALL_PACKAGES 后重新初始化
suspend fun Context.resetBlacklistInitialized() = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs.remove(GestureSettingsKeys.BLACKLIST_INITIALIZED)
    }
}

suspend fun Context.saveVibrationEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.VIBRATION_ENABLED] = enabled
    }
}

suspend fun Context.saveDoubleSwipeEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.DOUBLE_SWIPE_ENABLED] = enabled
    }
}

// 背面双击设置
suspend fun Context.saveBackTapEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_ENABLED] = enabled
    }
}

suspend fun Context.saveBackTapSensitivity(sensitivity: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_SENSITIVITY] = sensitivity.coerceIn(1, 10)
    }
}

suspend fun Context.saveBackTapRange(range: Int) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_RANGE] = range.coerceIn(1, 10)
    }
}

suspend fun Context.saveBackTapAction(action: GestureAction) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_ACTION] = action.value
    }
}

suspend fun Context.saveBackTapMode(mode: BackTapMode) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_MODE] = mode.value
    }
}

suspend fun Context.saveBackTapPauseOnCharging(pause: Boolean) = withContext(Dispatchers.IO) {
    gestureDataStore.edit { prefs ->
        prefs[GestureSettingsKeys.BACK_TAP_PAUSE_ON_CHARGING] = pause
    }
}

private fun Context.getSystemAppPackages(): Set<String> {
    val pm = packageManager
    val systemApps = mutableSetOf<String>()
    val installedApps = pm.getInstalledApplications(0)
    for (app in installedApps) {
        if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
            app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) {
            systemApps.add(app.packageName)
        }
    }
    return systemApps
}
