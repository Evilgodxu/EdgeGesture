package com.edgegesture.evilgodxu.screens.gesture.component.action

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.EdgePosition
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsKeys
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.screens.gesture.EdgeType
import com.edgegesture.evilgodxu.screens.gesture.component.action.getActionDisplayName

// 点击类手势在配置列表中的起始槽位（单击/双击/长按）
private const val TAP_SLOT_START = 6

// 手势行图标着色：长按+滑动为紫色，点击类为青色
private val SwipeLongPressIconColor = Color(0xFFa371f7)
private val TapIconColor = Color(0xFF00BFA5)

// 手势操作区块：分段标签 + 九宫动作列表，点击某行交由宿主打开动作选择
@Composable
fun GestureActionsSection(
    edgeType: EdgeType,
    settings: GestureSettingsState?,
    selectedSegment: Int,
    maxSegments: Int,
    appNameByPackage: Map<String, String>,
    onSegmentSelected: (Int) -> Unit,
    onActionClick: (Preferences.Key<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.gesture_operation_section_title),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        )

        if (maxSegments > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (1..maxSegments).forEach { segment ->
                    val isSelected = selectedSegment == segment
                    FilledTonalButton(
                        onClick = { onSegmentSelected(segment) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.gesture_segment_label, segment),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val gestureLabels = gestureLabels(edgeType)
                val gestures = gestureActions(edgeType, selectedSegment, settings)
                gestures.forEachIndexed { index, (action, key) ->
                    // 槽位 6..8 为点击类手势，无方向图标，用独立着色区分
                    val isTapGesture = index >= TAP_SLOT_START
                    GestureActionRow(
                        label = gestureLabels[index],
                        actionName = actionDisplayName(action, key, settings, appNameByPackage),
                        icon = if (isTapGesture) Icons.Outlined.TouchApp else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        iconRotation = if (isTapGesture) 0f else gestureIconRotation(edgeType, index),
                        iconColor = when {
                            isTapGesture -> TapIconColor
                            index % 2 == 1 -> SwipeLongPressIconColor
                            else -> MaterialTheme.colorScheme.primary
                        },
                        onClick = { onActionClick(key) },
                    )
                    if (index < gestures.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// 对话框回显：按存储键定位当前动作
fun gestureActionForDialog(
    edgeType: EdgeType,
    segment: Int,
    settings: GestureSettingsState?,
    key: Preferences.Key<String>?,
): GestureAction = gestureActions(edgeType, segment, settings)
    .firstOrNull { it.second == key }
    ?.first
    ?: GestureAction.NONE

@Composable
private fun GestureActionRow(
    label: String,
    actionName: String,
    icon: ImageVector,
    iconRotation: Float,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 左侧手势图标（方向箭头按方向旋转，点击类为触摸图标）
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(iconRotation),
                tint = iconColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = actionName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// 解析动作显示名：启动应用动作优先显示绑定的应用名，未绑定时回退动作名
@Composable
private fun actionDisplayName(
    action: GestureAction,
    key: Preferences.Key<String>,
    settings: GestureSettingsState?,
    appNameByPackage: Map<String, String>,
): String {
    val appName = if (action == GestureAction.LAUNCH_APP) {
        settings?.launchAppTargets?.get(key.name)?.let { appNameByPackage[it] }
    } else {
        null
    }
    return appName ?: getActionDisplayName(action)
}

// 根据边缘类型与行序号确定方向图标旋转角度，与显示文案解耦
private fun gestureIconRotation(edgeType: EdgeType, index: Int): Float {
    return when (edgeType) {
        EdgeType.LEFT -> when (index) {
            0, 1 -> 0f      // 右滑
            2, 3 -> -90f    // 上滑
            else -> 90f     // 下滑
        }

        EdgeType.RIGHT -> when (index) {
            0, 1 -> 180f    // 左滑
            2, 3 -> -90f    // 上滑
            else -> 90f     // 下滑
        }

        EdgeType.BOTTOM -> when (index) {
            0, 1 -> -90f    // 上滑
            2, 3 -> 180f    // 左滑
            else -> 0f      // 右滑
        }
    }
}

// 获取指定边缘和段的动作列表
private fun gestureActions(
    edgeType: EdgeType,
    segment: Int,
    settings: GestureSettingsState?,
): List<Pair<GestureAction, Preferences.Key<String>>> {
    if (settings == null) return emptyList()

    return when (edgeType) {
        EdgeType.LEFT -> {
            val edge = when (segment) {
                2 -> settings.leftEdgeSegment2
                3 -> settings.leftEdgeSegment3
                else -> settings.leftEdge
            }
            val keys = gestureKeys(EdgePosition.LEFT, segment)
            listOf(
                edge.swipeRight to keys[0],
                edge.swipeRightLong to keys[1],
                edge.swipeUp to keys[2],
                edge.swipeUpLong to keys[3],
                edge.swipeDown to keys[4],
                edge.swipeDownLong to keys[5],
                edge.tap to keys[6],
                edge.doubleTap to keys[7],
                edge.longPress to keys[8],
            )
        }

        EdgeType.RIGHT -> {
            val edge = when (segment) {
                2 -> settings.rightEdgeSegment2
                3 -> settings.rightEdgeSegment3
                else -> settings.rightEdge
            }
            val keys = gestureKeys(EdgePosition.RIGHT, segment)
            listOf(
                edge.swipeLeft to keys[0],
                edge.swipeLeftLong to keys[1],
                edge.swipeUp to keys[2],
                edge.swipeUpLong to keys[3],
                edge.swipeDown to keys[4],
                edge.swipeDownLong to keys[5],
                edge.tap to keys[6],
                edge.doubleTap to keys[7],
                edge.longPress to keys[8],
            )
        }

        EdgeType.BOTTOM -> {
            val edge = when (segment) {
                2 -> settings.bottomEdgeSegment2
                3 -> settings.bottomEdgeSegment3
                else -> settings.bottomEdge
            }
            val keys = gestureKeys(EdgePosition.BOTTOM, segment)
            listOf(
                edge.swipeUp to keys[0],
                edge.swipeUpLong to keys[1],
                edge.swipeLeft to keys[2],
                edge.swipeLeftLong to keys[3],
                edge.swipeRight to keys[4],
                edge.swipeRightLong to keys[5],
                edge.tap to keys[6],
                edge.doubleTap to keys[7],
                edge.longPress to keys[8],
            )
        }
    }
}

// 取指定边缘与分段的 9 个手势动作存储键，顺序与列表展示一致
private fun gestureKeys(position: EdgePosition, segment: Int): List<Preferences.Key<String>> =
    (0..8).map { slot -> GestureSettingsKeys.keyFor(position, segment - 1, slot) }

@Composable
private fun gestureLabels(edgeType: EdgeType): List<String> {
    val swipeLabels = when (edgeType) {
        EdgeType.LEFT -> listOf(
            stringResource(R.string.gesture_swipe_right),
            stringResource(R.string.gesture_swipe_right_long),
            stringResource(R.string.gesture_swipe_up),
            stringResource(R.string.gesture_swipe_up_long),
            stringResource(R.string.gesture_swipe_down),
            stringResource(R.string.gesture_swipe_down_long),
        )

        EdgeType.RIGHT -> listOf(
            stringResource(R.string.gesture_swipe_left),
            stringResource(R.string.gesture_swipe_left_long),
            stringResource(R.string.gesture_swipe_up),
            stringResource(R.string.gesture_swipe_up_long),
            stringResource(R.string.gesture_swipe_down),
            stringResource(R.string.gesture_swipe_down_long),
        )

        EdgeType.BOTTOM -> listOf(
            stringResource(R.string.gesture_swipe_up),
            stringResource(R.string.gesture_swipe_up_long),
            stringResource(R.string.gesture_swipe_left),
            stringResource(R.string.gesture_swipe_left_long),
            stringResource(R.string.gesture_swipe_right),
            stringResource(R.string.gesture_swipe_right_long),
        )
    }
    // 点击类手势标签与方向无关，各边缘一致
    return swipeLabels + listOf(
        stringResource(R.string.gesture_tap),
        stringResource(R.string.gesture_double_tap),
        stringResource(R.string.gesture_long_press),
    )
}
