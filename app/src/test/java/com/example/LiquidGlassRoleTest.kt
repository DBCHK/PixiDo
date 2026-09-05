package com.example

import com.example.ui.components.PixiGlassRole
import com.example.ui.components.PixiGlassWeight
import com.example.ui.components.resolvePixiGlassRole
import org.junit.Assert.assertEquals
import org.junit.Test

class LiquidGlassRoleTest {

    @Test
    fun explicitRoleWins() {
        assertEquals(
            PixiGlassRole.Content,
            resolvePixiGlassRole(PixiGlassRole.Content, liquid = true, PixiGlassWeight.Sheet)
        )
    }

    @Test
    fun sheetWeightMapsToSheet() {
        assertEquals(
            PixiGlassRole.Sheet,
            resolvePixiGlassRole(null, liquid = false, PixiGlassWeight.Sheet)
        )
    }

    @Test
    fun liquidChromeByDefault() {
        assertEquals(
            PixiGlassRole.Chrome,
            resolvePixiGlassRole(null, liquid = true, PixiGlassWeight.Bar)
        )
    }

    @Test
    fun contentWhenNeither() {
        assertEquals(
            PixiGlassRole.Content,
            resolvePixiGlassRole(null, liquid = false, PixiGlassWeight.Bar)
        )
    }
}
