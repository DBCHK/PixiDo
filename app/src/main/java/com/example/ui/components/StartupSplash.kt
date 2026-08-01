package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.ui.theme.PixiLavender
import com.example.ui.theme.PixiPink
import com.example.ui.theme.PixiYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Soft Lilac startup splash — logo pops in, brand fades up, sweet intro tone,
 * “Built by Pixipath” in a soft gradient, then the layer eases out.
 */
@Composable
fun StartupSplash(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sound = runCatching { LocalSoundEngine.current }.getOrNull()

    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(24f) }
    val creditAlpha = remember { Animatable(0f) }
    val creditOffset = remember { Animatable(16f) }
    val ringScale = remember { Animatable(0.6f) }
    val overlayAlpha = remember { Animatable(1f) }
    val overlayScale = remember { Animatable(1f) }

    var finished by remember { mutableStateOf(false) }

    // Soft gradient for “Built by Pixipath”
    val pixipathBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFC4A8F5),
                Color(0xFFFF6BA8),
                Color(0xFF67D4E8),
                Color(0xFFC4A8F5)
            )
        )
    }

    LaunchedEffect(Unit) {
        // Sweet, slow intro tone
        sound?.play(Sfx.SPLASH_INTRO)

        // Logo pop
        launch {
            logoAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
            )
        }
        launch {
            ringScale.animateTo(
                1.15f,
                tween(800, easing = FastOutSlowInEasing)
            )
        }

        delay(240)
        // Title rise
        launch {
            titleAlpha.animateTo(1f, tween(360))
        }
        launch {
            titleOffset.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 280f))
        }

        delay(280)
        // Built by Pixipath — soft delayed fade
        launch {
            creditAlpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        }
        launch {
            creditOffset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 220f))
        }

        delay(1100)

        // Exit: soft scale-up + fade
        launch {
            overlayAlpha.animateTo(0f, tween(480, easing = FastOutSlowInEasing))
        }
        launch {
            overlayScale.animateTo(1.06f, tween(480, easing = FastOutSlowInEasing))
        }
        delay(500)
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = overlayAlpha.value
                scaleX = overlayScale.value
                scaleY = overlayScale.value
            }
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Soft ambient blobs
        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer {
                    scaleX = ringScale.value
                    scaleY = ringScale.value
                    alpha = 0.35f * logoAlpha.value
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PixiLavender.copy(alpha = 0.55f),
                            PixiPink.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0f)
                        )
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PixiDoodle3D(
                resId = R.drawable.doodle_splash,
                size = 180.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
                },
                yawDegrees = 18f,
                pitchDegrees = 12f,
                orbitSeconds = 5,
                tiltStrength = 8f
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "PixiDo",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleOffset.value
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Plan · focus · grow",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value * 0.9f
                    translationY = titleOffset.value
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer { alpha = titleAlpha.value }
                    .clip(CircleShape)
                    .background(PixiYellow)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Soft gradient credit line
            Text(
                text = "Built by Pixipath",
                style = TextStyle(
                    brush = pixipathBrush,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                ),
                modifier = Modifier.graphicsLayer {
                    alpha = creditAlpha.value
                    translationY = creditOffset.value
                }
            )
        }
    }
}
