package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Low-latency procedural SFX engine.
 * Pre-renders unique PCM buffers per [Sfx] and plays via short [AudioTrack] streams.
 */
class SoundEngine private constructor(context: Context) {

    @Volatile
    var enabled: Boolean = true

    @Volatile
    var hapticsEnabled: Boolean = true

    private val appContext = context.applicationContext
    private val sampleRate = 22050
    private val buffers = ConcurrentHashMap<Sfx, ShortArray>()
    private val executor: ExecutorService = Executors.newFixedThreadPool(3)
    private val ready = AtomicBoolean(false)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        executor.execute {
            Sfx.entries.forEach { sfx ->
                buffers[sfx] = synthesize(sfx)
            }
            ready.set(true)
        }
    }

    fun play(sfx: Sfx, pitchShift: Float = 1f) {
        if (!enabled) return
        executor.execute {
            val raw = buffers[sfx] ?: synthesize(sfx).also { buffers[sfx] = it }
            val data = if (pitchShift == 1f) raw else pitch(raw, pitchShift)
            playPcm(data)
        }
        hapticFor(sfx)
    }

    /** Distinct tab tones: each tab index gets a unique pitch. */
    fun playTab(index: Int) {
        val shifts = floatArrayOf(0.92f, 1.0f, 1.08f, 1.16f)
        play(Sfx.TAB_SWITCH, shifts.getOrElse(index) { 1f + index * 0.05f })
    }

    fun release() {
        executor.shutdownNow()
        buffers.clear()
    }

    private fun hapticFor(sfx: Sfx) {
        if (!hapticsEnabled) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        val ms = when (sfx) {
            Sfx.TASK_COMPLETE, Sfx.GOAL_COMPLETE, Sfx.FOCUS_COMPLETE, Sfx.SUCCESS -> 28L
            Sfx.DELETE, Sfx.ERROR -> 18L
            Sfx.FAB, Sfx.ADD_TASK, Sfx.ADD_BUDGET, Sfx.ADD_EVENT, Sfx.ADD_GOAL -> 16L
            Sfx.TAB_SWITCH, Sfx.FILTER_SELECT, Sfx.DAY_SELECT -> 10L
            else -> 8L
        }
        val amp = when (sfx) {
            Sfx.TASK_COMPLETE, Sfx.GOAL_COMPLETE -> 180
            Sfx.DELETE -> 140
            else -> 90
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(ms, amp.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(ms)
            }
        } catch (_: Exception) {
            // Ignore devices without vibration permission / support
        }
    }

    private fun playPcm(pcm: ShortArray) {
        if (pcm.isEmpty()) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()

        try {
            track.write(pcm, 0, pcm.size)
            track.setNotificationMarkerPosition(pcm.size)
            track.play()
            // Let audio finish then release
            val durationMs = (pcm.size * 1000L / sampleRate) + 40L
            Thread.sleep(durationMs.coerceAtMost(900L))
        } catch (_: Exception) {
            // Swallow audio race conditions
        } finally {
            try {
                track.stop()
            } catch (_: Exception) {
            }
            track.release()
        }
    }

    private fun pitch(src: ShortArray, factor: Float): ShortArray {
        if (factor == 1f) return src
        val outLen = (src.size / factor).toInt().coerceAtLeast(1)
        val out = ShortArray(outLen)
        for (i in out.indices) {
            val srcIndex = (i * factor).toInt().coerceIn(0, src.lastIndex)
            out[i] = src[srcIndex]
        }
        return out
    }

    // region Synthesis recipes — each Sfx is intentionally unique

    private fun synthesize(sfx: Sfx): ShortArray = when (sfx) {
        Sfx.TAP_SOFT -> tone(
            durationMs = 45,
            freqs = doubleArrayOf(420.0),
            wave = Wave.SINE,
            attack = 0.02,
            release = 0.7,
            volume = 0.28
        )

        Sfx.TAP_CRISP -> tone(
            durationMs = 38,
            freqs = doubleArrayOf(980.0, 1460.0),
            wave = Wave.TRIANGLE,
            attack = 0.01,
            release = 0.55,
            volume = 0.22
        )

        Sfx.TAP_CONFIRM -> tone(
            durationMs = 90,
            freqs = doubleArrayOf(520.0, 780.0),
            wave = Wave.SINE,
            attack = 0.05,
            release = 0.45,
            volume = 0.32
        )

        Sfx.DIALOG_OPEN -> sweep(
            durationMs = 110,
            startHz = 280.0,
            endHz = 720.0,
            wave = Wave.SINE,
            volume = 0.26
        )

        Sfx.DIALOG_CLOSE -> sweep(
            durationMs = 95,
            startHz = 640.0,
            endHz = 240.0,
            wave = Wave.SINE,
            volume = 0.22
        )

        Sfx.ADD_TASK -> chord(
            durationMs = 140,
            freqs = doubleArrayOf(523.25, 659.25, 783.99), // C major arpeggio-ish
            staggerMs = 28,
            wave = Wave.SINE,
            volume = 0.28
        )

        Sfx.ADD_BUDGET -> chord(
            durationMs = 150,
            freqs = doubleArrayOf(392.0, 493.88), // G + B
            staggerMs = 40,
            wave = Wave.TRIANGLE,
            volume = 0.3
        )

        Sfx.ADD_EVENT -> chord(
            durationMs = 160,
            freqs = doubleArrayOf(440.0, 554.37, 659.25), // A maj
            staggerMs = 32,
            wave = Wave.SINE,
            volume = 0.27
        )

        Sfx.ADD_GOAL -> chord(
            durationMs = 180,
            freqs = doubleArrayOf(349.23, 440.0, 523.25, 698.46),
            staggerMs = 30,
            wave = Wave.SINE,
            volume = 0.26
        )

        Sfx.ADD_ACCOUNT -> tone(
            durationMs = 120,
            freqs = doubleArrayOf(330.0, 495.0, 660.0),
            wave = Wave.SQUARE_SOFT,
            attack = 0.04,
            release = 0.5,
            volume = 0.18
        )

        Sfx.TASK_COMPLETE -> chord(
            durationMs = 220,
            freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.5),
            staggerMs = 35,
            wave = Wave.SINE,
            volume = 0.32
        )

        Sfx.TASK_UNDO -> tone(
            durationMs = 80,
            freqs = doubleArrayOf(300.0, 220.0),
            wave = Wave.TRIANGLE,
            attack = 0.05,
            release = 0.6,
            volume = 0.24
        )

        Sfx.SUBTASK_TOGGLE -> tone(
            durationMs = 50,
            freqs = doubleArrayOf(880.0),
            wave = Wave.TRIANGLE,
            attack = 0.01,
            release = 0.65,
            volume = 0.2
        )

        Sfx.DELETE -> noiseBurst(
            durationMs = 90,
            lowPass = 1800.0,
            volume = 0.22,
            falloff = true
        )

        Sfx.FILTER_SELECT -> tone(
            durationMs = 55,
            freqs = doubleArrayOf(740.0, 1110.0),
            wave = Wave.SINE,
            attack = 0.02,
            release = 0.5,
            volume = 0.22
        )

        Sfx.FAB -> tone(
            durationMs = 100,
            freqs = doubleArrayOf(600.0, 900.0, 1200.0),
            wave = Wave.SINE,
            attack = 0.02,
            release = 0.4,
            volume = 0.28
        )

        Sfx.TAB_SWITCH -> tone(
            durationMs = 70,
            freqs = doubleArrayOf(560.0, 840.0),
            wave = Wave.TRIANGLE,
            attack = 0.02,
            release = 0.55,
            volume = 0.24
        )

        Sfx.PROFILE_OPEN -> sweep(
            durationMs = 130,
            startHz = 360.0,
            endHz = 900.0,
            wave = Wave.TRIANGLE,
            volume = 0.24
        )

        Sfx.PROFILE_SAVE -> chord(
            durationMs = 160,
            freqs = doubleArrayOf(494.0, 622.0, 740.0),
            staggerMs = 25,
            wave = Wave.SINE,
            volume = 0.28
        )

        Sfx.THEME_CHANGE -> chord(
            durationMs = 170,
            freqs = doubleArrayOf(415.3, 554.4, 830.6),
            staggerMs = 38,
            wave = Wave.SINE,
            volume = 0.26
        )

        Sfx.SETTINGS_CHANGE -> tone(
            durationMs = 85,
            freqs = doubleArrayOf(640.0, 960.0),
            wave = Wave.SQUARE_SOFT,
            attack = 0.03,
            release = 0.5,
            volume = 0.16
        )

        Sfx.FOCUS_START -> sweep(
            durationMs = 180,
            startHz = 220.0,
            endHz = 660.0,
            wave = Wave.SINE,
            volume = 0.3
        )

        Sfx.FOCUS_PAUSE -> tone(
            durationMs = 100,
            freqs = doubleArrayOf(480.0, 360.0),
            wave = Wave.TRIANGLE,
            attack = 0.05,
            release = 0.55,
            volume = 0.26
        )

        Sfx.FOCUS_RESET -> tone(
            durationMs = 90,
            freqs = doubleArrayOf(300.0, 300.0, 200.0),
            wave = Wave.SINE,
            attack = 0.04,
            release = 0.5,
            volume = 0.24
        )

        Sfx.FOCUS_COMPLETE -> chord(
            durationMs = 280,
            freqs = doubleArrayOf(392.0, 493.88, 587.33, 784.0),
            staggerMs = 45,
            wave = Wave.SINE,
            volume = 0.34
        )

        Sfx.GOAL_PROGRESS -> tone(
            durationMs = 95,
            freqs = doubleArrayOf(698.46, 880.0),
            wave = Wave.SINE,
            attack = 0.03,
            release = 0.45,
            volume = 0.26
        )

        Sfx.GOAL_COMPLETE -> chord(
            durationMs = 260,
            freqs = doubleArrayOf(523.25, 659.25, 783.99, 987.77, 1174.7),
            staggerMs = 40,
            wave = Wave.SINE,
            volume = 0.32
        )

        Sfx.DAY_SELECT -> tone(
            durationMs = 60,
            freqs = doubleArrayOf(700.0),
            wave = Wave.TRIANGLE,
            attack = 0.02,
            release = 0.6,
            volume = 0.22
        )

        Sfx.EVENT_TOGGLE -> tone(
            durationMs = 75,
            freqs = doubleArrayOf(540.0, 810.0),
            wave = Wave.SINE,
            attack = 0.02,
            release = 0.5,
            volume = 0.24
        )

        Sfx.NOTE_SAVE -> tone(
            durationMs = 110,
            freqs = doubleArrayOf(466.16, 587.33),
            wave = Wave.SINE,
            attack = 0.04,
            release = 0.5,
            volume = 0.26
        )

        Sfx.SEARCH_FOCUS -> tone(
            durationMs = 50,
            freqs = doubleArrayOf(1000.0),
            wave = Wave.SINE,
            attack = 0.01,
            release = 0.7,
            volume = 0.18
        )

        Sfx.SUCCESS -> chord(
            durationMs = 200,
            freqs = doubleArrayOf(587.33, 739.99, 880.0),
            staggerMs = 30,
            wave = Wave.SINE,
            volume = 0.3
        )

        Sfx.ERROR -> tone(
            durationMs = 140,
            freqs = doubleArrayOf(180.0, 150.0),
            wave = Wave.SQUARE_SOFT,
            attack = 0.05,
            release = 0.4,
            volume = 0.2
        )
    }

    private enum class Wave { SINE, TRIANGLE, SQUARE_SOFT }

    private fun sampleWave(wave: Wave, phase: Double): Double = when (wave) {
        Wave.SINE -> sin(phase)
        Wave.TRIANGLE -> {
            // phase 0..2π → triangle -1..1
            val t = (phase / (2 * PI)) % 1.0
            if (t < 0.5) 4 * t - 1 else 3 - 4 * t
        }
        Wave.SQUARE_SOFT -> {
            val s = sin(phase)
            // Soft square via tanh-ish
            (s * 3.0).coerceIn(-1.0, 1.0) * 0.7
        }
    }

    private fun tone(
        durationMs: Int,
        freqs: DoubleArray,
        wave: Wave,
        attack: Double,
        release: Double,
        volume: Double
    ): ShortArray {
        val n = (sampleRate * durationMs / 1000.0).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        val phases = DoubleArray(freqs.size)
        val inc = DoubleArray(freqs.size) { i -> 2 * PI * freqs[i] / sampleRate }
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val env = envelope(t, attack, release)
            var sample = 0.0
            for (f in freqs.indices) {
                sample += sampleWave(wave, phases[f])
                phases[f] += inc[f]
            }
            sample = (sample / freqs.size) * env * volume
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun sweep(
        durationMs: Int,
        startHz: Double,
        endHz: Double,
        wave: Wave,
        volume: Double
    ): ShortArray {
        val n = (sampleRate * durationMs / 1000.0).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / (n - 1).coerceAtLeast(1)
            val hz = startHz + (endHz - startHz) * t
            val env = envelope(t, 0.05, 0.55)
            val sample = sampleWave(wave, phase) * env * volume
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            phase += 2 * PI * hz / sampleRate
        }
        return out
    }

    private fun chord(
        durationMs: Int,
        freqs: DoubleArray,
        staggerMs: Int,
        wave: Wave,
        volume: Double
    ): ShortArray {
        val n = (sampleRate * durationMs / 1000.0).toInt().coerceAtLeast(1)
        val out = DoubleArray(n)
        val stagger = (sampleRate * staggerMs / 1000.0).toInt()
        freqs.forEachIndexed { idx, hz ->
            val start = idx * stagger
            var phase = 0.0
            val inc = 2 * PI * hz / sampleRate
            for (i in start until n) {
                val localT = (i - start).toDouble() / (n - start).coerceAtLeast(1)
                val env = envelope(localT, 0.04, 0.5)
                out[i] += sampleWave(wave, phase) * env
                phase += inc
            }
        }
        val maxAbs = out.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-6)
        return ShortArray(n) { i ->
            val s = (out[i] / maxAbs) * volume
            (s * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun noiseBurst(
        durationMs: Int,
        lowPass: Double,
        volume: Double,
        falloff: Boolean
    ): ShortArray {
        val n = (sampleRate * durationMs / 1000.0).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        var prev = 0.0
        val alpha = lowPass / (lowPass + sampleRate)
        val rnd = Random(42)
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val env = if (falloff) (1.0 - t) * (1.0 - t) else envelope(t, 0.02, 0.5)
            val white = rnd.nextDouble(-1.0, 1.0)
            prev = prev + alpha * (white - prev)
            val sample = prev * env * volume
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun envelope(t: Double, attack: Double, release: Double): Double {
        val a = attack.coerceIn(0.01, 0.4)
        val r = release.coerceIn(0.1, 0.9)
        return when {
            t < a -> (t / a)
            t > 1.0 - r -> ((1.0 - t) / r).coerceAtLeast(0.0)
            else -> 1.0
        }.coerceIn(0.0, 1.0)
    }

    // endregion

    companion object {
        @Volatile
        private var instance: SoundEngine? = null

        fun get(context: Context): SoundEngine {
            return instance ?: synchronized(this) {
                instance ?: SoundEngine(context).also { instance = it }
            }
        }
    }
}
