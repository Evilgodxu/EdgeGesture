package com.edgegesture.evilgodxu.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 单栏内容容器：窄窗占满可用宽度，宽窗限制行宽并水平居中。
 *
 * 只改变行宽、不改变功能区块的组成，因此形态差异用一个宽度上限表达；
 * 只有当两种形态的区块结构确实不同时，才值得拆成两套组装器。
 *
 * @param maxContentWidth 行宽上限，[Dp.Unspecified] 表示不限制。
 */
@Composable
fun ContentPane(
    maxContentWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = maxContentWidth)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}
