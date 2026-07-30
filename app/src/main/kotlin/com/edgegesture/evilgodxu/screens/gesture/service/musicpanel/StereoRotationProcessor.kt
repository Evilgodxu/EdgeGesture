package com.edgegesture.evilgodxu.screens.gesture.service.musicpanel

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.sin

class StereoRotationProcessor : BaseAudioProcessor() {

    @Volatile
    var enabled: Boolean = false

    private var sampleCounter: Long = 0

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.channelCount != 2 || inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun isActive(): Boolean = inputAudioFormat.sampleRate > 0

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val sampleRate = inputAudioFormat.sampleRate
        val out = replaceOutputBuffer(remaining)

        if (enabled) {
            val frames = remaining / 4
            for (i in 0 until frames) {
                val left = inputBuffer.short.toInt()
                val right = inputBuffer.short.toInt()

                val angle = 2.0 * PI * sampleCounter / (sampleRate * ROTATION_PERIOD)
                val mod = sin(angle) * ROTATION_DEPTH

                val leftOut = (left * (1.0 + mod)).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                val rightOut = (right * (1.0 - mod)).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                out.putShort(leftOut.toShort())
                out.putShort(rightOut.toShort())
                sampleCounter++
            }
        } else {
            out.put(inputBuffer)
        }

        out.flip()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onFlush() {
        sampleCounter = 0
    }

    companion object {
        val INSTANCE = StereoRotationProcessor()
        private const val ROTATION_PERIOD = 6.0
        private const val ROTATION_DEPTH = 0.3
    }
}