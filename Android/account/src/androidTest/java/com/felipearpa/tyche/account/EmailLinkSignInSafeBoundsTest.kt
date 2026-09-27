package com.felipearpa.tyche.account

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.felipearpa.tyche.account.byemail.EmailLinkSignInView
import com.felipearpa.tyche.session.emptyAccountBundle
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.LoadState
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.felipearpa.tyche.ui.R as SharedR

/**
 * Safe-bounds regression guard for the email-link sign-in result screens, which have no scaffold
 * and own their insets. In an edge-to-edge landscape window with large text, where the display
 * cutout sits on a side edge, the confirmation and its action must stay inside the area clear of
 * the system bars and the cutout, and the message must never spill over the action.
 */
class EmailLinkSignInSafeBoundsTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun turnTheWindowToLandscapeEdgeToEdge() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        composeTestRule.waitUntil(timeoutMillis = ROTATION_TIMEOUT_MILLIS) {
            composeTestRule.runOnUiThread {
                composeTestRule.activity.resources.configuration.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE
            }
        }
        composeTestRule.runOnUiThread {
            val activity = composeTestRule.activity
            activity.enableEdgeToEdge()
            // As MainActivity gets from enableEdgeToEdge() before its window is attached; set
            // after attachment, the mode must be reapplied to reach the window manager.
            activity.window.attributes = activity.window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }

    @Test
    fun theVerifiedConfirmationKeepsGetStartedInsideTheSafeAreaAndBelowTheMessage() {
        start {
            EmailLinkSignInView(
                state = LoadState.Loaded(emptyAccountBundle()),
                email = "gambler@example.com",
                onRetry = {},
            )
        }

        val action = composeTestRule.onNode(
            hasText(string(R.string.start_action)) and hasClickAction(),
        )
        assertInsideSafeArea(action.performScrollTo())
        assertAbove(
            composeTestRule.onNodeWithText(string(R.string.account_verified_description)),
            action,
        )
    }

    @Test
    fun theFailureKeepsRetryInsideTheSafeAreaAndBelowTheMessage() {
        start {
            EmailLinkSignInView(
                state = LoadState.Failure(UnknownLocalizedException()),
                email = "gambler@example.com",
                onRetry = {},
            )
        }

        val action = composeTestRule.onNode(
            hasText(string(SharedR.string.retry_action)) and hasClickAction(),
        )
        assertInsideSafeArea(action.performScrollTo())
        assertAbove(
            composeTestRule.onNodeWithText(
                string(SharedR.string.unknown_failure_recovery_suggestion),
                substring = true,
            ),
            action,
        )
    }

    private fun start(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = density.density, fontScale = FONT_SCALE),
            ) {
                TycheTheme { content() }
            }
        }
        composeTestRule.waitForIdle()
    }

    /** Clear of the system bars and the display cutout on every side, fully shown. */
    private fun assertInsideSafeArea(node: SemanticsNodeInteraction) {
        val bounds = node.assertIsDisplayed().bounds()
        val safeArea = safeArea()
        assertTrue(
            "The action $bounds must lie inside the safe area $safeArea",
            bounds.left >= safeArea.left - PIXEL_TOLERANCE &&
                bounds.top >= safeArea.top - PIXEL_TOLERANCE &&
                bounds.right <= safeArea.right + PIXEL_TOLERANCE &&
                bounds.bottom <= safeArea.bottom + PIXEL_TOLERANCE,
        )
    }

    private fun assertAbove(message: SemanticsNodeInteraction, action: SemanticsNodeInteraction) {
        val messageBottom = message.bounds().bottom
        val actionTop = action.bounds().top
        assertTrue(
            "The message (bottom $messageBottom) must end above the action (top $actionTop)",
            messageBottom <= actionTop + PIXEL_TOLERANCE,
        )
    }

    private fun safeArea(): Rect = composeTestRule.runOnUiThread {
        val decorView = composeTestRule.activity.window.decorView
        val insets = requireNotNull(ViewCompat.getRootWindowInsets(decorView)).getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
        )
        Rect(
            left = insets.left.toFloat(),
            top = insets.top.toFloat(),
            right = (decorView.width - insets.right).toFloat(),
            bottom = (decorView.height - insets.bottom).toFloat(),
        )
    }

    /** Unclipped window bounds: a node scrolled out of view keeps its real position. */
    private fun SemanticsNodeInteraction.bounds(): Rect {
        val node = fetchSemanticsNode()
        return Rect(offset = node.positionInWindow, size = node.size.toSize())
    }

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private companion object {
        const val FONT_SCALE = 2f
        const val PIXEL_TOLERANCE = 1.5f
        const val ROTATION_TIMEOUT_MILLIS = 10_000L
    }
}
