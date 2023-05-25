package com.felipearpa.ui

import androidx.compose.material3.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import junit.framework.TestCase.assertFalse
import org.junit.Rule
import org.junit.Test

class GestureDisableModifierTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun given_a_component_with_disable_gestures_when_is_clicked_then_click_action_is_not_performed() {
        var isClicked = false

        composeTestRule.setContent {
            Button(
                onClick = { isClicked = true },
                modifier = Modifier
                    .disabledGestures(disabled = true)
                    .testTag("button")
            ) {

            }
        }

        composeTestRule.onNodeWithTag(testTag = "button").performClick()

        assertFalse(isClicked)
    }
}