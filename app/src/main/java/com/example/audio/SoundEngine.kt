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
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Soft Lilac SFX engine — subtle, sweet, slow, low.
 *
 * Design principles (replaces the old retro/gamey palette):
 *  - Pure sine + soft second harmonic only (no square/triangle harshness)
 *  - Warm low–mid fundamentals (~180–520 Hz)
 *  - Longer soft attacks and trailing releases
 *  - Quiet volumes (gentle room presence, never sharp)
 *  - Feather-light haptics
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

    /** Distinct but gentle tab tones — small pitch offsets only. */
    fun playTab(index: Int) {
        val shifts = floatArrayOf(0.96f, 1.0f, 1.04f, 1.08f)
        play(Sfx.TAB_SWITCH, shifts.getOrElse(index) { 1f + index * 0.03f })
    }

    fun release() {
        executor.shutdownNow()
        buffers.clear()
    }

    private fun hapticFor(sfx: Sfx) {
        if (!hapticsEnabled) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        // Soft, short ticks — never punchy
        val ms = when (sfx) {
            Sfx.SPLASH_INTRO -> return // silent haptics on splash
            Sfx.TASK_COMPLETE, Sfx.GOAL_COMPLETE, Sfx.FOCUS_COMPLETE, Sfx.SUCCESS -> 18L
            Sfx.DELETE, Sfx.ERROR -> 12L
            Sfx.FAB, Sfx.ADD_TASK, Sfx.ADD_BUDGET, Sfx.ADD_EVENT, Sfx.ADD_GOAL -> 12L
            Sfx.TAB_SWITCH, Sfx.FILTER_SELECT, Sfx.DAY_SELECT -> 6L
            else -> 5L
        }
        val amp = when (sfx) {
            Sfx.TASK_COMPLETE, Sfx.GOAL_COMPLETE -> 90
            Sfx.DELETE -> 70
            else -> 45
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
            val durationMs = (pcm.size * 1000L / sampleRate) + 60L
            Thread.sleep(durationMs.coerceAtMost(1600L))
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

    // region Soft synthesis recipes

    private fun synthesize(sfx: Sfx): ShortArray = when (sfx) {
        // Soft wooden pillow tap
        Sfx.TAP_SOFT -> softTone(
            durationMs = 95,
            freqs = doubleArrayOf(280.0),
            attack = 0.08,
            release = 0.78,
            volume = 0.10,
            harmonic = 0.12
        )

        // Gentle glass click
        Sfx.TAP_CRISP -> softTone(
            durationMs = 80,
            freqs = doubleArrayOf(420.0, 630.0),
            attack = 0.06,
            release = 0.72,
            volume = 0.08,
            harmonic = 0.10
        )

        // Warm confirm hum
        Sfx.TAP_CONFIRM -> softTone(
            durationMs = 160,
            freqs = doubleArrayOf(260.0, 390.0),
            attack = 0.12,
            release = 0.65,
            volume = 0.12,
            harmonic = 0.15
        )

        // Slow airy open breath
        Sfx.DIALOG_OPEN -> softSweep(
            durationMs = 220,
            startHz = 180.0,
            endHz = 360.0,
            volume = 0.09
        )

        // Soft settle close
        Sfx.DIALOG_CLOSE -> softSweep(
            durationMs = 200,
            startHz = 320.0,
            endHz = 160.0,
            volume = 0.08
        )

        // Sweet rising third — new task
        Sfx.ADD_TASK -> softChord(
            durationMs = 320,
            freqs = doubleArrayOf(261.63, 329.63, 392.00), // C major, lower
            staggerMs = 55,
            volume = 0.11
        )

        // Warm two-tone — budget
        Sfx.ADD_BUDGET -> softChord(
            durationMs = 300,
            freqs = doubleArrayOf(220.0, 277.18), // A + C#
            staggerMs = 70,
            volume = 0.11
        )

        // Gentle event arpeggio
        Sfx.ADD_EVENT -> softChord(
            durationMs = 340,
            freqs = doubleArrayOf(246.94, 311.13, 370.0), // B maj-ish
            staggerMs = 60,
            volume = 0.10
        )

        // Soft sparkle triad — goal
        Sfx.ADD_GOAL -> softChord(
            durationMs = 380,
            freqs = doubleArrayOf(196.0, 246.94, 293.66, 349.23),
            staggerMs = 55,
            volume = 0.10
        )

        // Low bank-soft blip
        Sfx.ADD_ACCOUNT -> softTone(
            durationMs = 220,
            freqs = doubleArrayOf(196.0, 294.0),
            attack = 0.10,
            release = 0.68,
            volume = 0.10,
            harmonic = 0.12
        )

        // Satisfying soft major resolve — task done
        Sfx.TASK_COMPLETE -> softChord(
            durationMs = 420,
            freqs = doubleArrayOf(261.63, 329.63, 392.00, 523.25),
            staggerMs = 65,
            volume = 0.12
        )

        // Soft undo sigh
        Sfx.TASK_UNDO -> softTone(
            durationMs = 160,
            freqs = doubleArrayOf(240.0, 180.0),
            attack = 0.12,
            release = 0.70,
            volume = 0.09,
            harmonic = 0.08
        )

        // Tiny subtask tick
        Sfx.SUBTASK_TOGGLE -> softTone(
            durationMs = 90,
            freqs = doubleArrayOf(360.0),
            attack = 0.06,
            release = 0.75,
            volume = 0.08,
            harmonic = 0.10
        )

        // Soft paper hush (no harsh tear)
        Sfx.DELETE -> softHush(
            durationMs = 180,
            volume = 0.07
        )

        // Filter select murmur
        Sfx.FILTER_SELECT -> softTone(
            durationMs = 110,
            freqs = doubleArrayOf(300.0, 450.0),
            attack = 0.08,
            release = 0.68,
            volume = 0.09,
            harmonic = 0.10
        )

        // Soft FAB bloom
        Sfx.FAB -> softTone(
            durationMs = 180,
            freqs = doubleArrayOf(330.0, 440.0),
            attack = 0.08,
            release = 0.62,
            volume = 0.10,
            harmonic = 0.14
        )

        // Tab pad tone
        Sfx.TAB_SWITCH -> softTone(
            durationMs = 130,
            freqs = doubleArrayOf(240.0, 360.0),
            attack = 0.10,
            release = 0.70,
            volume = 0.09,
            harmonic = 0.10
        )

        // Profile sheet lift
        Sfx.PROFILE_OPEN -> softSweep(
            durationMs = 240,
            startHz = 200.0,
            endHz = 380.0,
            volume = 0.09
        )

        // Profile save soft chime
        Sfx.PROFILE_SAVE -> softChord(
            durationMs = 300,
            freqs = doubleArrayOf(277.18, 349.23, 415.30),
            staggerMs = 50,
            volume = 0.11
        )

        // Theme change gentle cascade
        Sfx.THEME_CHANGE -> softChord(
            durationMs = 340,
            freqs = doubleArrayOf(233.08, 293.66, 349.23),
            staggerMs = 70,
            volume = 0.10
        )

        // Settings soft tick
        Sfx.SETTINGS_CHANGE -> softTone(
            durationMs = 140,
            freqs = doubleArrayOf(280.0, 350.0),
            attack = 0.10,
            release = 0.68,
            volume = 0.08,
            harmonic = 0.08
        )

        // Focus start — slow breath up
        Sfx.FOCUS_START -> softSweep(
            durationMs = 360,
            startHz = 160.0,
            endHz = 320.0,
            volume = 0.11
        )

        // Focus pause — soft hold
        Sfx.FOCUS_PAUSE -> softTone(
            durationMs = 180,
            freqs = doubleArrayOf(240.0, 200.0),
            attack = 0.14,
            release = 0.68,
            volume = 0.09,
            harmonic = 0.08
        )

        // Focus reset — soft settle
        Sfx.FOCUS_RESET -> softTone(
            durationMs = 170,
            freqs = doubleArrayOf(220.0, 180.0),
            attack = 0.12,
            release = 0.68,
            volume = 0.09,
            harmonic = 0.08
        )

        // Focus complete — warm resolve
        Sfx.FOCUS_COMPLETE -> softChord(
            durationMs = 520,
            freqs = doubleArrayOf(196.0, 246.94, 293.66, 392.0),
            staggerMs = 80,
            volume = 0.12
        )

        // Goal progress bump
        Sfx.GOAL_PROGRESS -> softTone(
            durationMs = 160,
            freqs = doubleArrayOf(330.0, 415.0),
            attack = 0.10,
            release = 0.65,
            volume = 0.10,
            harmonic = 0.12
        )

        // Goal complete — soft fanfare (low)
        Sfx.GOAL_COMPLETE -> softChord(
            durationMs = 500,
            freqs = doubleArrayOf(220.0, 277.18, 329.63, 415.30),
            staggerMs = 75,
            volume = 0.12
        )

        // Day select
        Sfx.DAY_SELECT -> softTone(
            durationMs = 110,
            freqs = doubleArrayOf(300.0),
            attack = 0.08,
            release = 0.72,
            volume = 0.09,
            harmonic = 0.10
        )

        // Event toggle
        Sfx.EVENT_TOGGLE -> softTone(
            durationMs = 140,
            freqs = doubleArrayOf(280.0, 350.0),
            attack = 0.08,
            release = 0.68,
            volume = 0.09,
            harmonic = 0.10
        )

        // Note save
        Sfx.NOTE_SAVE -> softTone(
            durationMs = 200,
            freqs = doubleArrayOf(246.94, 311.13),
            attack = 0.12,
            release = 0.68,
            volume = 0.10,
            harmonic = 0.12
        )

        // Search focus
        Sfx.SEARCH_FOCUS -> softTone(
            durationMs = 100,
            freqs = doubleArrayOf(340.0),
            attack = 0.08,
            release = 0.78,
            volume = 0.07,
            harmonic = 0.08
        )

        // Success micro
        Sfx.SUCCESS -> softChord(
            durationMs = 360,
            freqs = doubleArrayOf(293.66, 370.0, 440.0),
            staggerMs = 55,
            volume = 0.11
        )

        // Soft error murmur (low, not buzzy)
        Sfx.ERROR -> softTone(
            durationMs = 240,
            freqs = doubleArrayOf(160.0, 140.0),
            attack = 0.14,
            release = 0.60,
            volume = 0.09,
            harmonic = 0.05
        )

        // Slow sweet splash intro — soft rising pad
        Sfx.SPLASH_INTRO -> softChord(
            durationMs = 900,
            freqs = doubleArrayOf(196.0, 246.94, 293.66, 392.0), // G minor-ish warm
            staggerMs = 140,
            volume = 0.09
        )
    }

    /**
     * Soft multi-partial sine tone.
     * [harmonic] = relative amplitude of second harmonic for sweetness.
     */
    private fun softTone(
        durationMs: Int,
        freqs: DoubleArray,
        attack: Double,
        release: Double,
        volume: Double,
        harmonic: Double
    ): ShortArray {
        val n = (sampleRate * durationMs / 1000.0).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        val phases = DoubleArray(freqs.size)
        val harmPhases = DoubleArray(freqs.size)
        val inc = DoubleArray(freqs.size) { i -> 2 * PI * freqs[i] / sampleRate }
        val harmInc = DoubleArray(freqs.size) { i -> 2 * PI * freqs[i] * 2.0 / sampleRate }

        for (i in 0 until n) {
            val t = i.toDouble() / n
            val env = softEnvelope(t, attack, release)
            var sample = 0.0
            for (f in freqs.indices) {
                val fund = sin(phases[f])
                val harm = sin(harmPhases[f]) * harmonic
                sample += fund + harm
                phases[f] += inc[f]
                harmPhases[f] += harmInc[f]
            }
            // Mild low-pass smoothness via neighbor blend is approximated by soft env
            sample = (sample / freqs.size) * env * volume
            out[i] = toShort(sample)
        }
        return out
    }

    private fun softSweep(
        durationMs: Int,
        startHz: Double,
        endHz: Double,
        volume: Double
    ): ShortArray {
        val n = (sampleRate * durationMs / 1000.0).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        var phase = 0.0
        var harmPhase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / (n - 1).coerceAtLeast(1)
            // Ease-in-out curve for slower, sweeter glide
            val eased = t * t * (3.0 - 2.0 * t)
            val hz = startHz + (endHz - startHz) * eased
            val env = softEnvelope(t, 0.12, 0.62)
            val sample = (sin(phase) + 0.12 * sin(harmPhase)) * env * volume
            out[i] = toShort(sample)
            phase += 2 * PI * hz / sampleRate
            harmPhase += 2 * PI * hz * 2.0 / sampleRate
        }
        return out
    }

    private fun softChord(
        durationMs: Int,
        freqs: DoubleArray,
        staggerMs: Int,
        volume: Double
    ): ShortArray {
        val n = (sampleRate * durationMs / 1000.0).toInt().coerceAtLeast(1)
        val out = DoubleArray(n)
        val stagger = (sampleRate * staggerMs / 1000.0).toInt()
        freqs.forEachIndexed { idx, hz ->
            val start = idx * stagger
            var phase = 0.0
            var harmPhase = 0.0
            val inc = 2 * PI * hz / sampleRate
            val harmInc = 2 * PI * hz * 2.0 / sampleRate
            for (i in start until n) {
                val localT = (i - start).toDouble() / (n - start).coerceAtLeast(1)
                val env = softEnvelope(localT, 0.10, 0.62)
                out[i] += (sin(phase) + 0.12 * sin(harmPhase)) * env
                phase += inc
                harmPhase += harmInc
            }
        }
        val maxAbs = out.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-6)
        return ShortArray(n) { i ->
            toShort((out[i] / maxAbs) * volume)
        }
    }

    /** Soft filtered hush for delete — quiet pink-ish air, never a tear. */
    private fun softHush(durationMs: Int, volume: Double): ShortArray {
        val n = (sampleRate * durationMs / 1000.0).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        var b0 = 0.0
        var b1 = 0.0
        var b2 = 0.0
        val rnd = Random(7)
        // Soft low sine bed under the hush
        var phase = 0.0
        val bedInc = 2 * PI * 140.0 / sampleRate
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val env = (1.0 - t).pow(1.6) * softEnvelope(t, 0.08, 0.55)
            val white = rnd.nextDouble(-1.0, 1.0)
            // Very soft pink-ish filter
            b0 = 0.99765 * b0 + white * 0.0990460
            b1 = 0.96300 * b1 + white * 0.2965164
            b2 = 0.57000 * b2 + white * 1.0526913
            val pink = (b0 + b1 + b2 + white * 0.1848) * 0.05
            val bed = sin(phase) * 0.35
            phase += bedInc
            out[i] = toShort((pink + bed) * env * volume)
        }
        return out
    }

    /**
     * Soft ADS-like envelope with rounded attack and long tail.
     * Attack and release are fractions of total duration.
     */
    private fun softEnvelope(t: Double, attack: Double, release: Double): Double {
        val a = attack.coerceIn(0.04, 0.45)
        val r = release.coerceIn(0.25, 0.9)
        val raw = when {
            t < a -> {
                // Smoothstep attack
                val x = (t / a).coerceIn(0.0, 1.0)
                x * x * (3.0 - 2.0 * x)
            }
            t > 1.0 - r -> {
                // Exponential-ish soft release
                val x = ((1.0 - t) / r).coerceAtLeast(0.0)
                x * x
            }
            else -> 1.0
        }
        // Gentle overall contour so peaks never feel spiky
        return (raw * exp(-0.15 * t)).coerceIn(0.0, 1.0)
    }

    private fun toShort(sample: Double): Short {
        val clamped = sample.coerceIn(-1.0, 1.0)
        return (clamped * Short.MAX_VALUE).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
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
