package com.edgegesture.evilgodxu.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 更新检查与下载状态的统一管理，供主界面与设置页共用，
// 状态生命周期与 ViewModel 一致，离开页面不会中断下载
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _showUpToDate = MutableStateFlow(false)
    val showUpToDate: StateFlow<Boolean> = _showUpToDate.asStateFlow()

    private val _showUpdateError = MutableStateFlow(false)
    val showUpdateError: StateFlow<Boolean> = _showUpdateError.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // 检查更新：有新版本时弹出更新对话框，否则区分"已是最新"与"检查失败"
    fun checkForUpdate(force: Boolean = false) {
        viewModelScope.launch {
            var checkFailed = false
            val result = UpdateManager.checkForUpdate(
                context,
                force = force,
                onError = { checkFailed = true }
            )
            if (result != null) {
                _updateInfo.value = result
                _showUpdateDialog.value = true
            } else if (checkFailed) {
                _showUpdateError.value = true
            } else {
                _showUpToDate.value = true
            }
        }
    }

    // 下载并安装当前待更新版本
    fun downloadAndInstall() {
        val info = _updateInfo.value ?: return
        _downloadState.value = DownloadState.Downloading(0f)
        viewModelScope.launch {
            val success = UpdateManager.downloadAndInstall(context, info) { progress ->
                _downloadState.value = if (progress < 0f) {
                    DownloadState.Failed("download_failed")
                } else {
                    DownloadState.Downloading(progress)
                }
            }
            if (success) {
                _downloadState.value = DownloadState.Success
                _showUpdateDialog.value = false
            } else if (_downloadState.value !is DownloadState.Failed) {
                _downloadState.value = DownloadState.Failed("download_failed")
            }
        }
    }

    // 关闭更新对话框并清理待更新信息
    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
        _downloadState.value = DownloadState.Idle
        UpdateManager.clearPendingUpdate(context)
    }

    fun clearUpToDate() {
        _showUpToDate.value = false
    }

    fun clearUpdateError() {
        _showUpdateError.value = false
    }
}
