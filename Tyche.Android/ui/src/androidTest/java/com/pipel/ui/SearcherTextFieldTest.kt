package com.pipel.ui

import androidx.compose.material.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import com.pipel.core.empty
import org.junit.Rule
import org.junit.Test

class SearcherTextFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun given_a_text_when_is_typed_in_textField_then_onValueChange_is_called() {
        composeTestRule.apply {
            var isOnValueChangeCalled = false

            setContent {
                SearcherTextField(
                    value = String.empty(),
                    onValueChange = { isOnValueChangeCalled = true }) {
                    Text(text = "Enter text to filer")
                }
            }

            onNodeWithTag(testTag = "textField").performTextInput(text = "text to filter")

            assert(isOnValueChangeCalled)
        }
    }

}