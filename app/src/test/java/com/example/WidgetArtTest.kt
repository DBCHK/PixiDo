package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.widget.PixiWidgetProvider
import com.example.widget.WidgetArt
import com.example.widget.WidgetKind
import com.example.widget.WidgetTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WidgetArtTest {

    @Test
    fun rendersEveryKindGlassAndSolid() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        WidgetKind.entries.forEach { kind ->
            val glass = WidgetArt.render(
                ctx,
                kind,
                PixiWidgetProvider.dummy(kind),
                520,
                260,
                WidgetTheme(glass = true, dark = false)
            )
            val solid = WidgetArt.render(
                ctx,
                kind,
                PixiWidgetProvider.dummy(kind),
                520,
                260,
                WidgetTheme(glass = false, dark = true)
            )
            assertEquals(kind.name, 520, glass.width)
            assertEquals(kind.name, 260, glass.height)
            assertTrue(kind.name, glass.byteCount > 0)
            assertEquals(kind.name, solid.width, glass.width)
        }
    }
}
