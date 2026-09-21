package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.creator.PoolLayoutModel
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * A "My pools" row is one focusable button carrying both of its actions.
 *
 * The description comes from [PoolScoreItem], which clears its subtree — including the
 * invite button's own node. The clickable wrapper in [PoolScoreList] supplies the button
 * role and the named open action, and re-exposes invite as a custom action on the row,
 * so a TalkBack user reaches both without leaving it.
 */
class PoolScoreListAccessibilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val rowDescription =
        "Neptune World Series 2023, Rank 4, 8 points, 101 members, Down 1 place"

    @Test
    fun rowIsASingleButtonWithANamedOpenAction() {
        var openedPoolId: String? = null
        var openedGamblerId: String? = null
        renderList(onPoolOpen = { poolId, gamblerId ->
            openedPoolId = poolId
            openedGamblerId = gamblerId
        })

        val row = composeTestRule
            .onNodeWithContentDescription(rowDescription)
            .assertIsDisplayed()
            .assertHasClickAction()

        val config = row.fetchSemanticsNode().config
        assertEquals(Role.Button, config.getOrNull(SemanticsProperties.Role))
        assertEquals("Open pool", config.getOrNull(SemanticsActions.OnClick)?.label)

        row.performClick()
        assertEquals("A3C2E1", openedPoolId)
        assertEquals("YF23H1", openedGamblerId)
    }

    @Test
    fun inviteIsOfferedAsANamedActionOnTheRow() {
        var joinedPoolId: String? = null
        renderList(onPoolJoin = { joinedPoolId = it })

        val config = composeTestRule
            .onNodeWithContentDescription(rowDescription)
            .fetchSemanticsNode()
            .config

        val invite = config.getOrNull(SemanticsActions.CustomActions)?.single()
        assertNotNull(invite)
        assertEquals("Invite to pool", invite!!.label)

        invite.action()
        assertEquals("A3C2E1", joinedPoolId)
    }

    @Test
    fun rowPartsAreNotSeparateScreenReaderStops() {
        renderList()

        // The rank tile ("4"), the trend value ("1"), the pool name, the points and the
        // member count are all drawn, but the row clears its subtree, so none of them is
        // reachable as its own node in the merged tree a screen reader traverses.
        composeTestRule.onAllNodesWithText("4").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("1").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Neptune World Series 2023").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("8 pts.").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("101 members").assertCountEquals(0)

        composeTestRule.onNodeWithContentDescription(rowDescription).assertIsDisplayed()
    }

    private fun renderList(
        onPoolOpen: (String, String) -> Unit = { _, _ -> },
        onPoolJoin: (String) -> Unit = {},
    ) {
        val pagingData = MutableStateFlow(PagingData.from(listOf(model())))
        val layouts = MutableStateFlow(PagingData.empty<PoolLayoutModel>())

        composeTestRule.setContent {
            TycheTheme {
                PoolScoreList(
                    modifier = Modifier.fillMaxSize(),
                    lazyPoolGamblerScores = pagingData.collectAsLazyPagingItems(),
                    lazyPoolLayouts = layouts.collectAsLazyPagingItems(),
                    onPoolOpen = onPoolOpen,
                    onPoolJoin = onPoolJoin,
                    onPoolLayoutSelect = {},
                    onSeeAllTemplates = {},
                )
            }
        }
    }

    // position 4 from beforePosition 3 is a one-place drop.
    private fun model() = PoolGamblerScoreModel(
        poolId = "A3C2E1",
        poolName = "Neptune World Series 2023",
        gamblerId = "YF23H1",
        gamblerUsername = "neptune-player",
        position = 4,
        beforePosition = 3,
        score = 8,
        gamblerCount = 101,
    )
}
