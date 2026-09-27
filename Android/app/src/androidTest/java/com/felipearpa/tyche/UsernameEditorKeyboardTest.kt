package com.felipearpa.tyche

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.SaveState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

/**
 * Keyboard regression guard for the Android username editor.
 *
 * The editor relies on the activity's window configuration instead of changing the soft-input
 * mode itself: MainActivity is edge to edge and declares `adjustResize`, and the editor's
 * scaffold fits its scrolling content above the keyboard. The test activity is configured the
 * same way before each test, reading the soft-input mode from MainActivity's manifest entry so
 * the harness cannot drift from the shipped configuration.
 *
 * Content is shown at the largest system font and display sizes so the editor overflows with the
 * keyboard open even on a tall phone: the case where the top app bar used to pan away and Save
 * could end up behind the keyboard. Keyboard transitions are awaited by condition, never by
 * elapsed time.
 */
class UsernameEditorKeyboardTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val saveState = mutableStateOf<SaveState<String>>(SaveState.Idle)
    private var backCount = 0

    @Before
    fun configureWindowLikeMainActivity() {
        composeTestRule.runOnUiThread {
            val activity = composeTestRule.activity
            activity.enableEdgeToEdge()
            activity.window.setSoftInputMode(mainActivityInfo().softInputMode)
        }
    }

    @Test
    fun mainActivityResizesForTheKeyboard() {
        assertEquals(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            mainActivityInfo().softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST,
        )
    }

    @Test
    fun withTheKeyboardOpenTheBackButtonStaysVisibleAndSaveCanBeScrolledAboveIt() {
        startEditor()
        awaitKeyboard(visible = true)

        assertTrue(
            "The editor must overflow for this check to exercise scrolling",
            editorContent().fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange].maxValue() > 0f,
        )

        toolbarBack().assertIsDisplayed().assertIsEnabled()
        assertTrue(toolbarBack().bounds().top >= 0f)

        saveButton().performScrollTo().assertIsDisplayed()
        val save = saveButton().bounds()
        assertTrue(
            "Save (bottom ${save.bottom}) must end above the keyboard (top ${keyboardTop()})",
            save.bottom <= keyboardTop() + PIXEL_TOLERANCE,
        )
        toolbarBack().assertIsDisplayed()
    }

    @Test
    fun saveStaysBelowTheFieldGuidanceAndScrollsWithTheContent() {
        startEditor()
        awaitKeyboard(visible = true)

        composeTestRule.onNodeWithText(string(R.string.edit_user_name_subtitle)).performScrollTo()
        val topGap = saveButton().bounds().top - helperText().bounds().bottom
        val topSaveOffset = saveButton().bounds().top

        saveButton().performScrollTo()
        val bottomGap = saveButton().bounds().top - helperText().bounds().bottom

        assertTrue("Save must follow the field guidance", topGap > 0f)
        assertEquals(topGap, bottomGap, PIXEL_TOLERANCE)
        assertTrue(
            "Save must move with the content instead of staying fixed",
            saveButton().bounds().top < topSaveOffset,
        )
    }

    @Test
    fun closingAndReopeningTheKeyboardKeepsTheDraftAndSelectionWithoutAGap() {
        startEditor()
        awaitKeyboard(visible = true)
        usernameField().performTextReplacement(CHANGED_USERNAME)
        usernameField().performTextInputSelection(SELECTION)
        // A gambler's selection is composed well before they can close the keyboard. Hiding it
        // in the same frame instead lets the keyboard's closing edit reach the field's input
        // session before the selection does, which a person cannot do.
        composeTestRule.waitForIdle()

        setKeyboardVisible(false)
        awaitKeyboard(visible = false)
        assertDraftAndSelectionPreserved()

        setKeyboardVisible(true)
        awaitKeyboard(visible = true)
        assertDraftAndSelectionPreserved()
    }

    @Test
    fun withTheKeyboardOpenSavingDisablesTheBackButtonAndAFailureEnablesIt() {
        startEditor()
        awaitKeyboard(visible = true)

        setSaveState(SaveState.Saving(CHANGED_USERNAME))
        toolbarBack().assertIsDisplayed().assertIsNotEnabled()
        toolbarBack().performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, backCount)

        setSaveState(SaveState.Failure(CHANGED_USERNAME, UnknownLocalizedException()))
        toolbarBack().assertIsDisplayed().assertIsEnabled()
        toolbarBack().performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, backCount)
    }

    private fun startEditor() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density * LARGEST_DISPLAY_SIZE,
                    fontScale = LARGEST_FONT_SCALE,
                ),
            ) {
                TycheTheme {
                    UsernameEditorScreen(
                        accountId = ACCOUNT_ID,
                        initialUsername = STORED_USERNAME,
                        saveState = saveState.value,
                        onSave = {},
                        onRetry = {},
                        onResetError = {},
                        onBack = { backCount++ },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Waits until the keyboard has reached the requested state and the editor's scrolling
     * content ends where the larger of the keyboard and the navigation bar begins — so the
     * content neither runs under the keyboard nor leaves a keyboard-sized gap once it closes.
     */
    private fun awaitKeyboard(visible: Boolean) {
        composeTestRule.waitUntil(timeoutMillis = KEYBOARD_TIMEOUT_MILLIS) {
            isKeyboardVisible() == visible &&
                abs(editorContent().bounds().bottom - keyboardTop()) <= PIXEL_TOLERANCE
        }
        composeTestRule.waitForIdle()
    }

    private fun assertDraftAndSelectionPreserved() {
        usernameField().assertTextEquals(CHANGED_USERNAME)
        assertEquals(
            SELECTION,
            usernameField().fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange],
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

    private fun setSaveState(next: SaveState<String>) {
        composeTestRule.runOnUiThread { saveState.value = next }
        composeTestRule.waitForIdle()
    }

    private fun rootInsets(): WindowInsetsCompat = composeTestRule.runOnUiThread {
        requireNotNull(ViewCompat.getRootWindowInsets(composeTestRule.activity.window.decorView))
    }

    private fun isKeyboardVisible() = rootInsets().isVisible(WindowInsetsCompat.Type.ime())

    /** The window y-coordinate where content must end: the keyboard or navigation bar top. */
    private fun keyboardTop(): Float {
        val insets = rootInsets()
        val bottomInset = max(
            insets.getInsets(WindowInsetsCompat.Type.ime()).bottom,
            insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom,
        )
        val windowHeight = composeTestRule.onRoot().fetchSemanticsNode().boundsInWindow.bottom
        return windowHeight - bottomInset
    }

    private fun mainActivityInfo(): ActivityInfo {
        val activity = composeTestRule.activity
        return activity.packageManager.getActivityInfo(
            ComponentName(activity, MainActivity::class.java),
            0,
        )
    }

    /** Unclipped window bounds: a node scrolled out of view keeps its real position. */
    private fun SemanticsNodeInteraction.bounds(): Rect {
        val node = fetchSemanticsNode()
        return Rect(offset = node.positionInWindow, size = node.size.toSize())
    }

    private fun editorContent() =
        composeTestRule.onNode(hasScrollAction() and hasAnyDescendant(hasSetTextAction()))

    private fun usernameField() = composeTestRule.onNode(hasSetTextAction())

    private fun helperText() =
        composeTestRule.onNodeWithText(string(R.string.username_field_helper))

    private fun saveButton() =
        composeTestRule.onNodeWithText(string(R.string.save_username_action))

    private fun toolbarBack() =
        composeTestRule.onNodeWithContentDescription(string(R.string.navigate_back_action))

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private companion object {
        const val ACCOUNT_ID = "username-editor-keyboard-account"
        const val STORED_USERNAME = "felipearpa"
        const val CHANGED_USERNAME = "neptune"
        val SELECTION = TextRange(1, 4)
        const val LARGEST_FONT_SCALE = 2f
        const val LARGEST_DISPLAY_SIZE = 1.35f
        const val PIXEL_TOLERANCE = 1.5f
        const val KEYBOARD_TIMEOUT_MILLIS = 15_000L
    }
}
