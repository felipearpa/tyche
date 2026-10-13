package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.core.type.KickoffGroupProgress
import com.felipearpa.tyche.pool.AvatarImageStoreKoinRule
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GamblerScoreListTest {
    @get:Rule(order = 0)
    val avatarImageStoreKoinRule = AvatarImageStoreKoinRule()

    @get:Rule(order = 1)
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
                "Rank 3, ElGoleador, You, 150 points, Up 1 place",
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
                "Rank unavailable, $username, Points unavailable, Movement unavailable",
            )
            .assertIsDisplayed()
    }

    @Test
    fun theLastGamblerEndsAboveTheBottomPaddingAtTheScrollLimit() {
        val models = List(LEADERBOARD_SIZE) { index ->
            score(username = "Gambler $index", position = index + 1, beforePosition = index + 1, score = 100 - index)
                .copy(gamblerId = "gambler-$index")
        }
        composeTestRule.setContent {
            TycheTheme {
                Box(modifier = Modifier.size(width = 360.dp, height = VIEWPORT_HEIGHT)) {
                    GamblerScoreList(
                        lazyPoolGamblerScores = MutableStateFlow(PagingData.from(models)).collectAsLazyPagingItems(),
                        loggedInGamblerId = models.first().gamblerId,
                        contentPadding = PaddingValues(top = TOP_PADDING, bottom = BOTTOM_PADDING),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        val lastRowTag = "gamblerScoreRow:${models.last().gamblerId}"
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty()
        }

        val list = composeTestRule.onNode(hasScrollToIndexAction())
        list.performScrollToNode(hasTestTag(lastRowTag))
        // Fling past the end so the list rests at its scroll limit.
        repeat(3) { list.performTouchInput { swipeUp() } }
        composeTestRule.waitForIdle()

        val lastRowBottom = composeTestRule.onNodeWithTag(lastRowTag).getUnclippedBoundsInRoot().bottom
        val paddedEnd = VIEWPORT_HEIGHT - BOTTOM_PADDING
        assertTrue(
            "Last row ends at $lastRowBottom; the padded end is $paddedEnd",
            lastRowBottom <= paddedEnd && lastRowBottom >= paddedEnd - 1.dp,
        )
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
        trendGroup = KickoffGroupProgress(computedMatches = 1, totalMatches = 1),
    )
}

private const val LEADERBOARD_SIZE = 13
private val VIEWPORT_HEIGHT = 480.dp
private val TOP_PADDING = 64.dp
private val BOTTOM_PADDING = 80.dp
