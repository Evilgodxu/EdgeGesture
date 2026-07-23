package com.edgegesture.evilgodxu.screens.launchblock.compact.rules_area.rules_title

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R

// 规则标题职责组件
@Composable
fun RulesTitle(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.launch_block_rules_title) + " (" +
            stringResource(R.string.launch_block_rules_count, count) + ")",
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}
