package com.felipearpa.tyche.bet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import com.felipearpa.tyche.bet.match.MatchGamblerBetItem
import com.felipearpa.tyche.bet.match.MatchHeader
import com.felipearpa.tyche.bet.pending.PendingBetItem
import com.felipearpa.tyche.bet.pending.PendingBetItemViewState
import com.felipearpa.tyche.bet.pending.partialPoolGamblerBetFakeModel
import com.felipearpa.tyche.ui.theme.TycheTheme
import org.junit.Rule
import org.junit.Test

/**
 * Bet list placeholders are production rows filled with `poolGamblerBetFakeModel()` and
 * rendered with `isPlaceholder = true`. Their filler (runs of "X" for names, 100–100 scores, +10 points) must not
 * reach a screen reader as if it were a bet, and the rows must not be actionable.
 */
class BetPlaceholderAccessibilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pendingBetPlaceholderExposesNoValuesOrActions() {
        assertInert { modifier ->
            PendingBetItem(
                poolGamblerBet = poolGamblerBetFakeModel(),
                viewState = PendingBetItemViewState.Visualization(partialPoolGamblerBetFakeModel()),
                modifier = modifier,
                isPlaceholder = true,
            )
        }
    }

    @Test
    fun matchGamblerBetPlaceholderExposesNoValuesOrActions() {
        assertInert { modifier ->
            MatchGamblerBetItem(
                poolGamblerBet = poolGamblerBetFakeModel(),
                modifier = modifier,
                isPlaceholder = true,
            )
        }
    }

    @Test
    fun matchHeaderPlaceholderExposesNoValuesOrActions() {
        assertInert { modifier ->
            MatchHeader(
                bet = poolGamblerBetFakeModel().copy(isLocked = false, isComputed = false),
                modifier = modifier,
                isPlaceholder = true,
            )
        }
    }

    private fun assertInert(placeholder: @Composable (Modifier) -> Unit) {
        composeTestRule.setContent {
            TycheTheme {
                placeholder(Modifier.fillMaxWidth().testTag(PLACEHOLDER_TAG))
            }
        }

        composeTestRule.onAllNodesWithText("X", substring = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("100", substring = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("+10", substring = true).assertCountEquals(0)
        composeTestRule.onNodeWithTag(PLACEHOLDER_TAG).assertHasNoClickAction()
    }
}

private const val PLACEHOLDER_TAG = "betPlaceholder"
