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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.felipearpa.tyche.account.byemail.EmailSignInView
import com.felipearpa.tyche.account.byemailandpassword.EmailAndPasswordSignInView
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.LoadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.abs
import kotlin.math.max

/**
 * Keyboard regression guard for email and email-and-password sign-in in a constrained window.
 *
 * The window is configured like MainActivity: edge to edge and resized for the keyboard (the
 * app module's username-editor keyboard test asserts that MainActivity's manifest declares
 * `adjustResize`). Each case runs in a constrained window (see [ConstrainedWindow]), so little
 * room is left once the keyboard opens. With the keyboard open, each
 * field, the form's guidance, and Sign in must be reachable above the keyboard while the top app
 * bar stays in place, and closing the keyboard must keep the draft without leaving a gap.
 * Nothing is submitted. Keyboard transitions are awaited by condition, never by elapsed time.
 */
@RunWith(Parameterized::class)
class SignInKeyboardTest(private val window: ConstrainedWindow) {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Turns the test activity to the case's orientation, which may recreate it, and then makes
     * the window edge to edge. The orientation request belongs to this activity and ends with it.
     */
    @Before
    fun configureTheWindow() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.requestedOrientation = window.requestedOrientation
        }
        composeTestRule.waitUntil(timeoutMillis = ROTATION_TIMEOUT_MILLIS) {
            composeTestRule.runOnUiThread {
                composeTestRule.activity.resources.configuration.orientation ==
                    window.orientation
            }
        }
        composeTestRule.runOnUiThread { composeTestRule.activity.enableEdgeToEdge() }
    }

    @Test
    fun emailSignInKeepsTheFieldAndSignInAboveTheKeyboard() {
        startEmailSignIn()

        emailField().performClick()
        awaitKeyboard(visible = true)
        emailField().performTextReplacement(EMAIL)

        assertAboveKeyboard(emailField())
        assertAboveKeyboard(signInButton().performScrollTo().assertIsEnabled())
        assertTopBarInPlace()
    }

    @Test
    fun emailSignInKeepsTheDraftAndSelectionWhenTheKeyboardClosesAndReopens() {
        startEmailSignIn()
        emailField().performClick()
        awaitKeyboard(visible = true)
        emailField().performTextReplacement(EMAIL)
        emailField().performTextInputSelection(SELECTION)

        setKeyboardVisible(false)
        awaitKeyboard(visible = false)
        assertDraftAndSelectionPreserved(emailField(), EMAIL)

        setKeyboardVisible(true)
        awaitKeyboard(visible = true)
        assertDraftAndSelectionPreserved(emailField(), EMAIL)
    }

    @Test
    fun passwordSignInKeepsEachFieldTheGuidanceAndSignInReachableAboveTheKeyboard() {
        startPasswordSignIn()

        emailField().performClick()
        awaitKeyboard(visible = true)
        assertTrue(
            "In landscape the form must overflow for this check to exercise scrolling",
            window != ConstrainedWindow.LANDSCAPE ||
                form().fetchSemanticsNode()
                    .config[SemanticsProperties.VerticalScrollAxisRange].maxValue() > 0f,
        )
        emailField().performTextReplacement(EMAIL)
        assertAboveKeyboard(emailField())
        signInButton().performScrollTo().assertIsNotEnabled()

        passwordField().performScrollTo().performClick()
        awaitKeyboard(visible = true)
        passwordField().performTextReplacement(PASSWORD)
        assertAboveKeyboard(passwordField())

        assertAboveKeyboard(signInButton().performScrollTo().assertIsEnabled())
        assertAboveKeyboard(
            composeTestRule.onNodeWithText(string(R.string.no_recovery_password_warning))
                .performScrollTo(),
        )
        assertTopBarInPlace()
    }

    @Test
    fun passwordSignInKeepsTheDraftWhenTheKeyboardClosesAndReopens() {
        startPasswordSignIn()
        emailField().performClick()
        awaitKeyboard(visible = true)
        emailField().performTextReplacement(EMAIL)
        passwordField().performScrollTo().performClick()
        passwordField().performTextReplacement(PASSWORD)
        passwordField().performTextInputSelection(SELECTION)

        setKeyboardVisible(false)
        awaitKeyboard(visible = false)
        emailField().assertEditableText(EMAIL)
        assertPasswordDraftPreserved()

        setKeyboardVisible(true)
        awaitKeyboard(visible = true)
        emailField().assertEditableText(EMAIL)
        assertPasswordDraftPreserved()
        signInButton().performScrollTo().assertIsEnabled()
    }

    private fun startEmailSignIn() = start {
        EmailSignInView(
            viewState = LoadState.Idle,
            onSignInWithEmail = {},
            onReset = {},
            onBack = {},
        )
    }

    private fun startPasswordSignIn() = start {
        EmailAndPasswordSignInView(
            viewState = LoadState.Idle,
            onSignIn = { _, _ -> },
            onBack = {},
            onReset = {},
            onAuthenticate = {},
        )
    }

    private fun start(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density * window.displaySize,
                    fontScale = window.fontScale,
                ),
            ) {
                TycheTheme { content() }
            }
        }
        composeTestRule.waitForIdle()
        applyResizingSoftInputMode()
    }

    /**
     * Gives the attached window MainActivity's manifest soft-input adjustment. Set before the
     * content is attached, the mode does not reach the window manager, which then pans the window.
     */
    @Suppress("DEPRECATION")
    private fun applyResizingSoftInputMode() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            )
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Waits until the keyboard has reached the requested state and the form's scrolling area
     * ends where the larger of the keyboard and the navigation bar begins, so the form neither
     * runs under the keyboard nor leaves a keyboard-sized gap once it closes.
     */
    private fun awaitKeyboard(visible: Boolean) {
        runCatching {
            composeTestRule.waitUntil(timeoutMillis = KEYBOARD_TIMEOUT_MILLIS) {
                isKeyboardVisible() == visible &&
                    abs(form().bounds().bottom - keyboardTop()) <= PIXEL_TOLERANCE
            }
        }
        composeTestRule.waitForIdle()
        assertEquals("Keyboard visibility", visible, isKeyboardVisible())
        assertEquals("Form bottom", keyboardTop(), form().bounds().bottom, PIXEL_TOLERANCE)
    }

    /**
     * The node lies between the top app bar and the keyboard. A node taller than that space
     * (a text field with its floating label in a landscape phone window) cannot fit, so it must
     * instead fill the whole space, showing as much of itself as fits.
     */
    private fun assertAboveKeyboard(node: SemanticsNodeInteraction) {
        val bounds = node.assertIsDisplayed().bounds()
        val top = form().bounds().top
        val bottom = keyboardTop()
        val visibleHeight = minOf(bounds.bottom, bottom) - maxOf(bounds.top, top)
        val expectedHeight = minOf(bounds.height, bottom - top)
        assertTrue(
            "The node (${bounds.top}..${bounds.bottom}) must show ${expectedHeight}px between " +
                "the top app bar ($top) and the keyboard ($bottom), not ${visibleHeight}px",
            visibleHeight >= expectedHeight - PIXEL_TOLERANCE,
        )
    }

    /** The top app bar has not panned away: its back control starts inside the window. */
    private fun assertTopBarInPlace() {
        val back = backButton().assertIsDisplayed().bounds()
        assertTrue("Back (top ${back.top}) must be inside the window", back.top >= 0f)
        assertTrue(
            "Back (bottom ${back.bottom}) must be above the form (top ${form().bounds().top})",
            back.bottom <= form().bounds().top + PIXEL_TOLERANCE,
        )
    }

    private fun assertDraftAndSelectionPreserved(field: SemanticsNodeInteraction, draft: String) {
        field.assertEditableText(draft)
        assertEquals(
            SELECTION,
            field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange],
        )
    }

    /** The password is masked, so its draft is checked through the edited text itself. */
    private fun assertPasswordDraftPreserved() {
        val config = passwordField().fetchSemanticsNode().config
        assertEquals(PASSWORD, config[SemanticsProperties.InputText].text)
        assertEquals(SELECTION, config[SemanticsProperties.TextSelectionRange])
    }

    private fun setKeyboardVisible(visible: Boolean) {
        composeTestRule.runOnUiThread {
            val window = composeTestRule.activity.window
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (visible) {
                controller.show(WindowInsetsCompat.Type.ime())
            } else {
                controller.hide(WindowInsetsCompat.Type.ime())
            }
        }
    }

    private fun rootInsets(): WindowInsetsCompat = composeTestRule.runOnUiThread {
        requireNotNull(ViewCompat.getRootWindowInsets(composeTestRule.activity.window.decorView))
    }

    private fun isKeyboardVisible() = rootInsets().isVisible(WindowInsetsCompat.Type.ime())

    /** The window y-coordinate where the form must end: the keyboard or navigation bar top. */
    private fun keyboardTop(): Float {
        val insets = rootInsets()
        val bottomInset = max(
            insets.getInsets(WindowInsetsCompat.Type.ime()).bottom,
            insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom,
        )
        // The window's own height: a text handle's popup is a second semantics root.
        val windowHeight = composeTestRule.runOnUiThread {
            composeTestRule.activity.window.decorView.height
        }
        return (windowHeight - bottomInset).toFloat()
    }

    /** The field's edited text alone; the merged label is not part of it. */
    private fun SemanticsNodeInteraction.assertEditableText(expected: String) = apply {
        assertEquals(expected, fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }

    /** Unclipped window bounds: a node scrolled out of view keeps its real position. */
    private fun SemanticsNodeInteraction.bounds(): Rect {
        val node = fetchSemanticsNode()
        return Rect(offset = node.positionInWindow, size = node.size.toSize())
    }

    private fun form() =
        composeTestRule.onNode(hasScrollAction() and hasAnyDescendant(hasSetTextAction()))

    private fun emailField() = composeTestRule.onNode(
        hasSetTextAction() and hasText(string(R.string.email_label)),
    )

    private fun passwordField() = composeTestRule.onNode(
        hasSetTextAction() and hasText(string(R.string.password_label)),
    )

    /** Sign in shares its text with the top app bar's title; only the button is clickable. */
    private fun signInButton() = composeTestRule.onNode(
        hasText(string(R.string.sign_in_action)) and hasClickAction(),
    )

    /**
     * The top app bar's back control has no accessible name, so it is found as the only
     * clickable node outside the form's scrolling content.
     */
    private fun backButton() =
        composeTestRule.onNode(hasClickAction() and !hasAnyAncestor(hasScrollAction()))

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private companion object {
        const val EMAIL = "gambler@example.com"
        const val PASSWORD = "not-a-real-password"
        val SELECTION = TextRange(2, 6)
        const val PIXEL_TOLERANCE = 1.5f
        const val KEYBOARD_TIMEOUT_MILLIS = 15_000L
        const val ROTATION_TIMEOUT_MILLIS = 10_000L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun windows() = ConstrainedWindow.entries
    }
}

/**
 * The constrained windows the form must work in: landscape with large text, where the keyboard
 * leaves the least height, and portrait at the largest system font and display sizes.
 */
enum class ConstrainedWindow(
    val requestedOrientation: Int,
    val orientation: Int,
    val fontScale: Float,
    val displaySize: Float,
) {
    LANDSCAPE(
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
        orientation = Configuration.ORIENTATION_LANDSCAPE,
        fontScale = 1.3f,
        displaySize = 1f,
    ),
    LARGEST_TEXT(
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
        orientation = Configuration.ORIENTATION_PORTRAIT,
        fontScale = 2f,
        displaySize = 1.35f,
    ),
}
