package com.byss.jh.data.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.byss.jh.data.gesture.clearExpandPanelShortcut
import com.byss.jh.data.gesture.initBlacklistIfNeeded
import com.byss.jh.data.gesture.removeFromAppSwitchBlacklist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// 应用仓库单例，提供全局应用列表缓存，支持延迟初始化和权限感知
class AppRepository private constructor(private val context: Context) {

    private val cacheManager = AppCacheManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    // 应用列表状态流
    private val _appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val appsFlow: StateFlow<List<AppInfo>> = _appsFlow.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 是否已初始化（从缓存加载过）
    private var isInitialized = false

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

    // 检查是否有查询应用权限
    fun hasQueryPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.QUERY_ALL_PACKAGES
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 延迟初始化：仅从缓存加载，不触发扫描
    // 应在应用启动时调用，无需权限
    fun initializeFromCache() {
        if (isInitialized) return
        isInitialized = true

        scope.launch {
            // 仅从缓存加载，不触发扫描
            val cachedApps = cacheManager.getCachedAppsFlow().first()
            _appsFlow.value = cachedApps
        }
    }

    // 完整初始化：加载缓存并触发后台扫描（需要 QUERY_ALL_PACKAGES 权限）
    // 应在获取权限后调用，如 GestureSettingsViewModel 中权限监控回调
    fun initializeWithScan() {
        if (!isInitialized) {
            initializeFromCache()
        }

        scope.launch {
            // 检查缓存是否有效，无效则刷新
            if (!cacheManager.isCacheValid() || _appsFlow.value.isEmpty()) {
                refreshAppsIfPermitted()
            } else {
                // 缓存有效，检查权限后初始化黑名单
                initBlacklistIfPermitted()
            }
        }

        // 注册应用变更监听
        registerAppChangeReceiver()
    }

    // 有条件地初始化黑名单：有权限时才初始化
    // 黑名单包含所有系统应用（包括无入口的），但应用列表只显示有入口的应用
    private suspend fun initBlacklistIfPermitted() {
        if (!hasQueryPermission()) return
        // 直接初始化黑名单，内部会获取所有系统应用
        context.initBlacklistIfNeeded()
    }

    // 条件刷新：有权限时才扫描
    suspend fun refreshAppsIfPermitted(): Boolean = mutex.withLock {
        if (!hasQueryPermission()) {
            return false
        }
        refreshAppsInternal()
        return true
    }

    // 强制刷新应用列表（调用前需确保有权限）
    suspend fun refreshApps() = mutex.withLock {
        refreshAppsInternal()
    }

    private suspend fun refreshAppsInternal() {
        withContext(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val apps = cacheManager.quickScanApps()
                _appsFlow.value = apps
                cacheManager.saveAppsToCache(apps)
                // 应用列表扫描完成后，初始化黑名单
                // 黑名单包含所有系统应用（包括无入口的），但应用列表只显示有入口的应用
                context.initBlacklistIfNeeded()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 获取当前应用列表（同步）
    fun getAppsSync(): List<AppInfo> = _appsFlow.value

    // 搜索应用
    fun searchApps(query: String): List<AppInfo> {
        val apps = _appsFlow.value
        if (query.isBlank()) return apps

        val lowerQuery = query.lowercase()
        return apps.filter {
            it.appName.lowercase().contains(lowerQuery) ||
            it.packageName.lowercase().contains(lowerQuery)
        }
    }

    // 根据包名获取应用信息
    fun getAppByPackageName(packageName: String): AppInfo? {
        return _appsFlow.value.find { it.packageName == packageName }
    }

    // 获取应用显示名称
    fun getAppLabel(packageName: String): String {
        return getAppByPackageName(packageName)?.appName ?: packageName
    }

    // 应用变更广播接收器
    private val appChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val packageName = intent.data?.schemeSpecificPart ?: return

            when (action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    // 新应用安装，延迟后刷新
                    scope.launch {
                        kotlinx.coroutines.delay(500)
                        refreshAppsIfPermitted()
                    }
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    // 应用卸载，立即从缓存中移除并清理相关数据
                    scope.launch {
                        removeAppFromCache(packageName)
                        cleanupUninstalledApp(packageName)
                    }
                }
                Intent.ACTION_PACKAGE_REPLACED -> {
                    // 应用更新，刷新缓存
                    scope.launch {
                        kotlinx.coroutines.delay(500)
                        refreshAppsIfPermitted()
                    }
                }
            }
        }
    }

    // 从缓存中移除指定应用
    private suspend fun removeAppFromCache(packageName: String) {
        mutex.withLock {
            val currentApps = _appsFlow.value.toMutableList()
            val removed = currentApps.removeAll { it.packageName == packageName }
            if (removed) {
                _appsFlow.value = currentApps
                cacheManager.saveAppsToCache(currentApps)
            }
        }
    }

    // 清理已卸载应用的相关数据（黑名单、扩展面板快捷方式）
    private suspend fun cleanupUninstalledApp(packageName: String) {
        withContext(Dispatchers.IO) {
            // 从应用切换黑名单中移除
            context.removeFromAppSwitchBlacklist(setOf(packageName))
            // 清理扩展面板快捷方式
            context.clearExpandPanelShortcut(packageName)
        }
    }

    // 注册应用安装/卸载监听
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

    // 注销广播接收器
    fun unregisterAppChangeReceiver() {
        if (!isReceiverRegistered) return
        isReceiverRegistered = false
        context.unregisterReceiver(appChangeReceiver)
    }
}

// 全局访问点扩展函数
fun Context.getAppRepository(): AppRepository {
    return AppRepository.getInstance(this)
}
