package com.pipel.tyche

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pipel.tyche.ui.ProgressIndicator
import org.junit.Rule
import org.junit.Test

class ProgressIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun given_a_positive_difference_when_is_ProgressIndicator_is_shown_then_UpIndicator_is_displayed() {
        composeTestRule.apply {
            setContent {
                ProgressIndicator(difference = 1)
            }

            onNodeWithTag(testTag = "upProgressIndicator").assertIsDisplayed()
            onNodeWithTag(testTag = "stableProgressIndicator").assertDoesNotExist()
            onNodeWithTag(testTag = "downProgressIndicator").assertDoesNotExist()
        }
    }

    @Test
    fun given_a_negative_difference_when_is_ProgressIndicator_is_shown_then_DownIndicator_is_displayed() {
        composeTestRule.apply {
            setContent {
                ProgressIndicator(difference = -1)
            }

            onNodeWithTag(testTag = "upProgressIndicator").assertDoesNotExist()
            onNodeWithTag(testTag = "stableProgressIndicator").assertDoesNotExist()
            onNodeWithTag(testTag = "downProgressIndicator").assertIsDisplayed()
        }
    }

    @Test
    fun given_a_zero_difference_when_is_ProgressIndicator_is_shown_then_StableIndicator_is_displayed() {
        composeTestRule.apply {
            setContent {
                ProgressIndicator(difference = 0)
            }

            onNodeWithTag(testTag = "upProgressIndicator").assertDoesNotExist()
            onNodeWithTag(testTag = "stableProgressIndicator").assertIsDisplayed()
            onNodeWithTag(testTag = "downProgressIndicator").assertDoesNotExist()
        }
    }

}