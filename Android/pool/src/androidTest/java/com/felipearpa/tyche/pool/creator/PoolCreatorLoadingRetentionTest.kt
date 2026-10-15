package com.felipearpa.tyche.pool.creator

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.MutationState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import com.felipearpa.tyche.ui.R as SharedR

/**
 * The pool creator keeps one stepper through every mutation state: an unsaved name typed on the
 * name step survives saving, a failure and its dismissal, which a recreated step would reset to
 * the preselected name. A created pool is handed off once, however often the screen recomposes.
 */
class PoolCreatorLoadingRetentionTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var state by mutableStateOf<MutationState<CreatePoolModel>>(
        MutationState.Idle(emptyCreatePoolModel()),
    )
    private var recompositionTrigger by mutableIntStateOf(0)
    private var resetCount = 0
    private val createdPoolIds = mutableListOf<String>()

    @Test
    fun givenAnUnsavedNameWhenSavingFailsAndIsDismissedThenTheNameIsKept() {
        startCreator()
        nameField().performTextReplacement(DRAFT)

        state = MutationState.Mutating(submittedModel)
        composeTestRule.waitForIdle()
        state = MutationState.Failure(submittedModel, UnknownLocalizedException())
        composeTestRule.waitForIdle()
        nameField().assertEditableText(DRAFT)

        composeTestRule.onNode(hasText(string(SharedR.string.done_action)) and hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals("Dismissing the error resets the mutation", 1, resetCount)

        state = MutationState.Idle(submittedModel)
        composeTestRule.waitForIdle()
        nameField().assertEditableText(DRAFT)
    }

    @Test
    fun givenACreatedPoolWhenTheScreenRecomposesThenItIsHandedOffOnce() {
        startCreator()

        state = MutationState.Mutating(submittedModel)
        composeTestRule.waitForIdle()
        state = MutationState.Mutated(submittedModel.copy(poolId = POOL_ID))
        composeTestRule.waitForIdle()
        repeat(3) {
            recompositionTrigger++
            composeTestRule.waitForIdle()
        }

        assertEquals(listOf(POOL_ID), createdPoolIds)
    }

    private fun startCreator() {
        composeTestRule.setContent {
            // Read so that bumping the trigger recomposes the whole creator.
            recompositionTrigger
            TycheTheme {
                PoolFromLayoutCreatorView(
                    state = state,
                    onSaveClick = {},
                    onPoolCreated = { poolId -> createdPoolIds += poolId },
                    onBackClick = {},
                    reset = { resetCount++ },
                    preselectedPoolLayoutId = LAYOUT_ID,
                    preselectedPoolName = PRESELECTED_NAME,
                    stepOneView = { _, _, _ -> },
                )
            }
        }
        composeTestRule.waitForIdle()
        nameField().assertEditableText(PRESELECTED_NAME)
    }

    private fun nameField() = composeTestRule.onNode(hasSetTextAction())

    private fun SemanticsNodeInteraction.assertEditableText(expected: String) = apply {
        assertEquals(expected, fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private companion object {
        const val LAYOUT_ID = "layout-1"
        const val PRESELECTED_NAME = "Champions League"
        const val DRAFT = "Office pool"
        const val POOL_ID = "pool-1"
        val submittedModel = CreatePoolModel(
            poolLayoutId = LAYOUT_ID,
            poolName = PRESELECTED_NAME,
            poolId = null,
        )
    }
}
