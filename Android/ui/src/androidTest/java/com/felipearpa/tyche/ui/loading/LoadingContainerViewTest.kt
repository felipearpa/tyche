package com.felipearpa.tyche.ui.loading

import android.view.KeyEvent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.activity.ComponentActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.tyche.ui.dpToPx
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Rule
import org.junit.Test

class LoadingContainerViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @After
    fun restoreTouchMode() {
        // Key presses leave touch mode; later tests expect it.
        runCatching { InstrumentationRegistry.getInstrumentation().setInTouchMode(true) }
    }

    @Test
    fun given_an_idle_host_when_shown_then_the_screen_is_interactive_and_no_status_is_exposed() {
        val harness = setUpHost()

        composeTestRule.onNodeWithText(SubmitLabel).performClick()

        assertSoftly {
            harness.submissions shouldBe 1
            composeTestRule.onAllNodesWithContentDescription(loadingStatus).fetchSemanticsNodes().size shouldBe 0
        }
    }

    @Test
    fun given_loading_when_the_covered_button_is_tapped_then_nothing_is_submitted() {
        val harness = setUpHost()
        val submit = composeTestRule.onNodeWithText(SubmitLabel).fetchSemanticsNode().boundsInRoot.center
        harness.isLoading = true
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performTouchInput { click(submit) }
        composeTestRule.waitForIdle()

        harness.submissions shouldBe 0
    }

    @Test
    fun given_loading_when_the_covered_list_is_swiped_then_it_keeps_its_position() {
        val harness = setUpHost()
        harness.isLoading = true
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()

        harness.listState.firstVisibleItemIndex shouldBe 0
        harness.listState.firstVisibleItemScrollOffset shouldBe 0
    }

    @Test
    fun given_a_focused_field_when_loading_begins_then_it_loses_focus_and_keys_do_not_reach_it() {
        val harness = setUpHost()
        composeTestRule.onNodeWithText(EmailLabel).performClick()
        composeTestRule.onNodeWithText(EmailLabel).performTextInput("abc")
        composeTestRule.onNodeWithText(EmailLabel).assertIsFocused()

        harness.isLoading = true
        composeTestRule.waitForIdle()
        listOf(KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_ENTER)
            .forEach { InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(it) }
        composeTestRule.waitForIdle()
        val focusedWhileLoading = harness.screenHasFocus
        harness.isLoading = false
        composeTestRule.waitForIdle()

        assertSoftly {
            focusedWhileLoading shouldBe false
            harness.submissions shouldBe 0
            composeTestRule.onNodeWithText(EmailLabel).assertIsNotFocused()
            composeTestRule.onNodeWithText(EmailLabel).assertTextContains("abc")
        }
    }

    @Test
    fun given_loading_when_accessibility_reads_the_screen_then_only_one_indeterminate_status_is_exposed() {
        val harness = setUpHost()
        harness.isLoading = true
        composeTestRule.waitForIdle()

        assertSoftly {
            composeTestRule.onNodeWithText(SubmitLabel).assertDoesNotExist()
            composeTestRule.onNodeWithText(EmailLabel).assertDoesNotExist()
            composeTestRule.onNodeWithText("Row 0").assertDoesNotExist()
            composeTestRule.onAllNodesWithContentDescription(loadingStatus)
                .fetchSemanticsNodes().size shouldBe 1
            composeTestRule.onNodeWithContentDescription(loadingStatus)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
            Unit
        }
    }

    @Test
    fun given_loading_with_typed_values_and_a_scrolled_list_when_loading_ends_then_the_same_screen_is_interactive_again() {
        val harness = setUpHost()
        composeTestRule.onNodeWithText(EmailLabel).performTextInput("kept@example.com")
        composeTestRule.runOnIdle { harness.listState.requestScrollToItem(index = 20) }
        composeTestRule.waitForIdle()

        harness.isLoading = true
        composeTestRule.waitForIdle()
        harness.isLoading = false
        composeTestRule.waitForIdle()
        harness.isLoading = true
        composeTestRule.waitForIdle()
        harness.isLoading = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(SubmitLabel).performClick()

        assertSoftly {
            harness.screenCreations shouldBe 1
            harness.screenDisposals shouldBe 0
            harness.listState.firstVisibleItemIndex shouldBe 20
            composeTestRule.onNodeWithText(EmailLabel).assertTextContains("kept@example.com")
            harness.submissions shouldBe 1
            composeTestRule.onAllNodesWithContentDescription(loadingStatus).fetchSemanticsNodes().size shouldBe 0
        }
    }

    @Test
    fun given_loading_when_tab_is_pressed_then_focus_does_not_enter_the_screen_until_loading_ends() {
        val harness = setUpHost()
        harness.isLoading = true
        composeTestRule.waitForIdle()

        repeat(3) { InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB) }
        composeTestRule.waitForIdle()
        val focusedWhileLoading = harness.screenHasFocus
        harness.isLoading = false
        composeTestRule.waitForIdle()
        repeat(2) { InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB) }
        composeTestRule.waitForIdle()

        focusedWhileLoading shouldBe false
        composeTestRule.focusedNodes() shouldBe 1
    }

    @Test
    fun given_a_full_window_host_when_loading_then_the_ball_is_centered_in_the_safe_area() {
        var insets = IntArray(4)
        composeTestRule.setContent {
            val density = LocalDensity.current
            val direction = LocalLayoutDirection.current
            val safe = WindowInsets.safeDrawing
            insets = intArrayOf(
                safe.getLeft(density, direction),
                safe.getTop(density),
                safe.getRight(density, direction),
                safe.getBottom(density),
            )
            TycheTheme {
                Surface {
                    LoadingContainerView(isLoading = true)
                }
            }
        }
        composeTestRule.waitForIdle()

        val root = composeTestRule.onRoot().fetchSemanticsNode().size
        val ball = composeTestRule.onNodeWithContentDescription(loadingStatus).fetchSemanticsNode()
        val (left, top, right, bottom) = insets.toList()
        val expectedX = left + (root.width - left - right) / 2f
        val expectedY = top + (root.height - top - bottom) / 2f
        val ballSize = composeTestRule.dpToPx(64f)

        withClue("root $root, insets ${insets.toList()}, ball ${ball.boundsInRoot}") {
            assertSoftly {
                ball.size.width.toFloat() shouldBe (ballSize plusOrMinus 1f)
                ball.size.height.toFloat() shouldBe (ballSize plusOrMinus 1f)
                ball.boundsInRoot.center.x shouldBe (expectedX plusOrMinus 1f)
                ball.boundsInRoot.center.y shouldBe (expectedY plusOrMinus 1f)
            }
        }
    }

    private fun setUpHost(): LoadingHostHarness {
        val harness = LoadingHostHarness()
        composeTestRule.setContent { harness.Content() }
        composeTestRule.waitForIdle()
        return harness
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.focusedNodes(): Int =
        onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .size
}


