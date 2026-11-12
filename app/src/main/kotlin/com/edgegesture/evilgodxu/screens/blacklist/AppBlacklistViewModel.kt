package com.edgegesture.evilgodxu.screens.blacklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgegesture.evilgodxu.appContainer
import com.edgegesture.evilgodxu.data.gesture.addToAppSwitchBlacklist
import com.edgegesture.evilgodxu.data.gesture.appSwitchBlacklistFlow
import com.edgegesture.evilgodxu.data.gesture.removeFromAppSwitchBlacklist
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 应用黑名单页 ViewModel：汇聚应用列表、黑名单与查询状态，负责过滤排序与开关写入
class AppBlacklistViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext
    private val appRepository = context.appContainer.appRepository

    private val _uiState = MutableStateFlow(
        AppBlacklistUiState(hasPermission = appRepository.hasQueryPermission()),
    )
    val uiState: StateFlow<AppBlacklistUiState> = _uiState.asStateFlow()

    // 输入防抖：用户停止输入 300ms 后才触发搜索过滤
    private var debounceJob: Job? = null

    init {
        viewModelScope.launch {
            context.appSwitchBlacklistFlow().collect { blacklist ->
                _uiState.update { it.copy(blacklist = blacklist) }
            }
        }
        viewModelScope.launch {
            appRepository.appsFlow.collect { apps ->
                _uiState.update { state -> state.copy(apps = apps, visibleApps = filterAppsByQuery(apps, state.debouncedQuery)) }
            }
        }
        viewModelScope.launch {
            appRepository.isLoading.collect { loading ->
                _uiState.update { it.copy(isLoading = loading) }
            }
        }
    }

    // 页面回到前台时重新查询权限，避免授权后不更新
    fun onResume() {
        _uiState.update { it.copy(hasPermission = appRepository.hasQueryPermission()) }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { state ->
                state.copy(
                    debouncedQuery = query,
                    visibleApps = filterAppsByQuery(state.apps, query),
                )
            }
        }
    }

    fun clearQuery() = onQueryChange("")

    // 勾选/取消黑名单：按包名批量写入，保持与拦截服务的集合语义一致
    fun setBlacklisted(packageName: String, blacklisted: Boolean) {
        viewModelScope.launch {
            if (blacklisted) {
                context.addToAppSwitchBlacklist(setOf(packageName))
            } else {
                context.removeFromAppSwitchBlacklist(setOf(packageName))
            }
        }
    }

    fun retryRefresh() {
        viewModelScope.launch {
            appRepository.refreshAppsIfPermitted()
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
