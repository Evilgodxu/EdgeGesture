package com.edgegesture.evilgodxu.screens.settings.component.info

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.edgegesture.evilgodxu.R
import com.edgegesture.evilgodxu.log.CrashLogManager
import com.edgegesture.evilgodxu.screens.settings.component.SettingsClickableItem
import com.edgegesture.evilgodxu.ui.component.SectionCard
import com.edgegesture.evilgodxu.update.UpdateSource

// 「关于」区块：手势配置入口与捐赠入口
@Composable
fun AppAboutSection(
    onShowGestureConfig: () -> Unit,
    onShowDonate: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.settings_about)) {
        SettingsClickableItem(
            icon = Icons.Default.Folder,
            title = stringResource(R.string.gesture_config_title),
            subtitle = stringResource(R.string.gesture_config_desc),
            onClick = onShowGestureConfig,
        )
        HorizontalDivider()
        SettingsClickableItem(
            icon = Icons.Default.Favorite,
            title = stringResource(R.string.settings_donate),
            subtitle = stringResource(R.string.settings_donate_desc),
            onClick = onShowDonate,
        )
    }
}

// 页面底部：作者、版本与项目链接；版本号仅在提供回调时可点击（与宽屏布局保持一致）
@Composable
fun AppFooter(
    version: String,
    onOpenRepository: () -> Unit,
    onCheckForUpdate: (() -> Unit)? = null,
) {
    Text(
        text = "Evilgodxu",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 2.dp),
    )
    Text(
        text = stringResource(R.string.settings_version, version),
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onCheckForUpdate != null) Modifier.clickable(onClick = onCheckForUpdate) else Modifier)
            .padding(bottom = 6.dp),
    )

    val context = LocalContext.current
    Text(
        text = stringResource(R.string.settings_log),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { shareTodayLog(context) }
            .padding(bottom = 6.dp),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onOpenRepository)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = UpdateSource.REPOSITORY_URL,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}

// 唤起系统分享今日异常日志；今日无日志时提示用户
private fun shareTodayLog(context: Context) {
    val logFile = CrashLogManager.getTodayLogFile()
    if (logFile == null) {
        Toast.makeText(context, context.getString(R.string.settings_log_empty_today), Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", logFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.settings_share_log_title)),
    )
}
