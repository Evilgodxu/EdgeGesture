package com.byss.jh.data.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.byss.jh.data.gesture.initBlacklistIfNeeded
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

    // 完整初始化：加载缓存并触发后台扫描（需要权限）
    // 应在用户同意隐私政策后调用
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
    private suspend fun initBlacklistIfPermitted() {
        if (!hasQueryPermission()) return
        val apps = _appsFlow.value
        if (apps.isEmpty()) return
        val systemApps = apps.filter { it.isSystemApp }.map { it.packageName }.toSet()
        if (systemApps.isNotEmpty()) {
            context.initBlacklistIfNeeded(systemApps)
        }
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
                // 应用列表扫描完成后，初始化黑名单（将系统应用加入黑名单）
                val systemApps = apps.filter { it.isSystemApp }.map { it.packageName }.toSet()
                context.initBlacklistIfNeeded(systemApps)
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

    // 注册应用安装/卸载监听
    private fun registerAppChangeReceiver() {
        if (isReceiverRegistered) return
        isReceiverRegistered = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // 应用变更时延迟刷新缓存
                scope.launch {
                    kotlinx.coroutines.delay(1000)
                    refreshAppsIfPermitted()
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}

// 全局访问点扩展函数
fun Context.getAppRepository(): AppRepository {
    return AppRepository.getInstance(this)
}
