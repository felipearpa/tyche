package com.felipearpa.tyche.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import com.felipearpa.tyche.core.emptyString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DelayedTextFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun given_a_textField_when_is_typed_then_onValueChange_is_called() {
        val touchTypedText = "text"
        var delayedNewValue: String? = null

        composeTestRule.setContent {
            DelayedTextField(
                value = emptyString(),
                onValueChange = { newValue ->
                    delayedNewValue = newValue
                },
                onDelayedValueChange = {}
            ) {}
        }

        composeTestRule.onNodeWithTag(testTag = "textField").performTextInput(text = touchTypedText)

        assertEquals(touchTypedText, delayedNewValue)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun given_a_textField_when_is_typed_then_onValueDelayedChange_is_called_after_a_delay_time() =
        runTest {
            val touchTypedText = "text"
            var delayedNewValue: String? = null
            val startTime: Long = System.currentTimeMillis()
            var endTime: Long? = null

            composeTestRule.setContent {
                DelayedTextField(
                    value = emptyString(),
                    onValueChange = {

                    },
                    onDelayedValueChange = { newValue ->
                        endTime = System.currentTimeMillis()
                        delayedNewValue = newValue
                    }
                ) {}
            }

            composeTestRule.onNodeWithTag(testTag = "textField")
                .performTextInput(text = touchTypedText)

            composeTestRule.waitUntil(timeoutMillis = 700) {
                endTime != null
            }

            Assert.assertNotNull(endTime)
            assert(endTime!! >= startTime + 700)

            assertEquals(touchTypedText, delayedNewValue)
        }
}