package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import com.edgegesture.evilgodxu.R

enum class SoundEffect(val displayNameResId: Int, val descriptionResId: Int) {
    BASS_BOOST(R.string.sound_effect_bass_boost, R.string.sound_effect_bass_boost_desc),
    PURE_VOICE(R.string.sound_effect_pure_voice, R.string.sound_effect_pure_voice_desc),
    SURROUND_ROTATION(R.string.sound_effect_surround_rotation, R.string.sound_effect_surround_rotation_desc),
    LOUDNESS_ENHANCER(R.string.sound_effect_loudness_enhancer, R.string.sound_effect_loudness_enhancer_desc),
    CONCERT_HALL(R.string.sound_effect_concert_hall, R.string.sound_effect_concert_hall_desc),
}

class AudioEffectManager(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var bassBoost: BassBoost? = null
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null

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

    fun isEffectEnabled(effect: SoundEffect): Boolean =
        prefs.getBoolean(effect.name, false)

    fun setEffectEnabled(effect: SoundEffect, enabled: Boolean) {
        prefs.edit().apply {
            if (enabled) {
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

        releaseAll()
        if (enabled) {
            applyEffect(effect, true)
        }
    }

    fun releaseAll() {
        listOf(bassBoost, equalizer, loudnessEnhancer, presetReverb)
            .forEach { it?.release() }
        bassBoost = null
        equalizer = null
        loudnessEnhancer = null
        presetReverb = null
        StereoRotationProcessor.INSTANCE.enabled = false
    }

    // ======================== 内部实现 ========================

    private fun applyEffect(effect: SoundEffect, enabled: Boolean) {
        try {
            when (effect) {
                SoundEffect.BASS_BOOST -> applyBassBoost(enabled)
                SoundEffect.PURE_VOICE -> applyPureVoice(enabled)
                SoundEffect.SURROUND_ROTATION -> applySurroundRotation(enabled)
                SoundEffect.LOUDNESS_ENHANCER -> applyLoudnessEnhancer(enabled)
                SoundEffect.CONCERT_HALL -> applyConcertHall(enabled)
            }
        } catch (_: Exception) {
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
                    val bandCount = numberOfBands.toInt()
                    val (minLevel, maxLevel) = bandLevelRange.let { it[0] to it[1] }
                    val boostLevel = (maxLevel * 0.4f).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                    val cutLevel = (minLevel * 0.25f).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                    for (i in 0 until bandCount) {
                        val centerFreqHz = getCenterFreq(i.toShort()) / 1000
                        val level = when {
                            centerFreqHz in 300..4000 -> boostLevel
                            centerFreqHz < 150 -> cutLevel
                            centerFreqHz > 8000 -> cutLevel
                            else -> 0
                        }
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

    private fun applySurroundRotation(enabled: Boolean) {
        StereoRotationProcessor.INSTANCE.enabled = enabled
    }

    private fun applyLoudnessEnhancer(enabled: Boolean) {
        if (enabled) {
            if (loudnessEnhancer == null) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    setTargetGain(400)
                    setEnabled(true)
                }
            } else {
                loudnessEnhancer!!.setEnabled(true)
            }
        } else {
            loudnessEnhancer?.let {
                it.setEnabled(false)
                it.release()
            }
            loudnessEnhancer = null
        }
    }


    private fun applyConcertHall(enabled: Boolean) {
        if (enabled) {
            if (presetReverb == null) {
                presetReverb = PresetReverb(0, audioSessionId).apply {
                    preset = PresetReverb.PRESET_LARGEHALL
                    setEnabled(true)
                }
            } else {
                presetReverb!!.setEnabled(true)
            }
        } else {
            presetReverb?.let {
                it.setEnabled(false)
                it.release()
            }
            presetReverb = null
        }
    }

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
