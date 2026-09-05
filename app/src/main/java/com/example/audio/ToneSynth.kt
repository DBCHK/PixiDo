package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/** One mallet/bell hit. */
internal data class Tone(
    val hz: Double,
    val startMs: Int = 0,
    val durMs: Int = 280,
    val vel: Double = 0.4,
    val bright: Double = 0.4
)

/**
 * Soft glass-mallet synthesizer — warm sine body, a hint of FM wood at the
 * attack, stereo chorus, and a short early reflection. Designed for UI chimes
 * rather than thin additive piano.
 */
internal object ToneSynth {
    const val SAMPLE_RATE = 44100

    fun render(tones: List<Tone>, peak: Double = 0.26): ShortArray {
        if (tones.isEmpty()) return ShortArray(0)
        var endMs = 0
        for (t in tones) endMs = max(endMs, t.startMs + t.durMs)
        val n = ((endMs + 48) * SAMPLE_RATE) / 1000
        val left = DoubleArray(n)
        val right = DoubleArray(n)
        for (tone in tones) renderTone(tone, left, right)
        addReflection(left, right)
        return interleave(left, right, peak)
    }

    private fun renderTone(tone: Tone, left: DoubleArray, right: DoubleArray) {
        val start = (tone.startMs * SAMPLE_RATE) / 1000
        val len = (tone.durMs * SAMPLE_RATE) / 1000
        if (len <= 0) return
        val end = min(left.size, start + len)
        val attack = 180.0
        val bodyDecay = 3.1
        val highDecay = 7.4
        val fmDecay = 11.0
        val cents = 6.5
        val fL = tone.hz * centsToRatio(-cents)
        val fR = tone.hz * centsToRatio(cents)
        writeVoice(
            buf = left,
            from = start,
            to = end,
            hz = fL,
            vel = tone.vel,
            bright = tone.bright,
            attack = attack,
            bodyDecay = bodyDecay,
            highDecay = highDecay,
            fmDecay = fmDecay
        )
        writeVoice(
            buf = right,
            from = start,
            to = end,
            hz = fR,
            vel = tone.vel,
            bright = tone.bright,
            attack = attack,
            bodyDecay = bodyDecay,
            highDecay = highDecay,
            fmDecay = fmDecay
        )
    }

    private fun writeVoice(
        buf: DoubleArray,
        from: Int,
        to: Int,
        hz: Double,
        vel: Double,
        bright: Double,
        attack: Double,
        bodyDecay: Double,
        highDecay: Double,
        fmDecay: Double
    ) {
        val sr = SAMPLE_RATE.toDouble()
        var ph = 0.0
        var ph2 = 0.0
        var ph3 = 0.0
        var modPh = 0.0
        val inc = 2.0 * PI * hz / sr
        val inc2 = 2.0 * PI * hz * 2.003 / sr
        val inc3 = 2.0 * PI * hz * 3.01 / sr
        val modInc = 2.0 * PI * hz * 2.0 / sr
        val amp2 = 0.22 * bright
        val amp3 = 0.07 * bright
        var lp = 0.0
        val lpCoeff = 0.28 + bright * 0.22
        val n = (to - from).coerceAtLeast(1)
        for (i in from until to) {
            val t = (i - from) / sr
            val att = 1.0 - exp(-t * attack)
            val idx = bright * 0.65 * exp(-t * fmDecay)
            val fm = idx * sin(modPh)
            modPh += modInc
            var s = sin(ph + fm) * exp(-t * bodyDecay)
            s += sin(ph2) * amp2 * exp(-t * highDecay)
            s += sin(ph3) * amp3 * exp(-t * (highDecay + 3.0))
            ph += inc
            ph2 += inc2
            ph3 += inc3
            s *= att * vel
            lp += lpCoeff * (s - lp)
            val x = (i - from).toDouble() / n
            val rel = if (x > 0.62) {
                0.5 * (1.0 + cos(PI * ((x - 0.62) / 0.38)))
            } else 1.0
            buf[i] += tanh(lp * 1.35) * rel
        }
    }

    private fun addReflection(left: DoubleArray, right: DoubleArray) {
        val d1 = (0.014 * SAMPLE_RATE).toInt()
        val d2 = (0.029 * SAMPLE_RATE).toInt()
        if (d1 <= 0 || d1 >= left.size) return
        val srcL = left.copyOf()
        val srcR = right.copyOf()
        for (i in d1 until left.size) {
            left[i] += srcL[i - d1] * 0.11
            right[i] += srcR[i - d1] * 0.09
            if (i >= d2) {
                left[i] += srcL[i - d2] * 0.05
                right[i] += srcR[i - d2] * 0.06
            }
        }
    }

    private fun interleave(left: DoubleArray, right: DoubleArray, peak: Double): ShortArray {
        var maxAbs = 1e-9
        for (i in left.indices) {
            val a = kotlin.math.abs(left[i])
            val b = kotlin.math.abs(right[i])
            if (a > maxAbs) maxAbs = a
            if (b > maxAbs) maxAbs = b
        }
        val g = peak / maxAbs
        val out = ShortArray(left.size * 2)
        for (i in left.indices) {
            out[i * 2] = toShort(left[i] * g)
            out[i * 2 + 1] = toShort(right[i] * g)
        }
        return out
    }

    private fun toShort(v: Double): Short {
        val x = v.coerceIn(-1.0, 1.0)
        return (x * Short.MAX_VALUE).toInt().toShort()
    }

    private fun centsToRatio(cents: Double): Double = Math.pow(2.0, cents / 1200.0)
}
