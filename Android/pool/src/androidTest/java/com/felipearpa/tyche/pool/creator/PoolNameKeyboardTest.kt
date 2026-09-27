package com.felipearpa.tyche.pool.creator

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.MutationState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.abs
import kotlin.math.max
import com.felipearpa.tyche.ui.R as SharedR

/**
 * Keyboard regression guard for the pool-name step of pool creation in a constrained window.
 *
 * The window is configured like MainActivity: edge to edge and resized for the keyboard (the
 * app module's username-editor keyboard test asserts that MainActivity's manifest declares
 * `adjustResize`). Each case runs in a constrained window (see [ConstrainedWindow]), so little
 * room is left once the keyboard opens. The real creation screen
 * hosts the production template step with fixed templates. With the keyboard open, the name field, its
 * validation message, and Done must be reachable above the keyboard while the top app bar and
 * its back control stay in place; closing the keyboard must keep the draft; and step navigation
 * must still work. Nothing is created. Keyboard transitions are awaited by condition, never by
 * elapsed time.
 */
@RunWith(Parameterized::class)
class PoolNameKeyboardTest(private val window: ConstrainedWindow) {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var saveCount = 0
    private var backCount = 0

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
    fun theNameFieldItsValidationAndDoneStayReachableAboveTheKeyboard() {
        startCreator()
        chooseTemplate(FIRST_TEMPLATE)

        nameField().performClick()
        awaitKeyboard(visible = true)
        assertTrue(
            "In landscape the form must overflow for this check to exercise scrolling",
            window != ConstrainedWindow.LANDSCAPE ||
                form().fetchSemanticsNode()
                    .config[SemanticsProperties.VerticalScrollAxisRange].maxValue() > 0f,
        )
        nameField().assertEditableText(FIRST_TEMPLATE)
        assertAboveKeyboard(nameField().performScrollTo())
        assertAboveKeyboard(doneButton().performScrollTo().assertIsEnabled())
        assertTopBarInPlace()

        nameField().performScrollTo().performTextReplacement("")
        assertAboveKeyboard(
            composeTestRule.onNodeWithText(string(R.string.pool_name_length_validation_error))
                .performScrollTo(),
        )
        assertAboveKeyboard(doneButton().performScrollTo().assertIsNotEnabled())
        assertTopBarInPlace()
        assertEquals("Nothing is created", 0, saveCount)
    }

    @Test
    fun closingAndReopeningTheKeyboardKeepsTheDraftAndSelectionWithoutAGap() {
        startCreator()
        chooseTemplate(FIRST_TEMPLATE)
        nameField().performClick()
        awaitKeyboard(visible = true)
        nameField().performTextReplacement(DRAFT)
        nameField().performTextInputSelection(SELECTION)
        composeTestRule.waitForIdle()

        setKeyboardVisible(false)
        awaitKeyboard(visible = false)
        assertDraftAndSelectionPreserved()
        assertAboveKeyboard(doneButton().performScrollTo().assertIsEnabled())

        setKeyboardVisible(true)
        awaitKeyboard(visible = true)
        assertDraftAndSelectionPreserved()
    }

    @Test
    fun withTheKeyboardOpenBackReturnsToTheTemplatesAndAnotherTemplateCanBeChosen() {
        startCreator()
        chooseTemplate(FIRST_TEMPLATE)
        nameField().performClick()
        awaitKeyboard(visible = true)

        backButton().performClick()
        composeTestRule.waitUntil(timeoutMillis = TRANSITION_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty()
        }
        assertEquals("Back from the name step stays in the creator", 0, backCount)

        chooseTemplate(SECOND_TEMPLATE)
        nameField().assertEditableText(SECOND_TEMPLATE)
        assertEquals("Nothing is created", 0, saveCount)
    }

    private fun startCreator() {
        val templates = MutableStateFlow(PagingData.from(poolLayoutDummyModels()))
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density * window.displaySize,
                    fontScale = window.fontScale,
                ),
            ) {
                TycheTheme {
                    val lazyTemplates = templates.collectAsLazyPagingItems()
                    PoolFromLayoutCreatorView(
                        state = MutationState.Idle(emptyCreatePoolModel()),
                        onSaveClick = { saveCount++ },
                        onPoolCreated = {},
                        onBackClick = { backCount++ },
                        reset = {},
                        stepOneView = { model, contentPadding, onNext ->
                            StepOneView(
                                lazyItems = lazyTemplates,
                                pageSize = 3,
                                createPoolModel = model,
                                onNextClick = onNext,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        applyResizingSoftInputMode()
    }

    /** Taps a template on the first step and waits for the name step. */
    private fun chooseTemplate(name: String) {
        composeTestRule.waitUntil(timeoutMillis = TRANSITION_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(name).performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = TRANSITION_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
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

    /**
     * The top app bar is pinned on this step: scrolling the form neither pans nor collapses it,
     * so its back control stays inside the window and fully above the form.
     */
    private fun assertTopBarInPlace() {
        val back = backButton().assertIsDisplayed().assertIsEnabled().bounds()
        assertTrue("Back (top ${back.top}) must be inside the window", back.top >= 0f)
        assertTrue(
            "Back (bottom ${back.bottom}) must be above the form (top ${form().bounds().top})",
            back.bottom <= form().bounds().top + PIXEL_TOLERANCE,
        )
    }

    private fun assertDraftAndSelectionPreserved() {
        nameField().assertEditableText(DRAFT)
        assertEquals(
            SELECTION,
            nameField().fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange],
        )
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

    private fun nameField() = composeTestRule.onNode(hasSetTextAction())

    private fun doneButton() =
        composeTestRule.onNodeWithText(string(SharedR.string.done_action))

    /**
     * The top app bar's back control has no accessible name, so it is found as the only
     * clickable node outside the form's scrolling content.
     */
    private fun backButton() =
        composeTestRule.onNode(hasClickAction() and !hasAnyAncestor(hasScrollAction()))

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private companion object {
        const val FIRST_TEMPLATE = "Champions League"
        const val SECOND_TEMPLATE = "Premier League"
        const val DRAFT = "Office pool"
        val SELECTION = TextRange(2, 6)
        const val PIXEL_TOLERANCE = 1.5f
        const val KEYBOARD_TIMEOUT_MILLIS = 15_000L
        const val ROTATION_TIMEOUT_MILLIS = 10_000L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun windows() = ConstrainedWindow.entries
        const val TRANSITION_TIMEOUT_MILLIS = 5_000L
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
