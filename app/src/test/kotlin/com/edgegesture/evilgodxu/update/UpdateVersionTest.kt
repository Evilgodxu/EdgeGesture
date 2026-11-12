package com.edgegesture.evilgodxu.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 版本比较：逐段按数值比较，缺位按 0 处理
class UpdateVersionTest {

    @Test
    fun `主版本更高判定为新版本`() {
        assertTrue(isNewerVersion("6.0.0", "5.3.0"))
        assertFalse(isNewerVersion("5.3.0", "6.0.0"))
    }

    @Test
    fun `按数值而不是字典序比较`() {
        assertTrue(isNewerVersion("5.10.0", "5.9.9"))
        assertFalse(isNewerVersion("5.9.9", "5.10.0"))
    }

    @Test
    fun `缺位按零处理`() {
        assertFalse(isNewerVersion("5.3", "5.3.0"))
        assertTrue(isNewerVersion("5.3.1", "5.3"))
    }

    @Test
    fun `相同版本不是新版本`() {
        assertFalse(isNewerVersion("5.3.0", "5.3.0"))
    }
}
