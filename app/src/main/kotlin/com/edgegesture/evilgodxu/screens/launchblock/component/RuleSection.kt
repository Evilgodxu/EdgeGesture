package com.edgegesture.evilgodxu.screens.launchblock.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule

// 拦截规则区块：标题（含条数）、新增入口与规则列表 / 空状态
@Composable
fun RuleSection(
    rules: List<LaunchBlockRule>,
    appNames: Map<String, String>,
    onResolveAppName: (String) -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (LaunchBlockRule) -> Unit,
    onDeleteRule: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.launch_block_rules_title) + " (" +
            pluralStringResource(R.plurals.launch_block_rules_count, rules.size, rules.size) + ")",
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
    )

    Button(
        onClick = onAddRule,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = stringResource(R.string.launch_block_add_rule),
            fontWeight = FontWeight.SemiBold,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (rules.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Text(
                text = stringResource(R.string.launch_block_no_rules),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rules.forEach { rule ->
                RuleCard(
                    rule = rule,
                    appNames = appNames,
                    onResolveAppName = onResolveAppName,
                    onEdit = { onEditRule(rule) },
                    onDelete = { onDeleteRule(rule.id) },
                )
            }
        }
    }
}
