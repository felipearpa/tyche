package com.felipearpa.tyche

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.felipearpa.tyche.poolhome.PoolHomeContent
import com.felipearpa.tyche.poolhome.Tab
import com.felipearpa.tyche.ui.theme.TycheTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

/**
 * Pool home's tabs share one top app bar. After one tab is scrolled, another tab opens at the top
 * of its list, so its bar must be shown in its unscrolled color; scrolling that tab must still
 * hide the bar, and bringing it back over scrolled content must tint it. Each tab's content is a plain list keyed by tab, as the real tabs are
 * separate lists.
 */
@OptIn(ExperimentalMaterial3Api::class)
class PoolHomeTabSwitchAppBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun everyTabSwitchShowsTheBarUnscrolledAndScrollingStillTintsAndHidesIt() {
        val tabs = listOf(Tab.HISTORY_BET, Tab.GAMBLER_SCORE, Tab.BET_EDITOR)
        var selectedTab by mutableStateOf(Tab.HISTORY_BET)
        startPoolHome(selectedTab = { selectedTab }, onTabChange = { selectedTab = it })
        val unscrolledColor = barCornerColor()

        tabs.forEach { from ->
            tabs.filter { it != from }.forEach { to ->
                selectTab(from)
                scrollList(distance = FULL_SCROLL)
                title(from).assertIsNotDisplayed()

                selectTab(to)

                title(to).assertIsDisplayed()
                assertEquals("bar color after $from → $to", unscrolledColor, barCornerColor())

                scrollList(distance = FULL_SCROLL)
                title(to).assertIsNotDisplayed()
                revealBar()
                title(to).assertIsDisplayed()
                assertNotEquals("bar tint over scrolled $to content", unscrolledColor, barCornerColor())
            }
        }
    }

    private fun startPoolHome(selectedTab: () -> Tab, onTabChange: (Tab) -> Unit) {
        composeTestRule.setContent {
            TycheTheme {
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                PoolHomeContent(
                    selectedTabIndex = selectedTab(),
                    onTabChange = onTabChange,
                    isDrawerOpen = false,
                    onDrawerOpenChange = {},
                    scrollBehavior = scrollBehavior,
                    onPoolChange = {},
                    drawerContent = {},
                    isSaving = false,
                    content = { padding -> key(selectedTab()) { TabList(padding) } },
                )
            }
        }
    }

    @Composable
    private fun TabList(padding: PaddingValues) {
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize().testTag(LIST_TAG)) {
            items(count = 60) { index ->
                Text(text = "Row $index", modifier = Modifier.fillMaxWidth().height(56.dp))
            }
        }
    }

    /** Taps the tab; its label repeats the bar title, but only the tab is clickable. */
    private fun selectTab(tab: Tab) {
        composeTestRule.onNode(hasText(titleText(tab)) and hasClickAction()).performClick()
        composeTestRule.waitForIdle()
    }

    private fun title(tab: Tab) = composeTestRule.onNode(hasText(titleText(tab)) and !hasClickAction())

    private fun scrollList(distance: Float) {
        composeTestRule.onNodeWithTag(LIST_TAG).performTouchInput {
            swipeUp(startY = centerY, endY = centerY - distance)
        }
        composeTestRule.waitForIdle()
    }

    /** A short downward drag brings the hidden bar back while the list stays scrolled. */
    private fun revealBar() {
        composeTestRule.onNodeWithTag(LIST_TAG).performTouchInput {
            swipeDown(startY = centerY, endY = centerY + REVEAL_DRAG)
        }
        composeTestRule.waitForIdle()
    }

    /** The window's top-left pixel, inside the top app bar while the bar is shown. */
    private fun barCornerColor(): Color =
        composeTestRule.onRoot().captureToImage().toPixelMap()[1, 1]

    private fun titleText(tab: Tab) = resources.getString(
        when (tab) {
            Tab.GAMBLER_SCORE -> R.string.score_tab
            Tab.BET_EDITOR -> R.string.bet_tab
            Tab.HISTORY_BET -> R.string.history_bets_tab
        },
    )
}

private const val LIST_TAG = "tabList"
private const val FULL_SCROLL = 900f
private const val REVEAL_DRAG = 300f
