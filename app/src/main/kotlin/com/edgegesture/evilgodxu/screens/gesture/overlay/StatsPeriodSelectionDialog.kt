package com.edgegesture.evilgodxu.screens.gesture.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.gesture.StatsPeriod

// 统计周期选择对话框 — 模态覆盖层
@Composable
fun StatsPeriodSelectionDialog(
    currentPeriod: StatsPeriod,
    onDismiss: () -> Unit,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.stats_period_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatsPeriod.entries.forEach { period ->
                    val isSelected = currentPeriod == period
                    Text(
                        text = stringResource(
                            when (period) {
                                StatsPeriod.DAY_1 -> R.string.stats_period_1d
                                StatsPeriod.DAY_7 -> R.string.stats_period_7d
                                StatsPeriod.DAY_30 -> R.string.stats_period_30d
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected && isDarkTheme -> MaterialTheme.colorScheme.primaryContainer
                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable { onPeriodSelected(period) }
                            .padding(vertical = 14.dp),
                        textAlign = TextAlign.Center,
                        color = when {
                            isSelected && isDarkTheme -> MaterialTheme.colorScheme.onPrimaryContainer
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        },
        confirmButton = {}
    )
}
