package com.edgegesture.evilgodxu.update

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgegesture.evilgodxu.data.repository.SettingsRepository
import com.edgegesture.evilgodxu.data.repository.UpdateCheckRepository
import com.edgegesture.evilgodxu.log.CrashLogManager
import com.edgegesture.evilgodxu.utils.localization.LocalizationManager
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 更新域 UI 状态：待更新信息、对话框显隐与下载进度聚合为一份，消费侧只读
data class UpdateUiState(
    val updateInfo: UpdateInfo? = null,
    val dialogVisible: Boolean = false,
    val downloadState: DownloadState = DownloadState.Idle,
)

// 更新检查与下载状态的统一管理，供主界面与设置页共用，
// 以 Activity 作用域注入，保证两处读写同一状态，对话框由宿主全局弹出
class UpdateViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val updateCheckRepository: UpdateCheckRepository,
    private val updateChecker: UpdateChecker,
    private val localizationManager: LocalizationManager,
) : AndroidViewModel(application) {

    private val appContext get() = getApplication<Application>().applicationContext

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    // 手动检查结果的一次性提示：无回放，自动检查静默、仅手动检查反馈
    private val _messages = MutableSharedFlow<CheckFeedback>(extraBufferCapacity = 1)
    val messages: Flow<CheckFeedback> = _messages.asSharedFlow()

    /** 手动检查结果的提示类型 */
    enum class CheckFeedback { UP_TO_DATE, ERROR }

    // 回前台：是否已进入新的一天由检查记录仓库判定，页面不参与该判断
    fun onForeground() {
        checkForUpdate(force = false)
    }

    // 检查更新：有新版本时弹出更新对话框，否则手动检查时给出"已是最新"或"失败"提示
    fun checkForUpdate(force: Boolean = false) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            // 非强制检查每天最多一次
            if (!force && updateCheckRepository.lastCheckDay() == today) return@launch

            var checkFailed = false
            val result = try {
                updateChecker.check(updateCheckRepository.ignoredVersion()).also {
                    // 请求成功即记录检查日；失败不记录，下次回前台会重试
                    updateCheckRepository.setLastCheckDay(today)
                }
            } catch (e: Exception) {
                CrashLogManager.logException("UpdateViewModel", "检查更新失败", e)
                checkFailed = true
                null
            }

            if (result != null) {
                _uiState.update { it.copy(updateInfo = result, dialogVisible = true) }
                return@launch
            }
            // 自动检查静默：仅手动检查反馈一次性提示
            if (!force) return@launch
            _messages.emit(if (checkFailed) CheckFeedback.ERROR else CheckFeedback.UP_TO_DATE)
        }
    }

    // 下载并安装当前待更新版本
    fun downloadAndInstall() {
        val info = _uiState.value.updateInfo ?: return
        _uiState.update { it.copy(downloadState = DownloadState.Downloading(0f)) }
        viewModelScope.launch {
            // 下载通知文案面向用户：按当前应用语言构造本地化 Context 取资源，不使用系统 Context
            val language = settingsRepository.appLanguage.first()
            val localizedContext = localizationManager.createLocalizedContext(
                localizationManager.localeFor(language),
            )
            val success = UpdateDownloader(localizedContext).downloadAndInstall(info) { progress ->
                _uiState.update { state ->
                    state.copy(
                        downloadState = if (progress < 0f) {
                            DownloadState.Failed("download_failed")
                        } else {
                            DownloadState.Downloading(progress)
                        },
                    )
                }
            }
            _uiState.update { state ->
                when {
                    success -> state.copy(downloadState = DownloadState.Success, dialogVisible = false)
                    state.downloadState is DownloadState.Failed -> state
                    else -> state.copy(downloadState = DownloadState.Failed("download_failed"))
                }
            }
        }
    }

    // 关闭更新对话框：仅重置界面状态，持久层只在检查时写入
    fun dismissUpdateDialog() {
        _uiState.update { it.copy(dialogVisible = false, downloadState = DownloadState.Idle) }
    }
}

// 供界面树消费的组合局部，由宿主提供
val LocalUpdateViewModel = staticCompositionLocalOf<UpdateViewModel> {
    error("UpdateViewModel is not provided")
}
