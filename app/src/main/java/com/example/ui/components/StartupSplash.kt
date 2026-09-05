package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Startup splash — interactive brand mark draws in (arc, check, green cap),
 * title rises, then the layer eases out. Tap anywhere except the logo to skip.
 */
@Composable
fun StartupSplash(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sound = runCatching { LocalSoundEngine.current }.getOrNull()

    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(24f) }
    val creditAlpha = remember { Animatable(0f) }
    val creditOffset = remember { Animatable(16f) }
    val ringScale = remember { Animatable(0.6f) }
    val overlayAlpha = remember { Animatable(1f) }
    val overlayScale = remember { Animatable(1f) }

    var finished by remember { mutableStateOf(false) }

    fun finish() {
        if (finished) return
        finished = true
        onFinished()
    }

    val pixipathBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF06B6D4),
                Color(0xFF10B981),
                Color(0xFF67D4E8),
                Color(0xFF06B6D4)
            )
        )
    }

    LaunchedEffect(Unit) {
        sound?.play(Sfx.SPLASH_INTRO)

        launch {
            ringScale.animateTo(
                1.18f,
                tween(900, easing = FastOutSlowInEasing)
            )
        }

        delay(260)
        launch { titleAlpha.animateTo(1f, tween(360)) }
        launch {
            titleOffset.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 280f))
        }

        delay(280)
        launch {
            creditAlpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        }
        launch {
            creditOffset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 220f))
        }

        delay(1200)

        launch {
            overlayAlpha.animateTo(0f, tween(480, easing = FastOutSlowInEasing))
        }
        launch {
            overlayScale.animateTo(1.06f, tween(480, easing = FastOutSlowInEasing))
        }
        delay(500)
        finish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = overlayAlpha.value
                scaleX = overlayScale.value
                scaleY = overlayScale.value
            }
            .background(PixiLogoBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { finish() }
            )
            .testTag("startup_splash"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer {
                    scaleX = ringScale.value
                    scaleY = ringScale.value
                    alpha = 0.42f * titleAlpha.value.coerceAtLeast(0.35f)
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PixiLogoCyan.copy(alpha = 0.42f),
                            PixiLogoGreen.copy(alpha = 0.16f),
                            PixiLogoBg.copy(alpha = 0f)
                        )
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PixiBrandLogo(
                size = 168.dp,
                animated = true,
                onTap = { sound?.play(Sfx.TAP_SOFT) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "PixiDo",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
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
                color = Color.White.copy(alpha = 0.62f),
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
                    .background(PixiLogoGreen)
            )

            Spacer(modifier = Modifier.height(20.dp))

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
