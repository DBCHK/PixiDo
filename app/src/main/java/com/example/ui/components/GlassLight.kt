package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * Shared key-light for Liquid Glass speculars.
 * nx/ny are -1..1: positive X is right, positive Y is down on screen.
 * Default is a ceiling light from the upper-left — the iOS 26 rest pose.
 */
@Stable
data class GlassLight(
    val nx: Float = 0.22f,
    val ny: Float = -0.38f
) {
    val asOffset: Offset get() = Offset(nx, ny)

    companion object {
        val Rest = GlassLight()
    }
}

val LocalGlassLight = compositionLocalOf { GlassLight.Rest }

/**
 * Samples gravity so chrome glass can refract like a real pane.
 * Content cards must NOT read [LocalGlassLight] — tilt would recompose the list.
 * Skipped when glass is off or Reduce Motion is on.
 */
@Composable
fun ProvideGlassLight(
    enabled: Boolean,
    reduceMotion: Boolean,
    content: @Composable () -> Unit
) {
    var light by remember { mutableStateOf(GlassLight.Rest) }
    val context = LocalContext.current

    DisposableEffect(enabled, reduceMotion) {
        if (!enabled || reduceMotion) {
            light = GlassLight.Rest
            return@DisposableEffect onDispose { }
        }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sm == null || sensor == null) {
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            private var fx = GlassLight.Rest.nx
            private var fy = GlassLight.Rest.ny
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size < 2) return
                val gx = event.values[0]
                val gz = event.values.getOrNull(2) ?: 0f
                val tx = (-gx / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                val ty = (gz / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                fx += (tx - fx) * 0.12f
                fy += (ty - fy) * 0.12f
                if (abs(fx - light.nx) + abs(fy - light.ny) > 0.028f) {
                    light = GlassLight(fx, fy)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalGlassLight provides light) {
        content()
    }
}
