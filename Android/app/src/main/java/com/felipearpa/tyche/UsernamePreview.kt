package com.felipearpa.tyche

import com.felipearpa.tyche.pool.PoolGamblerScoreModel

/**
 * Builds the illustrative [PoolGamblerScoreModel] that backs the username editor's live pool
 * preview.
 *
 * The projection is pure: it combines the signed-in account id and the current username draft
 * with fixed illustrative rank and score values. It never reads a repository, never performs a
 * network request, and never persists anything. Only the account id and draft are real user
 * data; rank and score are constant examples.
 */
object UsernamePreview {
    /** Fixed illustrative rank shown in the preview row. */
    const val POSITION = 1

    /**
     * Fixed illustrative previous rank; the production rank calculation
     * (`beforePosition - position`) renders an upward movement of one place.
     */
    const val BEFORE_POSITION = 2

    /** Fixed illustrative score shown in the preview row. */
    const val SCORE = 18

    /** Local-only pool identity for the preview row; never sent anywhere. */
    const val POOL_ID = "username-editor-preview-pool"
    const val POOL_NAME = "username-editor-preview"

    /**
     * Projects the account id and draft username into the production leaderboard model.
     *
     * @param accountId the signed-in account id, so the row resolves the current avatar.
     * @param draft the live username draft; trimmed of surrounding whitespace.
     * @param placeholder the localized text shown when the trimmed draft is empty.
     * @return a presentation-only model with fixed illustrative rank, score, and an upward
     *   movement of one place rendered by the production rank rail.
     */
    fun gamblerScore(
        accountId: String,
        draft: String,
        placeholder: String,
    ): PoolGamblerScoreModel {
        val trimmed = draft.trim()
        return PoolGamblerScoreModel(
            poolId = POOL_ID,
            poolName = POOL_NAME,
            gamblerId = accountId,
            gamblerUsername = trimmed.ifEmpty { placeholder },
            position = POSITION,
            beforePosition = BEFORE_POSITION,
            score = SCORE,
            gamblerCount = null,
        )
    }
}
