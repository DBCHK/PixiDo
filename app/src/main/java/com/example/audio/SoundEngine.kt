package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.SparseIntArray
import com.example.R
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Plays the recorded SoundFx pack (tap, tab, task, goal, transaction).
 * Splash intro stays a short synthesized chime.
 */
class SoundEngine private constructor(context: Context) {

    @Volatile
    var enabled: Boolean = true

    @Volatile
    var hapticsEnabled: Boolean = true

    private val appContext = context.applicationContext
    private val sampleRate = ToneSynth.SAMPLE_RATE
    private val executor: ExecutorService = Executors.newFixedThreadPool(3)
    private val activePlayers = Collections.synchronizedSet(mutableSetOf<MediaPlayer>())
    private val loaded = SparseIntArray()
    private val readyIds = mutableSetOf<Int>()
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                synchronized(readyIds) { readyIds.add(sampleId) }
            }
        }
        listOf(
            R.raw.sfx_tap,
            R.raw.sfx_tab_transition,
            R.raw.sfx_task,
            R.raw.sfx_goal,
            R.raw.sfx_transaction
        ).forEach { res ->
            loaded.put(res, pool.load(appContext, res, 1))
        }
    }

    fun play(sfx: Sfx, pitchShift: Float = 1f) {
        if (!enabled) return
        hapticFor(sfx)
        if (sfx == Sfx.SPLASH_INTRO) {
            val shift = pitchShift.toDouble().coerceIn(0.5, 2.0)
            executor.execute {
                playPcm(ToneSynth.render(gesture(sfx, shift)))
            }
            return
        }
        playSample(sampleRes(sfx))
    }

    fun playTab(@Suppress("UNUSED_PARAMETER") index: Int) {
        if (!enabled) return
        hapticFor(Sfx.TAB_SWITCH)
        playSample(R.raw.sfx_tab_transition)
    }

    fun release() {
        executor.shutdownNow()
        synchronized(activePlayers) {
            activePlayers.forEach { player ->
                try {
                    player.release()
                } catch (_: Exception) {
                }
            }
            activePlayers.clear()
        }
        pool.release()
    }

    private fun playSample(resId: Int) {
        val id = loaded.get(resId, 0)
        val ready = synchronized(readyIds) { id != 0 && readyIds.contains(id) }
        if (ready) {
            pool.play(id, 0.92f, 0.92f, 1, 0, 1f)
            return
        }
        executor.execute { playWithPlayer(resId) }
    }

    private fun playWithPlayer(resId: Int) {
        try {
            val mp = MediaPlayer.create(appContext, resId) ?: return
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setOnCompletionListener { player ->
                try {
                    player.release()
                } catch (_: Exception) {
                }
                activePlayers.remove(player)
            }
            activePlayers.add(mp)
            mp.start()
        } catch (_: Exception) {
        }
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
        if (pcm.size < 4) return
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
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
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(maxOf(pcm.size * 2, minBuf))
            .build()

        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            val frames = pcm.size / 2
            val durationMs = frames * 1000L / sampleRate + 40L
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

    companion object {
        internal fun sampleRes(sfx: Sfx): Int = when (sfx) {
            Sfx.SPLASH_INTRO -> 0
            Sfx.TAB_SWITCH -> R.raw.sfx_tab_transition
            Sfx.ADD_TASK, Sfx.TASK_COMPLETE, Sfx.TASK_UNDO,
            Sfx.FOCUS_COMPLETE, Sfx.SUCCESS, Sfx.ADD_EVENT,
            Sfx.NOTIF_SOFT -> R.raw.sfx_task
            Sfx.ADD_GOAL, Sfx.GOAL_PROGRESS, Sfx.GOAL_COMPLETE,
            Sfx.NOTIF_BRIGHT -> R.raw.sfx_goal
            Sfx.ADD_BUDGET, Sfx.ADD_ACCOUNT, Sfx.TRANSFER, Sfx.IMPORT,
            Sfx.NOTIF_CALM -> R.raw.sfx_transaction
            else -> R.raw.sfx_tap
        }

        // Concert pitch (Hz)
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
        private const val A5 = 880.00
        private const val B5 = 987.77
        private const val C6 = 1046.50
        private const val E6 = 1318.51

        internal fun gesture(sfx: Sfx, pitchShift: Double = 1.0): List<Tone> {
            fun t(
                hz: Double,
                start: Int = 0,
                dur: Int = 280,
                vel: Double = 0.4,
                bright: Double = 0.4
            ) = Tone(hz * pitchShift, start, dur, vel, bright)

            return when (sfx) {
                Sfx.TAP_SOFT -> listOf(t(E5, dur = 220, vel = 0.32, bright = 0.28))
                Sfx.TAP_CRISP -> listOf(t(G5, dur = 200, vel = 0.34, bright = 0.48))
                Sfx.TAP_CONFIRM -> listOf(
                    t(C5, dur = 340, vel = 0.38, bright = 0.36),
                    t(G5, start = 48, dur = 320, vel = 0.34, bright = 0.4)
                )
                Sfx.DIALOG_OPEN -> listOf(
                    t(E4, dur = 420, vel = 0.34, bright = 0.3),
                    t(G4, start = 70, dur = 400, vel = 0.32, bright = 0.32),
                    t(C5, start = 140, dur = 420, vel = 0.3, bright = 0.36)
                )
                Sfx.DIALOG_CLOSE -> listOf(
                    t(C5, dur = 300, vel = 0.3, bright = 0.3),
                    t(G4, start = 60, dur = 300, vel = 0.28, bright = 0.26),
                    t(E4, start = 120, dur = 320, vel = 0.26, bright = 0.22)
                )
                Sfx.ADD_TASK -> listOf(
                    t(C5, dur = 420, vel = 0.38, bright = 0.38),
                    t(E5, start = 78, dur = 400, vel = 0.36, bright = 0.4),
                    t(G5, start = 156, dur = 440, vel = 0.34, bright = 0.42)
                )
                Sfx.ADD_BUDGET -> listOf(
                    t(A4, dur = 360, vel = 0.36, bright = 0.32),
                    t(E5, start = 70, dur = 380, vel = 0.34, bright = 0.38)
                )
                Sfx.ADD_EVENT -> listOf(
                    t(D5, dur = 380, vel = 0.36, bright = 0.36),
                    t(F5, start = 72, dur = 360, vel = 0.34, bright = 0.38),
                    t(A5, start = 144, dur = 400, vel = 0.32, bright = 0.4)
                )
                Sfx.ADD_GOAL -> listOf(
                    t(G4, dur = 480, vel = 0.36, bright = 0.32),
                    t(C5, start = 88, dur = 460, vel = 0.35, bright = 0.36),
                    t(E5, start = 176, dur = 460, vel = 0.34, bright = 0.4),
                    t(G5, start = 264, dur = 500, vel = 0.32, bright = 0.42)
                )
                Sfx.ADD_ACCOUNT -> listOf(
                    t(F4, dur = 340, vel = 0.34, bright = 0.28),
                    t(C5, start = 78, dur = 360, vel = 0.32, bright = 0.34)
                )
                Sfx.TASK_COMPLETE -> listOf(
                    t(C5, dur = 520, vel = 0.4, bright = 0.4),
                    t(E5, start = 85, dur = 500, vel = 0.38, bright = 0.42),
                    t(G5, start = 170, dur = 520, vel = 0.36, bright = 0.46),
                    t(C6, start = 255, dur = 560, vel = 0.32, bright = 0.5)
                )
                Sfx.TASK_UNDO -> listOf(
                    t(E5, dur = 280, vel = 0.3, bright = 0.3),
                    t(C5, start = 70, dur = 300, vel = 0.28, bright = 0.26)
                )
                Sfx.SUBTASK_TOGGLE -> listOf(t(C5, dur = 180, vel = 0.28, bright = 0.3))
                Sfx.DELETE -> listOf(
                    t(A4, dur = 300, vel = 0.3, bright = 0.18),
                    t(E4, start = 55, dur = 320, vel = 0.28, bright = 0.16)
                )
                Sfx.FILTER_SELECT -> listOf(t(D5, dur = 200, vel = 0.3, bright = 0.34))
                Sfx.FAB -> listOf(
                    t(C5, dur = 280, vel = 0.36, bright = 0.38),
                    t(E5, start = 38, dur = 280, vel = 0.32, bright = 0.4)
                )
                Sfx.TAB_SWITCH -> listOf(t(E5, dur = 220, vel = 0.3, bright = 0.3))
                Sfx.PROFILE_OPEN -> listOf(
                    t(E4, dur = 400, vel = 0.34, bright = 0.3),
                    t(G4, start = 78, dur = 380, vel = 0.32, bright = 0.32),
                    t(B4, start = 156, dur = 400, vel = 0.3, bright = 0.36)
                )
                Sfx.PROFILE_SAVE -> listOf(
                    t(C5, dur = 400, vel = 0.36, bright = 0.36),
                    t(G5, start = 70, dur = 420, vel = 0.34, bright = 0.4),
                    t(C6, start = 150, dur = 440, vel = 0.3, bright = 0.44)
                )
                Sfx.THEME_CHANGE -> listOf(
                    t(A4, dur = 420, vel = 0.34, bright = 0.34),
                    t(E5, start = 90, dur = 400, vel = 0.32, bright = 0.38),
                    t(A5, start = 180, dur = 440, vel = 0.3, bright = 0.4)
                )
                Sfx.SETTINGS_CHANGE -> listOf(t(G5, dur = 200, vel = 0.3, bright = 0.34))
                Sfx.FOCUS_START -> listOf(
                    t(C4, dur = 560, vel = 0.34, bright = 0.24),
                    t(G4, start = 110, dur = 540, vel = 0.32, bright = 0.28),
                    t(C5, start = 220, dur = 580, vel = 0.3, bright = 0.32)
                )
                Sfx.FOCUS_PAUSE -> listOf(t(G4, dur = 300, vel = 0.3, bright = 0.2))
                Sfx.FOCUS_RESET -> listOf(
                    t(E4, dur = 300, vel = 0.3, bright = 0.22),
                    t(C4, start = 60, dur = 320, vel = 0.28, bright = 0.2)
                )
                Sfx.FOCUS_COMPLETE -> listOf(
                    t(C4, dur = 640, vel = 0.36, bright = 0.28),
                    t(G4, start = 110, dur = 620, vel = 0.34, bright = 0.32),
                    t(C5, start = 220, dur = 640, vel = 0.34, bright = 0.36),
                    t(E5, start = 330, dur = 640, vel = 0.32, bright = 0.4),
                    t(G5, start = 440, dur = 700, vel = 0.3, bright = 0.44)
                )
                Sfx.GOAL_PROGRESS -> listOf(
                    t(G5, dur = 260, vel = 0.34, bright = 0.4),
                    t(C6, start = 48, dur = 280, vel = 0.3, bright = 0.44)
                )
                Sfx.GOAL_COMPLETE -> listOf(
                    t(C5, dur = 560, vel = 0.38, bright = 0.4),
                    t(E5, start = 90, dur = 540, vel = 0.36, bright = 0.42),
                    t(G5, start = 180, dur = 560, vel = 0.34, bright = 0.46),
                    t(C6, start = 270, dur = 600, vel = 0.32, bright = 0.5),
                    t(E6, start = 360, dur = 640, vel = 0.26, bright = 0.52)
                )
                Sfx.DAY_SELECT -> listOf(t(A5, dur = 200, vel = 0.3, bright = 0.34))
                Sfx.EVENT_TOGGLE -> listOf(
                    t(E5, dur = 240, vel = 0.32, bright = 0.34),
                    t(G5, start = 40, dur = 240, vel = 0.3, bright = 0.36)
                )
                Sfx.NOTE_SAVE -> listOf(
                    t(D5, dur = 300, vel = 0.34, bright = 0.34),
                    t(A5, start = 52, dur = 320, vel = 0.3, bright = 0.38)
                )
                Sfx.SEARCH_FOCUS -> listOf(t(F5, dur = 190, vel = 0.26, bright = 0.3))
                Sfx.SUCCESS -> listOf(
                    t(E5, dur = 380, vel = 0.36, bright = 0.42),
                    t(G5, start = 70, dur = 380, vel = 0.34, bright = 0.44),
                    t(C6, start = 140, dur = 420, vel = 0.32, bright = 0.48)
                )
                Sfx.ERROR -> listOf(
                    t(D4, dur = 360, vel = 0.3, bright = 0.16),
                    t(A3, start = 50, dur = 380, vel = 0.28, bright = 0.14)
                )
                Sfx.SPLASH_INTRO -> listOf(
                    t(C4, dur = 720, vel = 0.34, bright = 0.26),
                    t(E4, start = 110, dur = 700, vel = 0.32, bright = 0.3),
                    t(G4, start = 220, dur = 720, vel = 0.32, bright = 0.34),
                    t(C5, start = 340, dur = 760, vel = 0.3, bright = 0.38),
                    t(E5, start = 460, dur = 800, vel = 0.26, bright = 0.4)
                )
                Sfx.TOGGLE_ON -> listOf(
                    t(E5, dur = 280, vel = 0.34, bright = 0.4),
                    t(B5, start = 40, dur = 300, vel = 0.3, bright = 0.44)
                )
                Sfx.TOGGLE_OFF -> listOf(
                    t(B5, dur = 260, vel = 0.3, bright = 0.34),
                    t(E5, start = 42, dur = 280, vel = 0.28, bright = 0.28)
                )
                Sfx.SNOOZE -> listOf(
                    t(G5, dur = 320, vel = 0.3, bright = 0.3),
                    t(E5, start = 70, dur = 320, vel = 0.28, bright = 0.26),
                    t(C5, start = 140, dur = 340, vel = 0.26, bright = 0.22)
                )
                Sfx.TRANSFER -> listOf(
                    t(D5, dur = 360, vel = 0.34, bright = 0.36),
                    t(A5, start = 72, dur = 360, vel = 0.32, bright = 0.4),
                    t(D5 * 2.0, start = 150, dur = 400, vel = 0.28, bright = 0.42)
                )
                Sfx.IMPORT -> listOf(
                    t(C5, dur = 380, vel = 0.34, bright = 0.36),
                    t(G5, start = 78, dur = 380, vel = 0.32, bright = 0.4),
                    t(C6, start = 156, dur = 420, vel = 0.3, bright = 0.44)
                )
                Sfx.PICK -> listOf(
                    t(A5, dur = 240, vel = 0.32, bright = 0.4),
                    t(E6, start = 34, dur = 260, vel = 0.28, bright = 0.46)
                )
                Sfx.NOTIF_SOFT -> listOf(
                    t(C5, dur = 700, vel = 0.4, bright = 0.36),
                    t(G5, start = 95, dur = 720, vel = 0.36, bright = 0.4)
                )
                Sfx.NOTIF_BRIGHT -> listOf(
                    t(E5, dur = 520, vel = 0.4, bright = 0.5),
                    t(G5, start = 70, dur = 500, vel = 0.36, bright = 0.52),
                    t(C6, start = 140, dur = 560, vel = 0.34, bright = 0.56)
                )
                Sfx.NOTIF_CALM -> listOf(
                    t(G4, dur = 900, vel = 0.34, bright = 0.24),
                    t(C5, start = 140, dur = 880, vel = 0.32, bright = 0.28),
                    t(E5, start = 280, dur = 920, vel = 0.3, bright = 0.3)
                )
            }
        }

        @Volatile
        private var instance: SoundEngine? = null

        fun get(context: Context): SoundEngine {
            return instance ?: synchronized(this) {
                instance ?: SoundEngine(context).also { instance = it }
            }
        }
    }
}
