package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import com.edgegesture.evilgodxu.R

/**
 * 音效类型枚举（使用字符串资源 ID）
 */
enum class SoundEffect(val displayNameResId: Int, val descriptionResId: Int) {
    BASS_BOOST(R.string.sound_effect_bass_boost, R.string.sound_effect_bass_boost_desc),
    PURE_VOICE(R.string.sound_effect_pure_voice, R.string.sound_effect_pure_voice_desc),
}

/**
 * 音效管理器——基于 Android [android.media.audiofx] 官方 API
 *
 * 使用方式：
 * 1. 由 [MusicPlaybackState] 持有单例
 * 2. 音频会话 ID 就绪后由 [audioSessionId] setter 同步
 * 3. 通过 [setEffectEnabled] 开关各音效
 */
class AudioEffectManager(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var bassBoost: BassBoost? = null
    private var equalizer: Equalizer? = null

    @Volatile
    var audioSessionId: Int = 0
        set(value) {
            if (field != value) {
                releaseAll()
                field = value
                if (value > 0) restoreEnabledEffects()
            }
        }

    // ======================== 公开 API ========================

    /** 查询某音效是否已启用 */
    fun isEffectEnabled(effect: SoundEffect): Boolean =
        prefs.getBoolean(effect.name, false)

    /** 开关某音效，并立即生效（单选模式：同时只能启用一个） */
    fun setEffectEnabled(effect: SoundEffect, enabled: Boolean) {
        prefs.edit().apply {
            if (enabled) {
                // 单选：先关闭其他所有音效
                SoundEffect.entries.forEach { other ->
                    if (other != effect) putBoolean(other.name, false)
                }
                putBoolean(effect.name, true)
            } else {
                putBoolean(effect.name, false)
            }
            apply()
        }

        if (audioSessionId <= 0) {
            return
        }

        // 释放全部实例，仅重建当前选中的音效
        releaseAll()
        if (enabled) {
            applyEffect(effect, true)
        }
    }

    /** 释放所有音效实例（在音频会话变更或不再需要时调用） */
    fun releaseAll() {
        listOf(bassBoost, equalizer)
            .forEach { it?.release() }
        bassBoost = null
        equalizer = null
    }

    // ======================== 内部实现 ========================

    private fun applyEffect(effect: SoundEffect, enabled: Boolean) {
        try {
            when (effect) {
                SoundEffect.BASS_BOOST -> applyBassBoost(enabled)
                SoundEffect.PURE_VOICE -> applyPureVoice(enabled)
            }
        } catch (e: Exception) {
        }
    }

    private fun applyBassBoost(enabled: Boolean) {
        if (enabled) {
            if (bassBoost == null) {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    setStrength(500.toShort())
                    setEnabled(true)
                }
            } else {
                bassBoost!!.setEnabled(true)
            }
        } else {
            bassBoost?.let {
                it.setEnabled(false)
                it.release()
            }
            bassBoost = null
        }
    }

    private fun applyPureVoice(enabled: Boolean) {
        if (enabled) {
            if (equalizer == null) {
                equalizer = Equalizer(0, audioSessionId).apply {
                    // 猜测人声频段对应中间 60% 的均衡器频段
                    val bandCount = numberOfBands.toInt()
                    val midStart = bandCount / 4
                    val midEnd = bandCount * 3 / 4
                    for (i in 0 until bandCount) {
                        val level = if (i in midStart until midEnd) 600 else 0
                        setBandLevel(i.toShort(), level.toShort())
                    }
                    setEnabled(true)
                }
            } else {
                equalizer!!.setEnabled(true)
            }
        } else {
            equalizer?.let {
                it.setEnabled(false)
                it.release()
            }
            equalizer = null
        }
    }

    /** 恢复所有已开启的生效（在音频会话就绪后调用） */
    private fun restoreEnabledEffects() {
        SoundEffect.entries.forEach { effect ->
            if (isEffectEnabled(effect)) {
                applyEffect(effect, true)
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "audio_effect_prefs"
    }
}
