package com.byss.jh.ui.settings.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byss.jh.R

/**
 * 捐赠对话框
 */
@Composable
fun DonateDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.settings_donate),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.settings_donate_thanks),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 支付宝
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                // 优先尝试使用支付宝 scheme 直接打开支付宝应用
                                val alipayIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = "alipays://platformapi/startapp?appId=20000067&url=https://qr.alipay.com/fkx19806mp7pzmxerluwrfd".toUri()
                                }
                                context.startActivity(alipayIntent)
                            } catch (e: Exception) {
                                // 如果支付宝应用未安装，降级到浏览器打开
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = "https://qr.alipay.com/fkx19806mp7pzmxerluwrfd".toUri()
                                    }
                                    context.startActivity(browserIntent)
                                } catch (_: Exception) {
                                }
                            }
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.settings_donate_alipay),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }


            }
        },
        confirmButton = {}
    )
}
