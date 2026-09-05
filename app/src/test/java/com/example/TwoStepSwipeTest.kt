package com.example

import com.example.ui.components.TwoStepAnchor
import com.example.ui.components.TwoStepPeekFraction
import com.example.ui.components.rubberBand
import com.example.ui.components.settleTwoStepSwipe
import com.example.ui.components.twoStepAnchorOffset
import com.example.ui.components.twoStepPeekPx
import com.example.ui.components.twoStepRubberOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TwoStepSwipeTest {

    private val width = 400f
    private val peek = width * TwoStepPeekFraction
    private val fling = 2000f

    @Test
    fun peekIsHalfway() {
        assertEquals(200f, twoStepPeekPx(width), 0.01f)
        assertEquals(TwoStepPeekFraction, 0.5f, 0.0f)
    }

    @Test
    fun smallSwipeFromRestSnapsBack() {
        val next = settleTwoStepSwipe(
            offset = width * 0.12f,
            velocity = 0f,
            width = width,
            settled = TwoStepAnchor.Rest,
            velocityThreshold = 800f
        )
        assertEquals(TwoStepAnchor.Rest, next)
    }

    @Test
    fun firstSwipeRightPeeksAndNeverCompletes() {
        val dragged = settleTwoStepSwipe(
            offset = width * 0.8f,
            velocity = 0f,
            width = width,
            settled = TwoStepAnchor.Rest,
            velocityThreshold = 800f
        )
        val flung = settleTwoStepSwipe(
            offset = width * 0.9f,
            velocity = fling,
            width = width,
            settled = TwoStepAnchor.Rest,
            velocityThreshold = 800f
        )
        assertEquals(TwoStepAnchor.PeekComplete, dragged)
        assertEquals(TwoStepAnchor.PeekComplete, flung)
    }

    @Test
    fun firstSwipeLeftPeeksAndNeverDeletes() {
        val dragged = settleTwoStepSwipe(
            offset = -width * 0.8f,
            velocity = 0f,
            width = width,
            settled = TwoStepAnchor.Rest,
            velocityThreshold = 800f
        )
        val flung = settleTwoStepSwipe(
            offset = -width * 0.9f,
            velocity = -fling,
            width = width,
            settled = TwoStepAnchor.Rest,
            velocityThreshold = 800f
        )
        assertEquals(TwoStepAnchor.PeekDelete, dragged)
        assertEquals(TwoStepAnchor.PeekDelete, flung)
    }

    @Test
    fun secondSwipeFromPeekCompletes() {
        val next = settleTwoStepSwipe(
            offset = peek + (width - peek) * 0.6f,
            velocity = 0f,
            width = width,
            settled = TwoStepAnchor.PeekComplete,
            velocityThreshold = 800f
        )
        assertEquals(TwoStepAnchor.Complete, next)
    }

    @Test
    fun secondSwipeFromPeekDeletes() {
        val next = settleTwoStepSwipe(
            offset = -peek - (width - peek) * 0.6f,
            velocity = 0f,
            width = width,
            settled = TwoStepAnchor.PeekDelete,
            velocityThreshold = 800f
        )
        assertEquals(TwoStepAnchor.Delete, next)
    }

    @Test
    fun reverseSwipeFromPeekCancels() {
        assertEquals(
            TwoStepAnchor.Rest,
            settleTwoStepSwipe(
                offset = peek * 0.2f,
                velocity = 0f,
                width = width,
                settled = TwoStepAnchor.PeekComplete,
                velocityThreshold = 800f
            )
        )
        assertEquals(
            TwoStepAnchor.Rest,
            settleTwoStepSwipe(
                offset = -peek * 0.2f,
                velocity = 0f,
                width = width,
                settled = TwoStepAnchor.PeekDelete,
                velocityThreshold = 800f
            )
        )
    }

    @Test
    fun restRubberNeverReachesFullDismiss() {
        val right = twoStepRubberOffset(width, TwoStepAnchor.Rest, width)
        val left = twoStepRubberOffset(-width, TwoStepAnchor.Rest, width)
        assertTrue(right < width * 0.8f)
        assertTrue(right > peek)
        assertTrue(left > -width * 0.8f)
        assertTrue(left < -peek)
    }

    @Test
    fun rubberBandIsLessThanOvershoot() {
        val over = 120f
        val band = rubberBand(over, 100f)
        assertTrue(band < over)
        assertTrue(band > 0f)
        assertEquals(0f, rubberBand(0f, 100f), 0.0f)
    }

    @Test
    fun anchorOffsetsMatchPeekAndEdges() {
        assertEquals(0f, twoStepAnchorOffset(TwoStepAnchor.Rest, width), 0.01f)
        assertEquals(peek, twoStepAnchorOffset(TwoStepAnchor.PeekComplete, width), 0.01f)
        assertEquals(-peek, twoStepAnchorOffset(TwoStepAnchor.PeekDelete, width), 0.01f)
        assertEquals(width, twoStepAnchorOffset(TwoStepAnchor.Complete, width), 0.01f)
        assertEquals(-width, twoStepAnchorOffset(TwoStepAnchor.Delete, width), 0.01f)
    }
}
