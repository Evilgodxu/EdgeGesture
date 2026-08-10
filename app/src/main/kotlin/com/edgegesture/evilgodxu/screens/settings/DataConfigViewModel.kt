package com.edgegesture.evilgodxu.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgegesture.evilgodxu.data.app.DataConfigManager
import com.edgegesture.evilgodxu.data.app.ManagedDataItem
import com.edgegesture.evilgodxu.data.app.ManagedDataType
import com.edgegesture.evilgodxu.screens.gesture.service.musicpanel.MusicPanelStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 数据配置页 ViewModel：封装缓存数据列表加载与清理操作
class DataConfigViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    // 缓存数据列表
    private val _items = MutableStateFlow<List<ManagedDataItem>>(emptyList())
    val items: StateFlow<List<ManagedDataItem>> = _items.asStateFlow()

    // 清理操作结果提示
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    // 重新加载缓存数据列表
    fun refresh() {
        viewModelScope.launch {
            _items.value = DataConfigManager.listData(context)
        }
    }

    // 清理所选数据：先释放音乐播放器再删除文件，最后刷新列表并提示结果
    fun clearData(selected: Set<ManagedDataType>) {
        viewModelScope.launch {
            DataConfigManager.clear(context, selected) { MusicPanelStateHolder.state.release() }
            _items.value = DataConfigManager.listData(context)
            _message.value = "清理完成，相关数据已重新初始化"
        }
    }
}
