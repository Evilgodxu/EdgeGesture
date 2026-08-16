package com.edgegesture.evilgodxu.data.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.edgegesture.evilgodxu.data.gesture.clearExpandPanelShortcut
import com.edgegesture.evilgodxu.data.gesture.initBlacklistIfNeeded
import com.edgegesture.evilgodxu.data.gesture.removeFromAppSwitchBlacklist
import com.edgegesture.evilgodxu.data.gesture.resetBlacklistInitialized
import com.edgegesture.evilgodxu.log.CrashLogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// 应用仓库单例，提供全局应用列表，支持延迟初始化和权限感知
class AppRepository private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    // 应用列表状态流
    private val _appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val appsFlow: StateFlow<List<AppInfo>> = _appsFlow.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 是否已注册广播监听
    private var isReceiverRegistered = false

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return instance ?: synchronized(this) {
                instance ?: AppRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    // 检查是否真正拥有查询所有应用权限
    fun hasQueryPermission(): Boolean {
        return runCatching {
            context.packageManager.getInstalledApplications(0).isNotEmpty()
        }.getOrDefault(false)
    }

    // 基于已扫描应用初始化黑名单
    private suspend fun initBlacklistFromApps(apps: List<AppInfo>) {
        val systemAppPackages = apps.filter { it.isSystemApp }.map { it.packageName }.toSet()
        context.initBlacklistIfNeeded(systemAppPackages)
    }

    private suspend fun initBlacklistFromCurrentApps() = mutex.withLock {
        initBlacklistFromApps(_appsFlow.value)
    }

    // 扫描应用列表
    private suspend fun scanApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))

        resolveInfos.map { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            val appInfo = activityInfo.applicationInfo
            val packageName = activityInfo.packageName
            val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                    appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

            AppInfo(
                packageName = packageName,
                appName = resolveInfo.loadLabel(pm).toString(),
                isSystemApp = isSystemApp,
                versionName = versionName(pm, packageName),
                sourcePath = appInfo.sourceDir.orEmpty()
            )
        }
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ !it.isSystemApp }, { it.appName }))
    }

    private fun versionName(pm: PackageManager, packageName: String): String {
        return runCatching {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L)).versionName
        }.getOrNull().orEmpty()
    }

    // 完整初始化：触发后台扫描
    fun initializeWithScan() {
        scope.launch {
            if (_appsFlow.value.isEmpty()) {
                refreshAppsIfPossible()
            } else {
                initBlacklistFromCurrentApps()
            }
        }
        registerAppChangeReceiver()
    }

    // 尝试刷新应用列表
    suspend fun refreshAppsIfPermitted(): Boolean = mutex.withLock {
        return try {
            refreshAppsInternal()
            true
        } catch (e: SecurityException) {
            CrashLogManager.logException("AppRepository", "刷新应用列表失败", e)
            false
        }
    }

    // 用户重新授予 QUERY_ALL_PACKAGES 后调用
    suspend fun onQueryPermissionGranted() = mutex.withLock {
        context.resetBlacklistInitialized()
        refreshAppsInternal()
    }

    private suspend fun refreshAppsInternal() {
        withContext(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val apps = scanApps()
                _appsFlow.value = apps
                initBlacklistFromApps(apps)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun refreshAppsIfPossible() {
        refreshAppsIfPermitted()
    }

    private val appChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val packageName = intent.data?.schemeSpecificPart ?: return

            when (action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    scope.launch {
                        kotlinx.coroutines.delay(500)
                        refreshAppsIfPermitted()
                    }
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    if (!isReplacing) {
                        scope.launch {
                            removeAppFromList(packageName)
                            cleanupUninstalledApp(packageName)
                        }
                    }
                }
                Intent.ACTION_PACKAGE_REPLACED -> {
                    scope.launch {
                        kotlinx.coroutines.delay(500)
                        refreshAppsIfPermitted()
                    }
                }
            }
        }
    }

    private suspend fun removeAppFromList(packageName: String) {
        mutex.withLock {
            val currentApps = _appsFlow.value.toMutableList()
            currentApps.removeAll { it.packageName == packageName }
            _appsFlow.value = currentApps
        }
    }

    private suspend fun cleanupUninstalledApp(packageName: String) {
        withContext(Dispatchers.IO) {
            context.removeFromAppSwitchBlacklist(setOf(packageName))
            context.clearExpandPanelShortcut(packageName)
        }
    }

    fun registerAppChangeReceiver() {
        if (isReceiverRegistered) return
        isReceiverRegistered = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context,
            appChangeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
