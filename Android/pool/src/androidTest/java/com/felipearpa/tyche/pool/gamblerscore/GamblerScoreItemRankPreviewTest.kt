package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.felipearpa.tyche.pool.AvatarImageStoreKoinRule
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.ui.theme.TycheTheme
import org.junit.Rule
import org.junit.Test

/**
 * Rendered regression guard for the username editor's pool preview.
 *
 * The preview embeds the production [GamblerScoreItem] with `position = 1` and
 * `beforePosition = 2`, so the production rank rail renders the rank tile `1` and an upward
 * movement of one place. These tests render that row the way the editor does and prove the
 * rank and movement stay exposed for every draft — a normal name, an empty-draft placeholder,
 * and a very long username. The row merges its children into a single accessibility description
 * via `clearAndSetSemantics`, so both values are asserted through the row's content description
 * (the visible pixels are covered by the manual accessibility pass). The assertion is exact
 * rather than a substring: "Up 1 place" is a prefix of the ungrammatical "Up 1 places" this
 * change removed, so a substring probe would no longer discriminate between them. The editor's
 * save-lifecycle states do not change the preview model, so the rail is invariant across them;
 * only the draft username varies here.
 */
class GamblerScoreItemRankPreviewTest {
    @get:Rule(order = 0)
    val avatarImageStoreKoinRule = AvatarImageStoreKoinRule()

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Test
    fun rankAndUpwardMovementAreExposedForANormalDraft() {
        val username = "neptune-player"
        renderPreviewRow(username = username)

        composeTestRule
            .onNodeWithContentDescription("Rank 1, $username, You, 18 points, Up 1 place")
            .assertIsDisplayed()
    }

    @Test
    fun rankAndUpwardMovementAreExposedForAnEmptyDraftPlaceholder() {
        val username = "Your username"
        renderPreviewRow(username = username)

        composeTestRule
            .onNodeWithContentDescription("Rank 1, $username, You, 18 points, Up 1 place")
            .assertIsDisplayed()
    }

    @Test
    fun rankRailSurvivesALongUsername() {
        // A long username must yield/truncate rather than push the rank rail out of the row.
        val username = "very-long-name-".repeat(10)
        renderPreviewRow(username = username)

        composeTestRule
            .onNodeWithContentDescription("Rank 1, $username, You, 18 points, Up 1 place")
            .assertIsDisplayed()
    }

    private fun renderPreviewRow(username: String) {
        composeTestRule.setContent {
            TycheTheme {
                GamblerScoreItem(
                    poolGamblerScore = previewModel(username = username),
                    isCurrentUser = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    private fun previewModel(username: String): PoolGamblerScoreModel {
        // Mirrors the app's UsernamePreview projection: position 1, beforePosition 2 (an upward
        // movement of one place through the production rank rail), score 18.
        return PoolGamblerScoreModel(
            poolId = "username-editor-preview-pool",
            poolName = "username-editor-preview",
            gamblerId = "preview-account",
            gamblerUsername = username,
            position = 1,
            beforePosition = 2,
            score = 18,
            gamblerCount = null,
        )
    }
}
