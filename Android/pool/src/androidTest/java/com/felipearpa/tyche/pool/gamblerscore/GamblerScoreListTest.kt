package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GamblerScoreListTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun signedInRowHasNoClickSemanticsWhileAnotherGamblerNavigates() {
        val allModels = poolGamblerScoreDummyModels().take(3)
        val models = allModels.take(2)
        val pagingData = MutableStateFlow(PagingData.from(models))
        var openedGamblerId: String? = null

        composeTestRule.setContent {
            TycheTheme {
                GamblerScoreList(
                    lazyPoolGamblerScores = pagingData.collectAsLazyPagingItems(),
                    loggedInGamblerId = models.first().gamblerId,
                    modifier = Modifier.fillMaxSize(),
                    onGamblerOpen = { _, gamblerId, _ ->
                        openedGamblerId = gamblerId
                    },
                )
            }
        }

        val currentRow = composeTestRule
            .onNodeWithTag("gamblerScoreRow:${models.first().gamblerId}")
            .assertHasNoClickAction()
            .assertIsDisplayed()
        val otherRow = composeTestRule
            .onNodeWithTag("gamblerScoreRow:${models.last().gamblerId}")
            .assertHasClickAction()
            .assertIsDisplayed()

        assertTrue(
            currentRow.fetchSemanticsNode().boundsInRoot.top <
                otherRow.fetchSemanticsNode().boundsInRoot.top,
        )

        otherRow
            .performClick()

        assertEquals(models.last().gamblerId, openedGamblerId)

        pagingData.value = PagingData.from(allModels)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule
                    .onNodeWithTag("gamblerScoreRow:${allModels.last().gamblerId}")
                    .fetchSemanticsNode()
            }.isSuccess
        }

        currentRow.assertIsDisplayed()
        otherRow.assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("gamblerScoreRow:${allModels.last().gamblerId}")
            .assertIsDisplayed()
    }

    @Test
    fun currentRowAnnouncesRankIdentityYouScoreAndMovement() {
        composeTestRule.setContent {
            TycheTheme {
                GamblerScoreItem(
                    poolGamblerScore = score(
                        username = "ElGoleador",
                        position = 3,
                        beforePosition = 4,
                        score = 150,
                    ),
                    isCurrentUser = true,
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Rank 3, ElGoleador, You, 150 points, Up 1 places",
            )
            .assertIsDisplayed()
    }

    @Test
    fun missingFieldsAndLongUsernameRemainInTheRowAnnouncement() {
        val username = "A very long username that must remain available to TalkBack"
        composeTestRule.setContent {
            TycheTheme {
                GamblerScoreItem(
                    poolGamblerScore = score(
                        username = username,
                        position = null,
                        beforePosition = null,
                        score = null,
                    ),
                    isCurrentUser = false,
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Rank unavailable, $username, Points unavailable",
            )
            .assertIsDisplayed()
    }

    private fun score(
        username: String,
        position: Int?,
        beforePosition: Int?,
        score: Int?,
    ) = PoolGamblerScoreModel(
        poolId = "pool",
        poolName = "Pool",
        gamblerId = "",
        gamblerUsername = username,
        position = position,
        beforePosition = beforePosition,
        score = score,
        gamblerCount = 10,
    )
}
