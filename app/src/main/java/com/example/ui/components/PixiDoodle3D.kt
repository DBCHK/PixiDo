package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Soft 3D clay doodle with depth motion:
 *  - Continuous time-based orbit (rotation X / Y / Z + float)
 *  - Device-tilt parallax so it reacts as the user moves the phone
 *
 * Perspective comes from [graphicsLayer] cameraDistance on a transparent PNG.
 */
@Composable
fun PixiDoodle3D(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    contentDescription: String? = null,
    yawDegrees: Float = 16f,
    pitchDegrees: Float = 10f,
    tiltStrength: Float = 12f,
    orbitSeconds: Int = 7,
    float: Boolean = true,
    contentScale: ContentScale = ContentScale.Fit
) {
    // Single infinite phase — half the animation work of dual orbits
    val infinite = rememberInfiniteTransition(label = "doodle3d")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (orbitSeconds * 1000).coerceAtLeast(4000),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitPhase"
    )

    // Sensor at UI rate (not GAME) for smoother, cooler battery on 90/120 Hz
    val tilt by rememberDeviceTilt(enabled = tiltStrength > 0f)

    val angle = (phase * 2f * PI).toFloat()
    val rotY = sin(angle) * yawDegrees + tilt.x * tiltStrength
    val rotX = cos(angle * 0.85f) * pitchDegrees + tilt.y * (tiltStrength * 0.75f)
    val rotZ = sin(angle * 0.5f) * 2.5f
    val translateY = if (float) sin(angle * 1.15f) * 8f else 0f
    val translateX = if (float) cos(angle * 0.7f) * 4f else 0f
    val scale = 1f + sin(angle) * 0.03f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    rotationX = rotX
                    rotationY = rotY
                    rotationZ = rotZ
                    translationX = translateX
                    translationY = translateY
                    scaleX = scale
                    scaleY = scale
                    cameraDistance = 16f * density
                    transformOrigin = TransformOrigin.Center
                    // Compositing-friendly layer
                    alpha = 0.95f
                }
        )
    }
}

/** Normalized device tilt in roughly [-1, 1]. */
data class DeviceTilt(val x: Float = 0f, val y: Float = 0f)

@Composable
fun rememberDeviceTilt(enabled: Boolean = true): State<DeviceTilt> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(DeviceTilt()) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null) return
                    val ax = event.values.getOrNull(0) ?: 0f
                    val ay = event.values.getOrNull(1) ?: 0f
                    val nx = (-ax / 9.8f).coerceIn(-1.25f, 1.25f)
                    val ny = ((ay - 9.8f) / 9.8f).coerceIn(-1.25f, 1.25f)
                    val prev = tiltState.value
                    // Low-pass filter for smooth motion
                    tiltState.value = DeviceTilt(
                        x = prev.x * 0.82f + nx * 0.18f,
                        y = prev.y * 0.82f + ny * 0.18f
                    )
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            if (sm != null && sensor != null) {
                // UI rate is enough for gentle parallax and kinder to high-refresh panels
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose { sm?.unregisterListener(listener) }
        }
    }

    return tiltState
}
