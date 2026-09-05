package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.example.ui.components.TwoStepSwipeBox
import com.example.ui.theme.PixiDoTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class TwoStepSwipeComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun completeRequiresTwoRightSwipes() {
        var complete = 0
        var delete = 0
        composeTestRule.setContent {
            PixiDoTheme {
                TwoStepSwipeBox(
                    onCommitStartToEnd = { complete++ },
                    onCommitEndToStart = { delete++ },
                    startToEndColor = Color.Green,
                    endToStartColor = Color.Red,
                    startToEndIcon = Icons.Filled.Check,
                    endToStartIcon = Icons.Filled.Delete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .testTag("swipe_target")
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("swipe_target").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertEquals(0, complete)
        assertEquals(0, delete)

        composeTestRule.onNodeWithTag("swipe_target").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertEquals(1, complete)
        assertEquals(0, delete)
    }

    @Test
    fun deleteRequiresTwoLeftSwipes() {
        var complete = 0
        var delete = 0
        composeTestRule.setContent {
            PixiDoTheme {
                TwoStepSwipeBox(
                    onCommitStartToEnd = { complete++ },
                    onCommitEndToStart = { delete++ },
                    startToEndColor = Color.Green,
                    endToStartColor = Color.Red,
                    startToEndIcon = Icons.Filled.Check,
                    endToStartIcon = Icons.Filled.Delete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .testTag("swipe_target")
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("swipe_target").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals(0, complete)
        assertEquals(0, delete)

        composeTestRule.onNodeWithTag("swipe_target").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals(0, complete)
        assertEquals(1, delete)
    }
}
