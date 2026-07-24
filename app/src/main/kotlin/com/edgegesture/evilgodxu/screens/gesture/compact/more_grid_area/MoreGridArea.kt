package com.edgegesture.evilgodxu.screens.gesture.compact.more_grid_area

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.screens.gesture.compact.more_grid_area.more_grid_item.MoreGridItem

// 更多功能 Area — 2×2 网格
@Composable
fun MoreGridArea(
    onExpandPanel: (() -> Unit)? = null,
    onBlacklist: (() -> Unit)? = null,
    onLaunchBlock: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.more_section_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MoreGridItem(icon = { Icon(imageVector = Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) }, label = stringResource(R.string.more_expand_panel), onClick = onExpandPanel, modifier = Modifier.weight(1f))
            MoreGridItem(icon = { Icon(imageVector = Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) }, label = stringResource(R.string.more_blacklist), onClick = onBlacklist, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MoreGridItem(icon = { Icon(imageVector = Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) }, label = stringResource(R.string.more_launch_block), onClick = onLaunchBlock, modifier = Modifier.weight(1f))
            MoreGridItem(icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) }, label = stringResource(R.string.more_settings), onClick = onSettings, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
