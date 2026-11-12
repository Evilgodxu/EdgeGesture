package com.edgegesture.evilgodxu.screens.blacklist

import com.edgegesture.evilgodxu.data.app.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

// 应用搜索的过滤与排序规则
class AppBlacklistFilterTest {

    private fun app(packageName: String, appName: String) =
        AppInfo(packageName = packageName, appName = appName, isSystemApp = false)

    private val apps = listOf(
        app("com.example.wechat", "微信"),
        app("com.example.wechatwork", "企业微信"),
        app("com.tencent.mm", "WeChat"),
        app("com.example.music", "音乐"),
    )

    @Test
    fun `空查询返回原列表`() {
        assertEquals(apps, filterAppsByQuery(apps, ""))
        assertEquals(apps, filterAppsByQuery(apps, "   "))
    }

    @Test
    fun `名称与包名都参与匹配且忽略大小写`() {
        val matched = filterAppsByQuery(apps, "wechat").map { it.packageName }.toSet()

        assertEquals(
            setOf("com.example.wechat", "com.example.wechatwork", "com.tencent.mm"),
            matched,
        )
        assertEquals(
            listOf("com.example.music"),
            filterAppsByQuery(apps, "MUSIC").map { it.packageName },
        )
    }

    @Test
    fun `完全匹配排在前缀匹配之前`() {
        val result = filterAppsByQuery(apps, "微信").map { it.packageName }

        assertEquals(listOf("com.example.wechat", "com.example.wechatwork"), result)
    }

    @Test
    fun `无匹配返回空列表`() {
        assertEquals(emptyList<AppInfo>(), filterAppsByQuery(apps, "不存在的应用"))
    }
}
