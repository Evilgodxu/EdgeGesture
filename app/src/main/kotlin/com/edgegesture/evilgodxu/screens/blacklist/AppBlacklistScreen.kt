package com.edgegesture.evilgodxu.screens.blacklist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgegesture.evilgodxu.screens.blacklist.assembly.AppBlacklistAssembly

// 页面入口：状态与事件上提，回前台时刷新权限判定
@Composable
fun AppBlacklistScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppBlacklistViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    AppBlacklistAssembly(
        state = uiState,
        onBack = onNavigateBack,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::clearQuery,
        onToggle = viewModel::setBlacklisted,
        onRetry = viewModel::retryRefresh,
    )
}
