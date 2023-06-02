package com.felipearpa.tyche.bet.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.felipearpa.tyche.core.emptyString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class BetTextFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun given_a_valid_value_when_is_typed_then_onValueChange_is_called() {
        val touchTypedText = "1"
        var newValue: String? = null

        composeTestRule.setContent {
            BetTextField(
                value = emptyString(),
                onValueChange = { value ->
                    newValue = value
                },
                onDelayedValueChange = {}
            )
        }

        composeTestRule.onNodeWithTag(testTag = "textField").performTextInput(text = touchTypedText)

        assertEquals(touchTypedText, newValue)
    }

    @Test
    fun given_a_empty_value_when_is_typed_then_onValueChange_is_called_with_zero() {
        var newValue: String? = null

        composeTestRule.setContent {
            BetTextField(
                value = "1",
                onValueChange = { value ->
                    newValue = value
                },
                onDelayedValueChange = {}
            )
        }

        composeTestRule.onNodeWithTag(testTag = "textField").performTextClearance()

        assertEquals("0", newValue)
    }

    @Test
    fun given_a_invalid_value_when_is_typed_then_onValueChange_is_not_called() {
        val touchTypedText = "hello"
        var newValue: String? = null

        composeTestRule.setContent {
            BetTextField(
                value = emptyString(),
                onValueChange = { value ->
                    newValue = value
                },
                onDelayedValueChange = {}
            )
        }

        composeTestRule.onNodeWithTag(testTag = "textField").performTextInput(text = touchTypedText)

        assertNull(newValue)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun given_a_valid_value_when_is_typed_then_onValueDelayedChange_is_called_after_a_delay_time() =
        runTest {
            val touchTypedText = "1"
            var delayedNewValue: String? = null
            val startTime: Long = System.currentTimeMillis()
            var endTime: Long? = null

            composeTestRule.setContent {
                BetTextField(
                    value = emptyString(),
                    onValueChange = {

                    },
                    onDelayedValueChange = { newValue ->
                        endTime = System.currentTimeMillis()
                        delayedNewValue = newValue
                    }
                )
            }

            composeTestRule.onNodeWithTag(testTag = "textField")
                .performTextInput(text = touchTypedText)

            composeTestRule.waitUntil(timeoutMillis = 700) {
                endTime != null
            }

            assertNotNull(endTime)
            assert(endTime!! >= startTime + 700)

            assertEquals(touchTypedText, delayedNewValue)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun given_a_invalid_value_when_is_typed_then_onValueDelayedChange_is_not_called_after_a_delay_time() =
        runTest {
            val touchTypedText = "hello"
            var endTime: Long? = null

            composeTestRule.setContent {
                BetTextField(
                    value = emptyString(),
                    onValueChange = {

                    },
                    onDelayedValueChange = {
                        endTime = System.currentTimeMillis()
                    }
                )
            }

            composeTestRule.onNodeWithTag(testTag = "textField")
                .performTextInput(text = touchTypedText)

            composeTestRule.waitUntil(timeoutMillis = 700) {
                endTime == null
            }

            assertNull(endTime)
        }
}