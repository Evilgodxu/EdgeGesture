package com.edgegesture.evilgodxu.screens.gesture.compact.status_area.service_status_card

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.materialsymbols.outlined.R.drawable as MsRDrawable
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.GestureStats
import com.edgegesture.evilgodxu.data.gesture.StatsPeriod
import com.edgegesture.evilgodxu.screens.gesture.compact.status_area.service_status_card.gesture_wave_icon.GestureWaveIcon
import com.edgegesture.evilgodxu.screens.gesture.compact.status_area.service_status_card.stat_item.StatItem

// 服务状态卡片职责组件
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServiceStatusCard(
    enabled: Boolean,
    isAccessibilityEnabled: Boolean,
    totalSegments: Int,
    totalGestures: Int,
    stats: GestureStats,
    statsPeriod: StatsPeriod,
    onToggleService: (Boolean) -> Unit,
    onLongPressStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Card(
            modifier = modifier.fillMaxWidth(0.95f).padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (enabled) stringResource(R.string.gesture_service_running) else stringResource(R.string.gesture_service_stopped),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light, letterSpacing = 2.sp),
                        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier.size(10.dp).background(
                            color = if (enabled) Color(0xFF3FB950) else MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(50)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.gesture_service_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onToggleService(!enabled) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = if (enabled) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = if (enabled) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (enabled) stringResource(R.string.gesture_service_stop) else stringResource(R.string.gesture_service_start), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongPressStats),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(
                            icon = {
                                Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                                    Box(modifier = Modifier.size(16.dp, 20.dp).border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(3.dp))) {
                                        Box(modifier = Modifier.align(Alignment.CenterStart).size(2.dp, 10.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)))
                                        Box(modifier = Modifier.align(Alignment.CenterEnd).size(2.dp, 10.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)))
                                        Box(modifier = Modifier.align(Alignment.BottomCenter).size(8.dp, 2.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)))
                                    }
                                }
                            },
                            value = totalSegments.toString(), label = stringResource(R.string.edge_segment_count)
                        )
                        StatItem(
                            icon = { Icon(painter = painterResource(MsRDrawable.materialsymbols_ic_mobile_hand_outlined), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            value = totalGestures.toString(), label = stringResource(R.string.gesture_count_label)
                        )
                        StatItem(
                            icon = { Icon(painter = painterResource(MsRDrawable.materialsymbols_ic_bar_chart_outlined), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
                            value = stats.gestureCount.toString(), label = stringResource(R.string.stats_gesture_count)
                        )
                        StatItem(
                            icon = { Icon(painter = painterResource(MsRDrawable.materialsymbols_ic_dangerous_outlined), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error) },
                            value = stats.blockCount.toString(), label = stringResource(R.string.stats_block_count)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.stats_period_hint) + stringResource(
                            when (statsPeriod) {
                                StatsPeriod.DAY_1 -> R.string.stats_period_1d
                                StatsPeriod.DAY_7 -> R.string.stats_period_7d
                                StatsPeriod.DAY_30 -> R.string.stats_period_30d
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
