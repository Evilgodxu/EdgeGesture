package com.edgegesture.evilgodxu.screens.launchblock.compact.rules_area

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.screens.launchblock.compact.rules_area.add_rule_button.AddRuleButton
import com.edgegesture.evilgodxu.screens.launchblock.compact.rules_area.empty_rules_state.EmptyRulesState
import com.edgegesture.evilgodxu.screens.launchblock.compact.rules_area.rule_card.RuleCard
import com.edgegesture.evilgodxu.screens.launchblock.compact.rules_area.rules_title.RulesTitle

// 拦截规则 Area — 仅负责组件排列
@Composable
fun RulesArea(
    rules: List<LaunchBlockRule>,
    onAddRule: () -> Unit,
    onEditRule: (LaunchBlockRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(20.dp))

        RulesTitle(count = rules.size)

        AddRuleButton(onClick = onAddRule)

        Spacer(modifier = Modifier.height(12.dp))

        if (rules.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rules.forEach { rule ->
                        RuleCard(rule = rule, onEdit = { onEditRule(rule) }, onDelete = { onDeleteRule(rule.id) })
                    }
                }
            }
        } else {
            EmptyRulesState()
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
