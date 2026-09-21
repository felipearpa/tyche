package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.ui.theme.TycheTheme
import org.junit.Rule
import org.junit.Test

/**
 * The "My pools" row announces one logical standing per pool, composed from
 * [PoolGamblerScoreModel] rather than scraped from the abbreviated visible copy.
 *
 * The row clears its subtree, so the rank tile's bare "4" and the trend indicator's bare
 * "1" never reach TalkBack on their own; everything is asserted through the single
 * content description. Unlike the leaderboard row this one leads with the pool name.
 * The open label, the button role, and the invite custom action live on the clickable
 * wrapper in `PoolScoreList` and are covered by [PoolScoreListAccessibilityTest].
 */
class PoolScoreItemAccessibilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun completeRowAnnouncesNameRankPointsMembersAndMovement() {
        // position 4 from beforePosition 3 is a one-place drop.
        renderRow(model(position = 4, beforePosition = 3, score = 8, gamblerCount = 101))

        composeTestRule
            .onNodeWithContentDescription(
                "Neptune World Series 2023, Rank 4, 8 points, 101 members, Down 1 place",
            )
            .assertIsDisplayed()
    }

    @Test
    fun missingRankAndScoreAreAnnouncedAsUnavailable() {
        // The pool is still identified and counted; with no rank there is nothing to
        // compare, so no movement is announced.
        renderRow(model(position = null, beforePosition = null, score = null, gamblerCount = 101))

        composeTestRule
            .onNodeWithContentDescription(
                "Neptune World Series 2023, Rank unavailable, Points unavailable, 101 members",
            )
            .assertIsDisplayed()
    }

    @Test
    fun rankWithoutAPreviousRankAnnouncesNoMovement() {
        renderRow(model(position = 4, beforePosition = null, score = 8, gamblerCount = 101))

        composeTestRule
            .onNodeWithContentDescription("Neptune World Series 2023, Rank 4, 8 points, 101 members")
            .assertIsDisplayed()
    }

    @Test
    fun unchangedRankAnnouncesTheSteadyPhraseLast() {
        renderRow(model(position = 4, beforePosition = 4, score = 8, gamblerCount = 101))

        composeTestRule
            .onNodeWithContentDescription(
                "Neptune World Series 2023, Rank 4, 8 points, 101 members, Rank unchanged",
            )
            .assertIsDisplayed()
    }

    @Test
    fun missingMemberCountIsOmittedRatherThanAnnouncedEmpty() {
        renderRow(model(position = 4, beforePosition = 3, score = 8, gamblerCount = null))

        composeTestRule
            .onNodeWithContentDescription("Neptune World Series 2023, Rank 4, 8 points, Down 1 place")
            .assertIsDisplayed()
    }

    @Test
    fun singularValuesUseSingularNouns() {
        // n == 1 is the modal case on a board that updates every match: one point,
        // one member, one place moved. Each noun must agree with its count.
        renderRow(model(position = 3, beforePosition = 4, score = 1, gamblerCount = 1))

        composeTestRule
            .onNodeWithContentDescription(
                "Neptune World Series 2023, Rank 3, 1 point, 1 member, Up 1 place",
            )
            .assertIsDisplayed()
    }

    @Test
    fun pluralValuesUsePluralNouns() {
        renderRow(model(position = 3, beforePosition = 5, score = 2, gamblerCount = 2))

        composeTestRule
            .onNodeWithContentDescription(
                "Neptune World Series 2023, Rank 3, 2 points, 2 members, Up 2 places",
            )
            .assertIsDisplayed()
    }

    @Test
    fun zeroValuesUsePluralNouns() {
        // English and Spanish both take the "other" category at zero.
        renderRow(model(position = 3, beforePosition = 3, score = 0, gamblerCount = 0))

        composeTestRule
            .onNodeWithContentDescription(
                "Neptune World Series 2023, Rank 3, 0 points, 0 members, Rank unchanged",
            )
            .assertIsDisplayed()
    }

    @Test
    fun placeholderRowExposesNoPoolContent() {
        composeTestRule.setContent {
            TycheTheme {
                PoolScorePlaceholderItem(modifier = Modifier.fillMaxWidth())
            }
        }

        // `poolGamblerScorePlaceholderModel()` fills the row with repeated "X" characters;
        // none of it may reach TalkBack, as text or as a description.
        composeTestRule.onAllNodesWithText("X".repeat(25)).assertCountEquals(0)
        composeTestRule
            .onAllNodesWithContentDescription("X".repeat(25), substring = true)
            .assertCountEquals(0)
    }

    private fun renderRow(poolGamblerScore: PoolGamblerScoreModel) {
        composeTestRule.setContent {
            TycheTheme {
                PoolScoreItem(
                    poolGamblerScore = poolGamblerScore,
                    onJoin = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    private fun model(
        position: Int?,
        beforePosition: Int?,
        score: Int?,
        gamblerCount: Int?,
    ) = PoolGamblerScoreModel(
        poolId = "A3C2E1",
        poolName = "Neptune World Series 2023",
        gamblerId = "YF23H1",
        gamblerUsername = "neptune-player",
        position = position,
        beforePosition = beforePosition,
        score = score,
        gamblerCount = gamblerCount,
    )
}
