package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

class SoundSynth {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeVoices = AtomicInteger(0)

    @Volatile
    var enabled: Boolean = true

    // Pre-synthesized PCM buffers: zero per-frame allocation or trig math during gameplay!
    private val shootPcm by lazy { synthSweep(360.0, 680.0, 70, 0.22f) }
    private val bouncePcm by lazy { synthSweep(520.0, 480.0, 40, 0.16f) }
    private val snapPcm by lazy { synthSweep(260.0, 210.0, 50, 0.18f) }
    private val dropPcm by lazy { synthSweep(784.0, 960.0, 60, 0.18f) }
    private val bombPcm by lazy { synthSweep(180.0, 46.0, 210, 0.34f) }
    private val lightningPcm by lazy { synthSweep(1100.0, 320.0, 140, 0.28f) }
    private val portalPcm by lazy { synthSweep(420.0, 1080.0, 110, 0.24f) }
    private val overdrivePcm by lazy { synthSweep(523.25, 1046.5, 180, 0.30f) }
    private val bossHitPcm by lazy { synthSweep(210.0, 75.0, 160, 0.32f) }
    private val coinPcm by lazy { synthSweep(987.77, 1318.51, 95, 0.22f) }
    private val popPcms by lazy {
        Array(13) { step ->
            val freq = 440.0 * 2.0.pow(step / 12.0)
            synthSweep(freq, freq * 1.45, 80, 0.26f)
        }
    }
    private val victoryPcms by lazy {
        listOf(523.25, 659.25, 783.99, 1046.50).map { synthSweep(it, it * 1.02, 105, 0.24f) }
    }
    private val gameOverPcms by lazy {
        listOf(392.0, 349.23, 311.13, 261.63).map { synthSweep(it, it * 0.96, 120, 0.20f) }
    }

    fun warmUp() {
        scope.launch {
            shootPcm; bouncePcm; snapPcm; dropPcm; bombPcm; lightningPcm; portalPcm; overdrivePcm; bossHitPcm; coinPcm; popPcms
        }
    }

    fun playShoot() = playCached(shootPcm, 70L)
    fun playBounce() = playCached(bouncePcm, 40L)
    fun playSnap() = playCached(snapPcm, 50L)
    fun playDrop() = playCached(dropPcm, 60L)
    fun playBomb() = playCached(bombPcm, 210L)
    fun playLightning() = playCached(lightningPcm, 140L)
    fun playPortal() = playCached(portalPcm, 110L)
    fun playOverdrive() = playCached(overdrivePcm, 180L)
    fun playBossHit() = playCached(bossHitPcm, 160L)
    fun playCoin() = playCached(coinPcm, 95L)

    fun playPop(comboStep: Int) {
        val idx = comboStep.coerceIn(0, popPcms.lastIndex)
        playCached(popPcms[idx], 80L)
    }

    fun playVictory() {
        if (!enabled) return
        scope.launch {
            for (pcm in victoryPcms) playBlocking(pcm, 105L)
        }
    }

    fun playGameOver() {
        if (!enabled) return
        scope.launch {
            for (pcm in gameOverPcms) playBlocking(pcm, 120L)
        }
    }

    private fun playCached(samples: ShortArray, durationMs: Long) {
        if (!enabled) return
        if (activeVoices.get() >= MAX_CONCURRENT_VOICES) return
        scope.launch {
            if (activeVoices.incrementAndGet() <= MAX_CONCURRENT_VOICES) {
                try {
                    playBlocking(samples, durationMs)
                } finally {
                    activeVoices.decrementAndGet()
                }
            } else {
                activeVoices.decrementAndGet()
            }
        }
    }

    private fun playBlocking(samples: ShortArray, durationMs: Long) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(durationMs + 12L)
            track.stop()
            track.release()
        } catch (_: Throwable) {
            // Ignore in headless/test environments
        }
    }

    private fun synthSweep(startHz: Double, endHz: Double, durationMs: Int, volume: Float): ShortArray {
        val count = (SAMPLE_RATE * durationMs) / 1000
        if (count <= 0) return ShortArray(0)
        val samples = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = startHz + (endHz - startHz) * t
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val env = exp(-4.5 * t) * (1.0 - exp(-40.0 * t))
            val sample = (sin(phase) * env * volume * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            samples[i] = sample.toShort()
        }
        return samples
    }

    private companion object {
        const val SAMPLE_RATE = 22050
        const val MAX_CONCURRENT_VOICES = 4
    }
}
