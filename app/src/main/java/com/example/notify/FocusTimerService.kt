package com.example.notify

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Focus foreground service + Now Bar.
 *
 * **Do not re-post the Live Update every second** — that demotes Now Bar on One UI 8.5.
 * Chronometer on the notification handles the visible countdown; we only:
 *  - startForeground once with the timer card
 *  - re-post on pause/resume/stop
 *  - soft progress refresh every ~20s
 *  - broadcast ticks to the UI every second
 */
class FocusTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null
    private var lastPostedAt = 0L
    private var lastPostedSeconds = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NowBarHelper.ensureChannel(this)

        when (intent?.action) {
            ACTION_START, ACTION_RESUME -> {
                val total = intent.getIntExtra(EXTRA_TOTAL_SECONDS, state.totalSeconds)
                    .coerceAtLeast(60)
                val left = intent.getIntExtra(EXTRA_SECONDS_LEFT, state.secondsLeft)
                    .coerceAtLeast(1)
                state = state.copy(
                    totalSeconds = total,
                    secondsLeft = left,
                    isRunning = true,
                    endAtElapsed = SystemClock.elapsedRealtime() + left * 1000L
                )
                // Promote once with the timer card (this is what sticks Now Bar)
                postAndBindForeground(force = true)
                startTicking()
                broadcastState()
            }
            ACTION_PAUSE -> {
                syncSecondsFromClock()
                state = state.copy(isRunning = false)
                tickJob?.cancel()
                postAndBindForeground(force = true)
                broadcastState()
            }
            ACTION_STOP -> stopSession(completed = false)
            else -> {
                if (state.secondsLeft > 0) {
                    postAndBindForeground(force = true)
                } else {
                    // Satisfy FGS contract if we were started empty
                    postAndBindForeground(force = true)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun postAndBindForeground(force: Boolean) {
        val endWall = System.currentTimeMillis() + state.secondsLeft * 1000L
        val notification = NowBarHelper.buildFocusNotification(
            context = this,
            secondsLeft = state.secondsLeft,
            totalSeconds = state.totalSeconds,
            isRunning = state.isRunning,
            endAtMillis = endWall
        )
        enterForeground(notification)
        if (force || shouldRefreshNotification()) {
            try {
                androidx.core.app.NotificationManagerCompat.from(this)
                    .notify(NowBarHelper.ID_FOCUS, notification)
                lastPostedAt = SystemClock.elapsedRealtime()
                lastPostedSeconds = state.secondsLeft
            } catch (e: Exception) {
                Log.e(TAG, "notify failed", e)
            }
        }
    }

    /** Throttle: re-post at most every 20s while running (progress bar only). */
    private fun shouldRefreshNotification(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPostedAt > 20_000L) return true
        // Also if minutes rolled for progress segment feel
        if (lastPostedSeconds >= 0 &&
            lastPostedSeconds / 60 != state.secondsLeft / 60
        ) return true
        return false
    }

    private fun enterForeground(notification: Notification): Boolean {
        // Always try SPECIAL_USE first (long Focus sessions), then SHORT_SERVICE, then bare.
        if (Build.VERSION.SDK_INT >= 34) {
            val types = intArrayOf(
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            )
            for (type in types) {
                try {
                    ServiceCompat.startForeground(
                        this,
                        NowBarHelper.ID_FOCUS,
                        notification,
                        type
                    )
                    Log.i(TAG, "FGS started type=$type")
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "FGS type=$type failed: ${e.message}")
                }
            }
            return try {
                @Suppress("DEPRECATION")
                startForeground(NowBarHelper.ID_FOCUS, notification)
                true
            } catch (e: Exception) {
                Log.e(TAG, "startForeground bare failed", e)
                // Still post the Live Update even if FGS is blocked
                try {
                    androidx.core.app.NotificationManagerCompat.from(this)
                        .notify(NowBarHelper.ID_FOCUS, notification)
                } catch (_: Exception) {
                }
                false
            }
        }
        return try {
            startForeground(NowBarHelper.ID_FOCUS, notification)
            true
        } catch (e: Exception) {
            Log.e(TAG, "enterForeground", e)
            false
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive && state.isRunning) {
                delay(1000L)
                if (!isActive || !state.isRunning) break
                syncSecondsFromClock()
                // UI tick only — do NOT re-notify every second (breaks Now Bar)
                broadcastState()
                // Rare progress refresh
                if (shouldRefreshNotification()) {
                    postAndBindForeground(force = false)
                }
                if (state.secondsLeft <= 0) {
                    stopSession(completed = true)
                    break
                }
            }
        }
    }

    private fun syncSecondsFromClock() {
        if (!state.isRunning) return
        val left = ((state.endAtElapsed - SystemClock.elapsedRealtime()) / 1000L)
            .toInt().coerceAtLeast(0)
        state = state.copy(secondsLeft = left)
    }

    private fun stopSession(completed: Boolean) {
        tickJob?.cancel()
        val left = if (completed) 0 else state.secondsLeft
        state = state.copy(isRunning = false, secondsLeft = left)
        NowBarHelper.clearFocus(this)
        sendBroadcast(
            Intent(ACTION_STATE).setPackage(packageName).apply {
                putExtra(EXTRA_SECONDS_LEFT, left)
                putExtra(EXTRA_TOTAL_SECONDS, state.totalSeconds)
                putExtra(EXTRA_RUNNING, false)
                putExtra(EXTRA_COMPLETED, completed)
            }
        )
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
        stopSelf()
        state = FocusState()
        lastPostedSeconds = -1
    }

    private fun broadcastState() {
        sendBroadcast(
            Intent(ACTION_STATE).setPackage(packageName).apply {
                putExtra(EXTRA_SECONDS_LEFT, state.secondsLeft)
                putExtra(EXTRA_TOTAL_SECONDS, state.totalSeconds)
                putExtra(EXTRA_RUNNING, state.isRunning)
                putExtra(EXTRA_COMPLETED, false)
            }
        )
    }

    companion object {
        private const val TAG = "PixiDoFocusSvc"

        const val ACTION_START = "com.example.pixido.FOCUS_START"
        const val ACTION_PAUSE = "com.example.pixido.FOCUS_PAUSE"
        const val ACTION_RESUME = "com.example.pixido.FOCUS_RESUME"
        const val ACTION_STOP = "com.example.pixido.FOCUS_STOP"
        const val ACTION_STATE = "com.example.pixido.FOCUS_STATE"

        const val EXTRA_SECONDS_LEFT = "focus_seconds_left"
        const val EXTRA_TOTAL_SECONDS = "focus_total_seconds"
        const val EXTRA_RUNNING = "focus_running"
        const val EXTRA_COMPLETED = "focus_completed"

        @Volatile
        private var state = FocusState()

        data class FocusState(
            val secondsLeft: Int = 25 * 60,
            val totalSeconds: Int = 25 * 60,
            val isRunning: Boolean = false,
            val endAtElapsed: Long = 0L
        )

        fun start(context: Context, secondsLeft: Int, totalSeconds: Int) {
            val endAt = System.currentTimeMillis() + secondsLeft * 1000L
            // Immediate promote from UI process
            NowBarHelper.showFocus(context, secondsLeft, totalSeconds, true, endAt)
            startFg(
                context,
                Intent(context, FocusTimerService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_SECONDS_LEFT, secondsLeft)
                    putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
                }
            )
        }

        fun resume(context: Context, secondsLeft: Int, totalSeconds: Int) {
            val left = if (secondsLeft > 0) secondsLeft else state.secondsLeft
            val total = if (totalSeconds > 0) totalSeconds else state.totalSeconds
            val endAt = System.currentTimeMillis() + left * 1000L
            NowBarHelper.showFocus(context, left, total, true, endAt)
            startFg(
                context,
                Intent(context, FocusTimerService::class.java).apply {
                    action = ACTION_RESUME
                    putExtra(EXTRA_SECONDS_LEFT, left)
                    putExtra(EXTRA_TOTAL_SECONDS, total)
                }
            )
        }

        fun pause(context: Context) {
            startFg(
                context,
                Intent(context, FocusTimerService::class.java).setAction(ACTION_PAUSE)
            )
        }

        fun stop(context: Context) {
            startFg(
                context,
                Intent(context, FocusTimerService::class.java).setAction(ACTION_STOP)
            )
        }

        private fun startFg(context: Context, intent: Intent) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "startFg", e)
                try {
                    context.startService(intent)
                } catch (e2: Exception) {
                    Log.e(TAG, "startService", e2)
                }
            }
        }
    }
}
