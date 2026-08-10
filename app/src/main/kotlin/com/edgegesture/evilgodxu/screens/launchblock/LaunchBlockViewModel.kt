package com.edgegesture.evilgodxu.screens.launchblock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.LaunchBlockState
import com.edgegesture.evilgodxu.data.launchblock.addLaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.launchBlockFlow
import com.edgegesture.evilgodxu.data.launchblock.removeLaunchBlockRule
import com.edgegesture.evilgodxu.data.launchblock.setLaunchBlockEnabled
import com.edgegesture.evilgodxu.data.launchblock.updateLaunchBlockRule
import com.edgegesture.evilgodxu.log.CrashLogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 启动拦截页 ViewModel：订阅拦截规则流，集中处理规则增删改与总开关
class LaunchBlockViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    val state: StateFlow<LaunchBlockState> = context.launchBlockFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LaunchBlockState(),
        )

    // 应用名缓存：包名 -> 应用名，异步解析避免在主线程查询 PackageManager
    private val _appNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val appNames: StateFlow<Map<String, String>> = _appNames.asStateFlow()

    // 设置拦截总开关
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.setLaunchBlockEnabled(enabled)
        }
    }

    // 新增规则
    fun addRule(rule: LaunchBlockRule) {
        viewModelScope.launch {
            context.addLaunchBlockRule(rule)
        }
    }

    // 更新规则
    fun updateRule(rule: LaunchBlockRule) {
        viewModelScope.launch {
            context.updateLaunchBlockRule(rule)
        }
    }

    // 删除规则
    fun removeRule(ruleId: String) {
        viewModelScope.launch {
            context.removeLaunchBlockRule(ruleId)
        }
    }

    // 异步解析包名对应的应用名并写入缓存，已缓存或空包名直接跳过
    fun resolveAppName(packageName: String) {
        if (packageName.isBlank() || _appNames.value.containsKey(packageName)) return
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { loadAppName(packageName) }
            _appNames.value = _appNames.value + (packageName to name)
        }
    }

    private fun loadAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            CrashLogManager.logException("LaunchBlockViewModel", "获取应用名称失败", e)
            packageName
        }
    }
}
