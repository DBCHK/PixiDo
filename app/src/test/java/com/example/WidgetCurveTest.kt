package com.example

import com.example.widget.WidgetCurve
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetCurveTest {

    @Test
    fun emptyPointsYieldsEmptyPath() {
        val path = WidgetCurve.path(emptyList())
        assertTrue(path.isEmpty)
    }

    @Test
    fun twoPointsBuildACubic() {
        val path = WidgetCurve.path(listOf(0f to 10f, 40f to 4f, 80f to 18f))
        assertFalse(path.isEmpty)
        val fill = WidgetCurve.fillUnder(path, 0f, 80f, 20f)
        assertFalse(fill.isEmpty)
    }
}
