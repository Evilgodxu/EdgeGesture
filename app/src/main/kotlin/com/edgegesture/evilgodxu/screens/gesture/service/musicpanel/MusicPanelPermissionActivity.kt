package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// 透明权限申请 Activity，用于从 Service/无障碍服务上下文动态申请权限：
// 1. 申请 READ_MEDIA_AUDIO（音频文件访问）
// 2. 音频权限申请完后，若未授予全部文件访问权限，自动跳转系统设置由用户手动授予
class MusicPanelPermissionActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // 音频权限申请流程结束（无论授予与否），继续检查全部文件访问权限
        requestAllFilesAccessIfNeeded()
    }

    // 音乐面板为高优先级悬浮窗，需等用户从系统设置返回后再展示，避免遮挡设置页
    private val allFilesSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        completeAndFinish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            hasAudioPermission() && hasAllFilesAccess() -> completeAndFinish()
            !hasAudioPermission() -> permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            else -> launchAllFilesSettings()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicPanelPermissionBridge.clearPendingShowAction()
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun hasAllFilesAccess(): Boolean = Environment.isExternalStorageManager()

    private fun requestAllFilesAccessIfNeeded() {
        if (hasAllFilesAccess()) {
            completeAndFinish()
            return
        }
        launchAllFilesSettings()
    }

    private fun launchAllFilesSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:$packageName")
        )
        try {
            allFilesSettingsLauncher.launch(intent)
        } catch (e: Exception) {
            // 部分 ROM 不支持该入口时兜底跳转应用详情页
            allFilesSettingsLauncher.launch(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun completeAndFinish() {
        MusicPanelPermissionBridge.takePendingShowAction()?.invoke()
        finish()
    }
}

// 权限申请与面板显示的桥接对象
object MusicPanelPermissionBridge {
    private var pendingToken = 0L
    private var pendingAction: (() -> Unit)? = null

    var pendingShowAction: (() -> Unit)?
        @Synchronized get() = pendingAction
        @Synchronized set(value) {
            pendingToken++
            pendingAction = value
        }

    @Synchronized
    fun clearPendingShowAction() {
        pendingToken++
        pendingAction = null
    }

    @Synchronized
    fun takePendingShowAction(): (() -> Unit)? {
        pendingToken++
        return pendingAction.also { pendingAction = null }
    }
}
