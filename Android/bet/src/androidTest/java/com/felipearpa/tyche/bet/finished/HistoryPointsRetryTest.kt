package com.felipearpa.tyche.bet.finished

import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.kotest.matchers.shouldBe
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * The points summary's retry, for both owners and both failures: a secondary icon-only button
 * inline after the failure text, named "Retry", with a 48 dp target, that requests only the total.
 */
class HistoryPointsRetryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun personalInitialFailure() {
        assertInlineIconRetry(
            state = HistoryPointsSummaryState.initial.failed(),
            owner = HistoryOwner.SignedInGambler,
            message = "Couldn't load your points.",
        )
    }

    @Test
    fun personalRefreshFailureKeepsTheLastTotal() {
        assertInlineIconRetry(
            state = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(589)).failed(),
            owner = HistoryOwner.SignedInGambler,
            message = "Couldn't update your points. Showing the last total.",
        )
        composeTestRule.onNodeWithContentDescription("589 points earned in this pool").assertExists()
    }

    @Test
    fun selectedGamblerInitialFailure() {
        assertInlineIconRetry(
            state = HistoryPointsSummaryState.initial.failed(),
            owner = SELECTED,
            message = "Couldn't load points.",
        )
    }

    @Test
    fun selectedGamblerRefreshFailureKeepsTheLastTotal() {
        assertInlineIconRetry(
            state = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(676)).failed(),
            owner = SELECTED,
            message = "Couldn't update points. Showing the last total.",
        )
        composeTestRule.onNodeWithContentDescription("El mono earned 676 points in this pool").assertExists()
    }

    @Test
    fun theRetryIsNamedInSpanish() {
        render(state = HistoryPointsSummaryState.initial.failed(), owner = SELECTED, locale = SPANISH, onRetry = {})

        composeTestRule.onNodeWithContentDescription("Reintentar").assertExists()
    }

    @Test
    fun aCurrentTotalOffersNoRetry() {
        render(
            state = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(4)),
            owner = HistoryOwner.SignedInGambler,
            onRetry = {},
        )

        composeTestRule.onAllNodesWithText("Retry").assertCountEquals(0)
        composeTestRule.onNodeWithContentDescription("Retry").assertDoesNotExist()
    }

    private fun assertInlineIconRetry(state: HistoryPointsSummaryState, owner: HistoryOwner, message: String) {
        var retries = 0
        render(state = state, owner = owner, onRetry = { retries++ })

        val retry = composeTestRule.onNodeWithContentDescription("Retry")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchWidthIsEqualTo(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
        // Icon-only: no visible "Retry" label.
        composeTestRule.onAllNodesWithText("Retry").assertCountEquals(0)

        val text = composeTestRule.onNodeWithText(message).getUnclippedBoundsInRoot()
        val icon = retry.getUnclippedBoundsInRoot()
        assertTrue("The retry starts after the text ($icon, $text)", icon.left >= text.right)
        assertTrue(
            "The retry shares the text's line ($icon, $text)",
            icon.top < text.bottom && icon.bottom > text.top,
        )

        retry.performClick()
        retries shouldBe 1
    }

    private fun render(
        state: HistoryPointsSummaryState,
        owner: HistoryOwner,
        onRetry: () -> Unit,
        locale: Locale = Locale.US,
    ) {
        composeTestRule.setContent {
            Localized(locale) {
                TycheTheme {
                    Surface {
                        HistoryPointsHeader(state = state, onRetry = onRetry, owner = owner)
                    }
                }
            }
        }
    }
}

private val SELECTED = HistoryOwner.SelectedGambler(name = "El mono")
private val SPANISH = Locale.forLanguageTag("es-CO")
