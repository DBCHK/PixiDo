package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Calm piano SFX — additive felt-piano notes with a unique voicing every play.
 *
 * Each gesture is a short, quiet piano figure (single notes, dyads, arpeggios)
 * with random velocity, cents detune, and inversion so it never feels looped
 * or retro/chiptune.
 */
class SoundEngine private constructor(context: Context) {

    @Volatile
    var enabled: Boolean = true

    @Volatile
    var hapticsEnabled: Boolean = true

    private val appContext = context.applicationContext
    private val sampleRate = 44100
    private val executor: ExecutorService = Executors.newFixedThreadPool(3)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun play(sfx: Sfx, pitchShift: Float = 1f) {
        if (!enabled) return
        executor.execute {
            playPcm(pianoGesture(sfx, pitchShift))
        }
        hapticFor(sfx)
    }

    fun playTab(index: Int) {
        val notes = doubleArrayOf(C4, E4, G4, A4)
        playPitched(notes.getOrElse(index) { C4 }, durationMs = 520, velocity = 0.28)
    }

    fun release() {
        executor.shutdownNow()
    }

    private fun playPitched(hz: Double, durationMs: Int, velocity: Double) {
        if (!enabled) return
        executor.execute {
            val rnd = Random.Default
            playPcm(
                pianoNote(
                    hz = hz * centsToRatio(rnd.nextDouble(-7.0, 7.0)),
                    durationMs = durationMs,
                    velocity = velocity * rnd.nextDouble(0.88, 1.08)
                )
            )
        }
        hapticFor(Sfx.TAB_SWITCH)
    }

    private fun hapticFor(sfx: Sfx) {
        if (!hapticsEnabled) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        val ms = when (sfx) {
            Sfx.SPLASH_INTRO -> return
            Sfx.TASK_COMPLETE, Sfx.GOAL_COMPLETE, Sfx.FOCUS_COMPLETE, Sfx.SUCCESS -> 16L
            Sfx.DELETE, Sfx.ERROR -> 10L
            Sfx.FAB, Sfx.ADD_TASK, Sfx.ADD_BUDGET, Sfx.ADD_EVENT, Sfx.ADD_GOAL -> 10L
            Sfx.TAB_SWITCH, Sfx.FILTER_SELECT, Sfx.DAY_SELECT -> 5L
            else -> 4L
        }
        val amp = when (sfx) {
            Sfx.TASK_COMPLETE, Sfx.GOAL_COMPLETE -> 70
            Sfx.DELETE -> 55
            else -> 32
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(ms, amp.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(ms)
            }
        } catch (_: Exception) {
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
            track.play()
            val durationMs = (pcm.size * 1000L / sampleRate) + 80L
            Thread.sleep(durationMs.coerceAtMost(2800L))
        } catch (_: Exception) {
        } finally {
            try {
                track.pause()
            } catch (_: Exception) {
            }
            track.release()
        }
    }

    // region Piano gestures

    private fun pianoGesture(sfx: Sfx, pitchShift: Float): ShortArray {
        val rnd = Random.Default
        val detune = centsToRatio(rnd.nextDouble(-6.0, 6.0)) * pitchShift
        val vel = rnd.nextDouble(0.34, 0.48)
        val invert = rnd.nextInt(3)

        return when (sfx) {
            Sfx.TAP_SOFT -> pianoNote(vary(E5, rnd) * detune, 480, vel * 0.7)
            Sfx.TAP_CRISP -> pianoNote(vary(G5, rnd) * detune, 460, vel * 0.66)
            Sfx.TAP_CONFIRM -> pianoChord(
                voicing(doubleArrayOf(C4, G4), invert),
                durationMs = 620,
                staggerMs = 28,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.DIALOG_OPEN -> pianoArp(
                doubleArrayOf(C4, E4, G4),
                durationMs = 620,
                staggerMs = 70,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.DIALOG_CLOSE -> pianoArp(
                doubleArrayOf(G4, E4, C4),
                durationMs = 540,
                staggerMs = 65,
                velocity = vel * 0.9,
                detune = detune,
                rnd = rnd
            )
            Sfx.ADD_TASK -> pianoArp(
                voicing(doubleArrayOf(C4, E4, G4, C5), invert),
                durationMs = 780,
                staggerMs = 85,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.ADD_BUDGET -> pianoChord(
                voicing(doubleArrayOf(A3, E4), invert),
                durationMs = 520,
                staggerMs = 40,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.ADD_EVENT -> pianoArp(
                voicing(doubleArrayOf(D4, F4, A4), invert),
                durationMs = 700,
                staggerMs = 80,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.ADD_GOAL -> pianoArp(
                voicing(doubleArrayOf(G3, C4, E4, G4), invert),
                durationMs = 860,
                staggerMs = 90,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.ADD_ACCOUNT -> pianoChord(
                voicing(doubleArrayOf(F3, C4), invert),
                durationMs = 500,
                staggerMs = 36,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.TASK_COMPLETE -> pianoArp(
                voicing(doubleArrayOf(C4, E4, G4, C5), invert),
                durationMs = 980,
                staggerMs = 95,
                velocity = vel * 1.05,
                detune = detune,
                rnd = rnd
            )
            Sfx.TASK_UNDO -> pianoChord(
                doubleArrayOf(E4, C4),
                durationMs = 420,
                staggerMs = 40,
                velocity = vel * 0.85,
                detune = detune,
                rnd = rnd
            )
            Sfx.SUBTASK_TOGGLE -> pianoNote(vary(C5, rnd) * detune, 420, vel * 0.58)
            Sfx.DELETE -> pianoChord(
                doubleArrayOf(A3, E3),
                durationMs = 480,
                staggerMs = 30,
                velocity = vel * 0.7,
                detune = detune,
                rnd = rnd
            )
            Sfx.FILTER_SELECT -> pianoNote(vary(D5, rnd) * detune, 460, vel * 0.6)
            Sfx.FAB -> pianoChord(
                voicing(doubleArrayOf(C4, E4), invert),
                durationMs = 440,
                staggerMs = 32,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.TAB_SWITCH -> pianoNote(vary(E4, rnd) * detune, 500, vel * 0.55)
            Sfx.PROFILE_OPEN -> pianoArp(
                doubleArrayOf(E4, G4, C5),
                durationMs = 640,
                staggerMs = 75,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.PROFILE_SAVE -> pianoChord(
                voicing(doubleArrayOf(C4, G4, E5), invert),
                durationMs = 640,
                staggerMs = 45,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.THEME_CHANGE -> pianoArp(
                voicing(doubleArrayOf(A3, E4, A4), invert),
                durationMs = 720,
                staggerMs = 80,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.SETTINGS_CHANGE -> pianoNote(vary(G4, rnd) * detune, 480, vel * 0.58)
            Sfx.FOCUS_START -> pianoArp(
                doubleArrayOf(C3, G3, C4),
                durationMs = 900,
                staggerMs = 110,
                velocity = vel * 0.9,
                detune = detune,
                rnd = rnd
            )
            Sfx.FOCUS_PAUSE -> pianoNote(G3 * detune, 420, vel * 0.7)
            Sfx.FOCUS_RESET -> pianoChord(
                doubleArrayOf(E3, C4),
                durationMs = 480,
                staggerMs = 36,
                velocity = vel * 0.75,
                detune = detune,
                rnd = rnd
            )
            Sfx.FOCUS_COMPLETE -> pianoArp(
                voicing(doubleArrayOf(C3, G3, C4, E4, G4), invert),
                durationMs = 1200,
                staggerMs = 100,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.GOAL_PROGRESS -> pianoChord(
                voicing(doubleArrayOf(G4, C5), invert),
                durationMs = 460,
                staggerMs = 30,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.GOAL_COMPLETE -> pianoArp(
                voicing(doubleArrayOf(C4, E4, G4, C5, E5), invert),
                durationMs = 1200,
                staggerMs = 95,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.DAY_SELECT -> pianoNote(vary(A4, rnd) * detune, 480, vel * 0.58)
            Sfx.EVENT_TOGGLE -> pianoChord(
                voicing(doubleArrayOf(E4, G4), invert),
                durationMs = 400,
                staggerMs = 28,
                velocity = vel * 0.75,
                detune = detune,
                rnd = rnd
            )
            Sfx.NOTE_SAVE -> pianoChord(
                voicing(doubleArrayOf(D4, A4), invert),
                durationMs = 480,
                staggerMs = 34,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.SEARCH_FOCUS -> pianoNote(vary(F5, rnd) * detune, 440, vel * 0.48)
            Sfx.SUCCESS -> pianoArp(
                voicing(doubleArrayOf(E4, G4, C5), invert),
                durationMs = 760,
                staggerMs = 80,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.ERROR -> pianoChord(
                doubleArrayOf(D3, A3),
                durationMs = 560,
                staggerMs = 40,
                velocity = vel * 0.7,
                detune = detune,
                rnd = rnd
            )
            Sfx.SPLASH_INTRO -> pianoArp(
                voicing(doubleArrayOf(C3, G3, C4, E4, G4), invert),
                durationMs = 1700,
                staggerMs = 140,
                velocity = vel * 0.8,
                detune = detune,
                rnd = rnd
            )
            Sfx.TOGGLE_ON -> pianoChord(
                voicing(doubleArrayOf(E4, B4, E5), invert),
                durationMs = 560,
                staggerMs = 36,
                velocity = vel * 0.9,
                detune = detune,
                rnd = rnd
            )
            Sfx.TOGGLE_OFF -> pianoChord(
                voicing(doubleArrayOf(E5, B4, E4), invert),
                durationMs = 520,
                staggerMs = 40,
                velocity = vel * 0.82,
                detune = detune,
                rnd = rnd
            )
            Sfx.SNOOZE -> pianoArp(
                doubleArrayOf(G4, E4, C4),
                durationMs = 640,
                staggerMs = 70,
                velocity = vel * 0.85,
                detune = detune,
                rnd = rnd
            )
            Sfx.TRANSFER -> pianoArp(
                voicing(doubleArrayOf(D4, A4, D5), invert),
                durationMs = 700,
                staggerMs = 75,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.IMPORT -> pianoArp(
                voicing(doubleArrayOf(C4, G4, C5), invert),
                durationMs = 720,
                staggerMs = 80,
                velocity = vel,
                detune = detune,
                rnd = rnd
            )
            Sfx.PICK -> pianoChord(
                voicing(doubleArrayOf(A4, E5), invert),
                durationMs = 480,
                staggerMs = 28,
                velocity = vel * 0.85,
                detune = detune,
                rnd = rnd
            )
        }
    }

    /**
     * Felt-piano additive note: inharmonic partials, faster decay up the spectrum,
     * a few milliseconds of hammer dust, long quiet tail.
     */
    private fun pianoNote(
        hz: Double,
        durationMs: Int,
        velocity: Double,
        delayMs: Int = 0
    ): ShortArray {
        val n = (sampleRate * (durationMs + delayMs) / 1000.0).toInt().coerceAtLeast(1)
        val out = DoubleArray(n)
        val delay = (sampleRate * delayMs / 1000.0).toInt().coerceAtLeast(0)
        val partials = 9
        val B = 0.00018 * (C4 / hz).coerceIn(0.4, 2.4)
        val phases = DoubleArray(partials)
        val incs = DoubleArray(partials)
        val amps = DoubleArray(partials)
        val decays = DoubleArray(partials)
        for (p in 1..partials) {
            val i = p - 1
            val inharm = p * hz * sqrt(1.0 + B * p * p)
            incs[i] = 2 * PI * inharm / sampleRate
            amps[i] = velocity / p.toDouble().pow(1.25)
            decays[i] = 1.6 + p * 0.55
        }

        var noise = 0.0
        val rnd = Random(hz.toBits() xor durationMs.toLong())
        val sounding = n - delay
        for (i in delay until n) {
            val t = (i - delay).toDouble() / sampleRate
            val attack = 1.0 - exp(-t * 70.0)
            var sample = 0.0
            for (p in 0 until partials) {
                val env = attack * exp(-t * decays[p])
                sample += sin(phases[p]) * amps[p] * env
                phases[p] += incs[p]
            }
            if (t < 0.012) {
                noise = noise * 0.72 + rnd.nextDouble(-1.0, 1.0) * 0.28
                val hammerEnv = (1.0 - t / 0.012).coerceAtLeast(0.0)
                sample += noise * 0.03 * velocity * hammerEnv
            }
            val local = (i - delay).toDouble() / sounding.coerceAtLeast(1)
            out[i] = sample * cosineRelease(local, 0.38)
        }
        return normalize(withSilencePad(out), 0.09)
    }

    private fun pianoChord(
        freqs: DoubleArray,
        durationMs: Int,
        staggerMs: Int,
        velocity: Double,
        detune: Double,
        rnd: Random
    ): ShortArray {
        val notes = freqs.map { hz ->
            pianoNote(
                hz = hz * detune * centsToRatio(rnd.nextDouble(-4.0, 4.0)),
                durationMs = durationMs,
                velocity = velocity * rnd.nextDouble(0.9, 1.05),
                delayMs = 0
            )
        }
        return mix(notes, staggerMs, rnd)
    }

    private fun pianoArp(
        freqs: DoubleArray,
        durationMs: Int,
        staggerMs: Int,
        velocity: Double,
        detune: Double,
        rnd: Random
    ): ShortArray {
        val notes = freqs.mapIndexed { idx, hz ->
            pianoNote(
                hz = hz * detune * centsToRatio(rnd.nextDouble(-5.0, 5.0)),
                durationMs = (durationMs - idx * staggerMs / 2).coerceAtLeast(220),
                velocity = velocity * rnd.nextDouble(0.88, 1.06),
                delayMs = idx * staggerMs
            )
        }
        return mix(notes, 0, rnd)
    }

    private fun mix(notes: List<ShortArray>, staggerMs: Int, rnd: Random): ShortArray {
        if (notes.isEmpty()) return ShortArray(0)
        val stagger = (sampleRate * staggerMs / 1000.0).toInt()
        val n = notes.mapIndexed { i, arr -> arr.size + i * stagger }.maxOrNull() ?: 0
        val acc = DoubleArray(n)
        notes.forEachIndexed { idx, arr ->
            val start = idx * stagger
            val wobble = 1.0 + rnd.nextDouble(-0.03, 0.03)
            for (i in arr.indices) {
                val dest = start + i
                if (dest < n) acc[dest] += arr[i] / 32767.0 * wobble
            }
        }
        val faded = DoubleArray(acc.size) { i ->
            acc[i] * cosineRelease(i.toDouble() / acc.size.coerceAtLeast(1), 0.32)
        }
        return normalize(withSilencePad(faded), 0.09)
    }

    /** Cosine fade over the last [releaseFrac] of the note so it never clips off. */
    private fun cosineRelease(t: Double, releaseFrac: Double): Double {
        val start = (1.0 - releaseFrac).coerceIn(0.15, 0.92)
        if (t <= start) return 1.0
        val x = ((t - start) / (1.0 - start)).coerceIn(0.0, 1.0)
        return 0.5 * (1.0 + cos(PI * x))
    }

    private fun withSilencePad(src: DoubleArray, padMs: Int = 90): DoubleArray {
        val pad = (sampleRate * padMs / 1000.0).toInt().coerceAtLeast(0)
        if (pad == 0) return src
        val out = DoubleArray(src.size + pad)
        for (i in src.indices) out[i] = src[i]
        return out
    }

    private fun normalize(src: DoubleArray, peak: Double): ShortArray {
        val maxAbs = src.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-9)
        val g = peak / maxAbs
        return ShortArray(src.size) { i ->
            val v = (src[i] * g).coerceIn(-1.0, 1.0)
            (v * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun voicing(notes: DoubleArray, invert: Int): DoubleArray {
        if (notes.isEmpty()) return notes
        val k = invert.mod(notes.size)
        if (k == 0) return notes
        return DoubleArray(notes.size) { i ->
            val src = notes[(i + k) % notes.size]
            if (i + k >= notes.size) src / 2.0 else src
        }
    }

    private fun vary(hz: Double, rnd: Random): Double {
        val neighbors = doubleArrayOf(hz, hz * 1.12246, hz / 1.12246, hz * 0.8909)
        return neighbors[rnd.nextInt(neighbors.size)]
    }

    private fun centsToRatio(cents: Double): Double = 2.0.pow(cents / 1200.0)

    // endregion

    companion object {
        // Piano pitches (Hz)
        private const val C3 = 130.81
        private const val D3 = 146.83
        private const val E3 = 164.81
        private const val F3 = 174.61
        private const val G3 = 196.00
        private const val A3 = 220.00
        private const val C4 = 261.63
        private const val D4 = 293.66
        private const val E4 = 329.63
        private const val F4 = 349.23
        private const val G4 = 392.00
        private const val A4 = 440.00
        private const val B4 = 493.88
        private const val C5 = 523.25
        private const val D5 = 587.33
        private const val E5 = 659.25
        private const val F5 = 698.46
        private const val G5 = 783.99

        @Volatile
        private var instance: SoundEngine? = null

        fun get(context: Context): SoundEngine {
            return instance ?: synchronized(this) {
                instance ?: SoundEngine(context).also { instance = it }
            }
        }
    }
}
