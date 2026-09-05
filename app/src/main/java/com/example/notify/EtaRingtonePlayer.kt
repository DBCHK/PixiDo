package com.example.notify

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.R

/**
 * Plays PixiDo's custom sweet ETA ringtone (looping) until stopped.
 * Used when a task due-time popup appears.
 */
object EtaRingtonePlayer {

    private const val TAG = "EtaRingtone"
    private var player: MediaPlayer? = null

    fun ringtoneUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.sfx_task}")

    @Synchronized
    fun start(context: Context, loop: Boolean = true) {
        stop()
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context.applicationContext, ringtoneUri(context))
                isLooping = loop
                setVolume(0.55f, 0.55f)
                prepare()
                start()
            }
            player = mp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ETA ringtone", e)
            player = null
        }
    }

    @Synchronized
    fun stop() {
        try {
            player?.run {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (_: Exception) {
        } finally {
            player = null
        }
    }

    @Synchronized
    fun isPlaying(): Boolean = try {
        player?.isPlaying == true
    } catch (_: Exception) {
        false
    }
}
