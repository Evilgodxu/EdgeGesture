package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// 透明权限申请 Activity，用于从 Service/无障碍服务上下文动态申请 READ_MEDIA_AUDIO
class MusicPanelPermissionActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            MusicPanelPermissionBridge.pendingShowAction?.invoke()
        }
        MusicPanelPermissionBridge.pendingShowAction = null
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    MusicPanelPermissionBridge.pendingShowAction?.invoke()
                    MusicPanelPermissionBridge.pendingShowAction = null
                    finish()
                }
                else -> {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                }
            }
        } else {
            // minSdk 为 33，理论上不会进入此分支；作为兜底直接放行
            MusicPanelPermissionBridge.pendingShowAction?.invoke()
            MusicPanelPermissionBridge.pendingShowAction = null
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicPanelPermissionBridge.pendingShowAction = null
    }
}

// 权限申请与面板显示的桥接对象
object MusicPanelPermissionBridge {
    var pendingShowAction: (() -> Unit)? = null
}