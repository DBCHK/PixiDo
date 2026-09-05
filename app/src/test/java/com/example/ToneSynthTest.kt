package com.example

import com.example.audio.Sfx
import com.example.audio.SoundEngine
import com.example.audio.Tone
import com.example.audio.ToneSynth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ToneSynthTest {

    @Test
    fun tapIsShortAndAudible() {
        val pcm = ToneSynth.render(
            listOf(Tone(hz = 659.25, durMs = 220, vel = 0.32, bright = 0.28))
        )
        assertTrue(pcm.size > 1000)
        val frames = pcm.size / 2
        val durMs = frames * 1000.0 / ToneSynth.SAMPLE_RATE
        assertTrue("tap should stay under 400ms, was $durMs", durMs < 400)
        assertTrue(peak(pcm) > 1000)
    }

    @Test
    fun completeHasRisingLength() {
        val pcm = ToneSynth.render(SoundEngine.gesture(Sfx.TASK_COMPLETE))
        val durMs = pcm.size / 2 * 1000.0 / ToneSynth.SAMPLE_RATE
        assertTrue("complete should linger a bit, was $durMs", durMs in 500.0..1200.0)
        assertTrue(peak(pcm) > 2000)
    }

    @Test
    fun everyGestureRenders() {
        for (sfx in Sfx.entries) {
            if (sfx == Sfx.SPLASH_INTRO) {
                val pcm = ToneSynth.render(SoundEngine.gesture(sfx))
                assertTrue("$sfx was silent", pcm.size > 200)
                assertTrue("$sfx peak too low", peak(pcm) > 500)
            } else {
                assertTrue("$sfx missing sample", SoundEngine.sampleRes(sfx) != 0)
            }
        }
    }

    @Test
    fun stereoInterleavedEvenLength() {
        val pcm = ToneSynth.render(SoundEngine.gesture(Sfx.NOTIF_SOFT), peak = 0.4)
        assertEquals(0, pcm.size % 2)
    }

    private fun peak(pcm: ShortArray): Int {
        var m = 0
        for (s in pcm) m = maxOf(m, abs(s.toInt()))
        return m
    }
}
