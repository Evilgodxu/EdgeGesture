package com.edgegesture.evilgodxu.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

// 主题持久化值的解析规则
class ThemeModeTest {

    @Test
    fun `写入值与枚举一一对应`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromValue(mode.value))
        }
    }

    @Test
    fun `未知值回落到跟随系统`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue("unsupported"))
    }

    @Test
    fun `空值回落到跟随系统`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue(""))
    }

    @Test
    fun `语言标签解析未知值回落到跟随系统`() {
        assertEquals(AppLanguage.CHINESE, AppLanguage.fromTag("zh"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
    }
}
