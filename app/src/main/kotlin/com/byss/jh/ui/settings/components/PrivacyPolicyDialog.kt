package com.byss.jh.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byss.jh.R

// 隐私政策对话框，在设置页面中展示完整的隐私政策内容
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.privacy_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 第1节：数据收集
                PrivacyPolicySection(
                    title = stringResource(R.string.privacy_section_1_title),
                    content = stringResource(R.string.privacy_section_1_content)
                )

                // 第2节：权限说明
                PrivacyPolicySection(
                    title = stringResource(R.string.privacy_section_2_title),
                    content = stringResource(R.string.privacy_section_2_content)
                )

                // 第3节：数据存储
                PrivacyPolicySection(
                    title = stringResource(R.string.privacy_section_3_title),
                    content = stringResource(R.string.privacy_section_3_content)
                )

                // 第4节：数据保留
                PrivacyPolicySection(
                    title = stringResource(R.string.privacy_section_4_title),
                    content = stringResource(R.string.privacy_section_4_content)
                )

                // 第5节：用户权利
                PrivacyPolicySection(
                    title = stringResource(R.string.privacy_section_5_title),
                    content = stringResource(R.string.privacy_section_5_content)
                )

                // 第6节：联系我们
                PrivacyPolicySection(
                    title = stringResource(R.string.privacy_section_6_title),
                    content = stringResource(R.string.privacy_section_6_content)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.privacy_dialog_confirm))
            }
        }
    )
}

// 隐私政策章节组件
@Composable
private fun PrivacyPolicySection(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
