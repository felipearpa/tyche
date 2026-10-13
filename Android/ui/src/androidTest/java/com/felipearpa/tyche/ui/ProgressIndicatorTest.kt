package com.felipearpa.tyche.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.felipearpa.tyche.core.type.KickoffGroupProgress
import com.felipearpa.tyche.core.type.RankMovement
import org.junit.Rule
import org.junit.Test

class ProgressIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun given_a_positive_difference_when_is_ProgressIndicator_is_shown_then_UpIndicator_is_displayed() {
        composeTestRule.setContent {
            TrendIndicator(movement = RankMovement.Moved(places = 1, partialProgress = null))
        }

        composeTestRule.onNodeWithTag(testTag = "upProgressIndicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag(testTag = "stableProgressIndicator").assertDoesNotExist()
        composeTestRule.onNodeWithTag(testTag = "downProgressIndicator").assertDoesNotExist()
    }

    @Test
    fun given_a_negative_difference_when_is_ProgressIndicator_is_shown_then_DownIndicator_is_displayed() {
        composeTestRule.apply {
            setContent {
                TrendIndicator(movement = RankMovement.Moved(places = -1, partialProgress = null))
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
                TrendIndicator(movement = RankMovement.Moved(places = 0, partialProgress = null))
            }

            onNodeWithTag(testTag = "upProgressIndicator").assertDoesNotExist()
            onNodeWithTag(testTag = "stableProgressIndicator").assertIsDisplayed()
            onNodeWithTag(testTag = "downProgressIndicator").assertDoesNotExist()
        }
    }

    @Test
    fun given_pending_movement_when_shown_then_a_label_replaces_the_steady_symbol() {
        composeTestRule.setContent { TrendIndicator(movement = RankMovement.Pending) }

        composeTestRule.onNodeWithTag(testTag = "pendingTrendIndicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag(testTag = "stableProgressIndicator").assertDoesNotExist()
    }

    @Test
    fun given_unavailable_movement_when_shown_then_a_label_replaces_the_steady_symbol() {
        composeTestRule.setContent { TrendIndicator(movement = RankMovement.Unavailable) }

        composeTestRule.onNodeWithTag(testTag = "unavailableTrendIndicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag(testTag = "stableProgressIndicator").assertDoesNotExist()
    }

    @Test
    fun given_a_partial_group_when_shown_then_the_computed_count_follows_the_movement() {
        composeTestRule.setContent {
            TrendIndicator(
                movement = RankMovement.Moved(
                    places = 3,
                    partialProgress = KickoffGroupProgress(computedMatches = 1, totalMatches = 2),
                ),
            )
        }

        composeTestRule.onNodeWithTag(testTag = "upProgressIndicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 of 2 computed").assertIsDisplayed()
    }
}
