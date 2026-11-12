package com.edgegesture.evilgodxu.screens.gesture.component.status

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Dangerous
import androidx.compose.material.icons.outlined.SwipeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState
import com.edgegesture.evilgodxu.data.gesture.GestureStats
import com.edgegesture.evilgodxu.data.gesture.StatsPeriod
import com.edgegesture.evilgodxu.screens.gesture.component.dialog.StatsPeriodDialog
import com.edgegesture.evilgodxu.screens.gesture.component.dialog.labelRes

// 服务状态区块：运行状态、启停按钮与统计网格（长按切换统计周期）
@Composable
fun ServiceStatusSection(
    settings: GestureSettingsState,
    stats: GestureStats,
    statsPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit,
    onToggleService: (Boolean) -> Unit,
) {
    // 手势配置数仅在设置变化时重新统计，避免每次重组全量遍历
    val totalGestureCount = remember(settings) { countNonNoneGestures(settings) }
    var showStatsPeriodDialog by remember { mutableStateOf(false) }

    if (showStatsPeriodDialog) {
        StatsPeriodDialog(
            currentPeriod = statsPeriod,
            onDismiss = { showStatsPeriodDialog = false },
            onPeriodSelected = { period ->
                onPeriodSelected(period)
                showStatsPeriodDialog = false
            },
        )
    }

    Text(
        text = stringResource(R.string.gesture_service_title),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 10.dp),
    )

    ServiceStatusCard(
        enabled = settings.gestureEnabled,
        totalSegments = settings.leftSegmentCount + settings.rightSegmentCount + settings.bottomSegmentCount,
        totalGestures = totalGestureCount,
        stats = stats,
        statsPeriod = statsPeriod,
        onToggleService = onToggleService,
        onLongPressStats = { showStatsPeriodDialog = true },
    )
}

// 启动服务状态卡片：状态标题 + 圆点指示器 + 启停按钮 + 统计信息
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServiceStatusCard(
    enabled: Boolean,
    totalSegments: Int,
    totalGestures: Int,
    stats: GestureStats,
    statsPeriod: StatsPeriod,
    onToggleService: (Boolean) -> Unit,
    onLongPressStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 状态标题 + 圆点指示器
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (enabled) {
                            stringResource(R.string.gesture_service_running)
                        } else {
                            stringResource(R.string.gesture_service_stopped)
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp,
                        ),
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (enabled) Color(0xFF3FB950) else MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(50),
                            ),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.gesture_service_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onToggleService(!enabled) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = if (enabled) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = if (enabled) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (enabled) {
                            stringResource(R.string.gesture_service_stop)
                        } else {
                            stringResource(R.string.gesture_service_start)
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 统计信息网格（长按切换周期）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            // onClick 为空实现仅为支撑长按手势，单击本身无实际作用
                            onClick = {},
                            onLongClick = onLongPressStats,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatItem(
                            icon = {
                                Box(
                                    modifier = Modifier.size(20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp, 20.dp)
                                            .border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                                RoundedCornerShape(3.dp),
                                            ),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .size(2.dp, 10.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)),
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .size(2.dp, 10.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)),
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .size(8.dp, 2.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)),
                                        )
                                    }
                                }
                            },
                            value = totalSegments.toString(),
                            label = stringResource(R.string.edge_segment_count),
                        )

                        StatItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.SwipeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            value = totalGestures.toString(),
                            label = stringResource(R.string.gesture_count_label),
                        )

                        StatItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.DataUsage,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            value = stats.gestureCount.toString(),
                            label = stringResource(R.string.stats_gesture_count),
                        )

                        StatItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Dangerous,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            value = stats.blockCount.toString(),
                            label = stringResource(R.string.stats_block_count),
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.stats_period_hint) + stringResource(statsPeriod.labelRes()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: @Composable () -> Unit,
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// 统计所有非 NONE 的手势动作总数，仅统计已启用的分段
internal fun countNonNoneGestures(settings: GestureSettingsState): Int {
    var count = 0
    fun countActions(actions: List<GestureAction>) {
        count += actions.count { it != GestureAction.NONE }
    }

    val left = settings.leftEdge
    countActions(
        listOf(
            left.swipeRight, left.swipeRightLong, left.swipeUp, left.swipeUpLong,
            left.swipeDown, left.swipeDownLong, left.tap, left.doubleTap, left.longPress,
        ),
    )
    if (settings.leftSegmentCount >= 2) {
        val s = settings.leftEdgeSegment2
        countActions(
            listOf(
                s.swipeRight, s.swipeRightLong, s.swipeUp, s.swipeUpLong,
                s.swipeDown, s.swipeDownLong, s.tap, s.doubleTap, s.longPress,
            ),
        )
    }
    if (settings.leftSegmentCount >= 3) {
        val s = settings.leftEdgeSegment3
        countActions(
            listOf(
                s.swipeRight, s.swipeRightLong, s.swipeUp, s.swipeUpLong,
                s.swipeDown, s.swipeDownLong, s.tap, s.doubleTap, s.longPress,
            ),
        )
    }

    val right = settings.rightEdge
    countActions(
        listOf(
            right.swipeLeft, right.swipeLeftLong, right.swipeUp, right.swipeUpLong,
            right.swipeDown, right.swipeDownLong, right.tap, right.doubleTap, right.longPress,
        ),
    )
    if (settings.rightSegmentCount >= 2) {
        val s = settings.rightEdgeSegment2
        countActions(
            listOf(
                s.swipeLeft, s.swipeLeftLong, s.swipeUp, s.swipeUpLong,
                s.swipeDown, s.swipeDownLong, s.tap, s.doubleTap, s.longPress,
            ),
        )
    }
    if (settings.rightSegmentCount >= 3) {
        val s = settings.rightEdgeSegment3
        countActions(
            listOf(
                s.swipeLeft, s.swipeLeftLong, s.swipeUp, s.swipeUpLong,
                s.swipeDown, s.swipeDownLong, s.tap, s.doubleTap, s.longPress,
            ),
        )
    }

    val bottom = settings.bottomEdge
    countActions(
        listOf(
            bottom.swipeUp, bottom.swipeUpLong, bottom.swipeLeft, bottom.swipeLeftLong,
            bottom.swipeRight, bottom.swipeRightLong, bottom.tap, bottom.doubleTap, bottom.longPress,
        ),
    )
    if (settings.bottomSegmentCount >= 2) {
        val s = settings.bottomEdgeSegment2
        countActions(
            listOf(
                s.swipeUp, s.swipeUpLong, s.swipeLeft, s.swipeLeftLong,
                s.swipeRight, s.swipeRightLong, s.tap, s.doubleTap, s.longPress,
            ),
        )
    }
    if (settings.bottomSegmentCount >= 3) {
        val s = settings.bottomEdgeSegment3
        countActions(
            listOf(
                s.swipeUp, s.swipeUpLong, s.swipeLeft, s.swipeLeftLong,
                s.swipeRight, s.swipeRightLong, s.tap, s.doubleTap, s.longPress,
            ),
        )
    }
    return count
}
