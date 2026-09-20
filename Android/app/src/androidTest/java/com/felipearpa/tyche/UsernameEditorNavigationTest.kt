package com.felipearpa.tyche

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import com.felipearpa.tyche.session.authentication.application.UpdateUsername
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.SaveState
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Navigation regression guard for the Android username editor's top-app-bar back button.
 *
 * Leaving the editor must only pop the route: the draft is local, so an unchanged, changed,
 * empty, or failed draft is discarded without submitting a username update or a retry, and a
 * reopened editor is seeded from the stored username again. While a save is in flight both the
 * toolbar button and system back are blocked; a failure restores both.
 *
 * The save-lifecycle cases drive the screen's [SaveState] directly so every transition can be
 * asserted deterministically. The success case uses the real view model so the single
 * return-to-Profile callback is proven end to end.
 *
 * System back is observed with a test [OnBackPressedCallback] registered before the content is
 * set: the dispatcher prefers the most recently added enabled callback, so the editor's own
 * `BackHandler` wins while it is enabled and this one only sees presses the editor lets pass.
 */
class UsernameEditorNavigationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val saveState = mutableStateOf<SaveState<String>>(SaveState.Idle)
    private val isEditorOpen = mutableStateOf(true)

    private var backCount = 0
    private var systemBackCount = 0
    private var saveCount = 0
    private var retryCount = 0
    private var savedValues = emptyList<String>()

    @Test
    fun anUnchangedDraftIsLeftWithoutSavingOrRetrying() {
        startEditor()

        tapToolbarBack()

        assertEquals(1, backCount)
        assertEquals(0, saveCount)
        assertEquals(0, retryCount)
    }

    @Test
    fun aChangedDraftIsDiscardedWithoutSavingOrRetrying() {
        startEditor()

        usernameField().performTextReplacement(CHANGED_USERNAME)
        tapToolbarBack()

        assertEquals(1, backCount)
        assertEquals(0, saveCount)
        assertEquals(0, retryCount)
    }

    @Test
    fun anEmptyDraftIsDiscardedWithoutSavingOrRetrying() {
        startEditor()

        usernameField().performTextClearance()
        tapToolbarBack()

        assertEquals(1, backCount)
        assertEquals(0, saveCount)
        assertEquals(0, retryCount)
    }

    @Test
    fun aFailedSaveIsLeftWithoutRetrying() {
        startEditor()
        setSaveState(SaveState.Failure(CHANGED_USERNAME, UnknownLocalizedException()))

        toolbarBack().assertIsDisplayed()
        tapToolbarBack()

        assertEquals(1, backCount)
        assertEquals(0, retryCount)
        assertEquals(0, saveCount)
    }

    @Test
    fun aReopenedEditorIsSeededFromTheStoredUsername() {
        startEditor()

        usernameField().performTextReplacement(CHANGED_USERNAME)
        tapToolbarBack()
        reopenEditor()

        usernameField().assertTextEquals(STORED_USERNAME)
    }

    @Test
    fun savingKeepsTheToolbarBackButtonVisibleButDisabled() {
        startEditor()
        setSaveState(SaveState.Saving(CHANGED_USERNAME))

        toolbarBack().assertIsDisplayed()
        toolbarBack().assertIsNotEnabled()
        tapToolbarBack()

        assertEquals(0, backCount)
        assertEquals(0, saveCount)
        assertEquals(0, retryCount)
    }

    @Test
    fun savingBlocksSystemBackNavigation() {
        startEditor()
        setSaveState(SaveState.Saving(CHANGED_USERNAME))

        pressSystemBack()

        assertEquals(0, systemBackCount)
        assertEquals(0, backCount)
    }

    @Test
    fun aFailedSaveRestoresSystemBackNavigation() {
        startEditor()
        setSaveState(SaveState.Failure(CHANGED_USERNAME, UnknownLocalizedException()))

        pressSystemBack()

        assertEquals(1, systemBackCount)
        assertEquals(0, retryCount)
        assertEquals(0, saveCount)
    }

    @Test
    fun theEditorAsksForAResizingWindowAndRestoresTheModeWhenItIsLeft() {
        // The default soft-input mode pans a Compose window, which slides the top app bar off
        // screen with the keyboard open on a small viewport or at a large font scale. The route
        // asks for resize only while it is on screen.
        val originalMode = composeTestRule.activity.window.attributes.softInputMode

        startEditor()

        assertEquals(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            composeTestRule.activity.window.attributes.softInputMode and
                WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST,
        )

        tapToolbarBack()

        assertEquals(originalMode, composeTestRule.activity.window.attributes.softInputMode)
    }

    @Test
    fun aSuccessfulSaveReturnsToProfileOnce() {
        val updateUsername = mockk<UpdateUsername>()
        coEvery { updateUsername.execute(CHANGED_USERNAME) } returns Result.success(CHANGED_USERNAME)
        val viewModel = UsernameEditorViewModel(updateUsername = updateUsername)

        registerSystemBackProbe()
        composeTestRule.setContent {
            TycheTheme {
                if (isEditorOpen.value) {
                    UsernameEditorScreen(
                        accountId = ACCOUNT_ID,
                        initialUsername = STORED_USERNAME,
                        viewModel = viewModel,
                        onBack = { backCount++ },
                        onSaved = { saved ->
                            savedValues = savedValues + saved
                            isEditorOpen.value = false
                        },
                    )
                }
            }
        }

        usernameField().performTextReplacement(CHANGED_USERNAME)
        composeTestRule
            .onNodeWithText(string(R.string.save_username_action))
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { savedValues.isNotEmpty() }
        composeTestRule.waitForIdle()

        assertEquals(listOf(CHANGED_USERNAME), savedValues)
        assertEquals(0, backCount)
    }

    private fun startEditor() {
        registerSystemBackProbe()
        composeTestRule.setContent {
            TycheTheme {
                if (isEditorOpen.value) {
                    UsernameEditorScreen(
                        accountId = ACCOUNT_ID,
                        initialUsername = STORED_USERNAME,
                        saveState = saveState.value,
                        onSave = { saveCount++ },
                        onRetry = { retryCount++ },
                        onResetError = {},
                        onBack = {
                            backCount++
                            isEditorOpen.value = false
                        },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun registerSystemBackProbe() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.addCallback(
                composeTestRule.activity,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        systemBackCount++
                    }
                },
            )
        }
    }

    private fun setSaveState(next: SaveState<String>) {
        composeTestRule.runOnUiThread { saveState.value = next }
        composeTestRule.waitForIdle()
    }

    private fun reopenEditor() {
        composeTestRule.runOnUiThread { isEditorOpen.value = true }
        composeTestRule.waitForIdle()
    }

    private fun pressSystemBack() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
    }

    private fun toolbarBack() =
        composeTestRule.onNodeWithContentDescription(string(R.string.navigate_back_action))

    private fun tapToolbarBack() {
        toolbarBack().performClick()
        composeTestRule.waitForIdle()
    }

    // The only node in the editor that accepts text; the preview row merges its children into
    // one accessibility description, so it can never be mistaken for the field.
    private fun usernameField() = composeTestRule.onNode(hasSetTextAction())

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private companion object {
        const val ACCOUNT_ID = "username-editor-navigation-account"
        const val STORED_USERNAME = "felipearpa"
        const val CHANGED_USERNAME = "neptune"
    }
}
