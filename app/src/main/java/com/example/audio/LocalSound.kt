package com.example.audio

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalSoundEngine = staticCompositionLocalOf<SoundEngine> {
    error("SoundEngine not provided")
}

@Composable
fun ProvideSoundEngine(
    enabled: Boolean,
    hapticsEnabled: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val engine = remember { SoundEngine.get(context) }
    engine.enabled = enabled
    engine.hapticsEnabled = hapticsEnabled
    CompositionLocalProvider(LocalSoundEngine provides engine, content = content)
}

@Composable
fun rememberSfx(): (Sfx) -> Unit {
    val engine = LocalSoundEngine.current
    return remember(engine) { { sfx: Sfx -> engine.play(sfx) } }
}

@Composable
fun rememberSoundEngine(): SoundEngine = LocalSoundEngine.current
