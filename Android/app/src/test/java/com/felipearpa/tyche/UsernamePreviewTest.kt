package com.felipearpa.tyche

import com.felipearpa.tyche.pool.rank
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UsernamePreviewTest {
    private val accountId = "account-123"
    private val placeholder = "Your username"

    @Test
    fun `given a normal draft when projected then the row shows the trimmed draft with fixed illustrative values`() {
        val model = UsernamePreview.gamblerScore(
            accountId = accountId,
            draft = "  neptune-player  ",
            placeholder = placeholder,
        )

        model.gamblerUsername shouldBe "neptune-player"
        model.gamblerId shouldBe accountId
        model.position shouldBe 1
        model.beforePosition shouldBe 2
        model.rank() shouldBe 1 // production rank rail renders an upward movement of one place
        model.score shouldBe 18
        model.gamblerCount shouldBe null
    }

    @Test
    fun `given a whitespace-only draft when projected then the row shows the placeholder`() {
        val model = UsernamePreview.gamblerScore(
            accountId = accountId,
            draft = "   \n\t ",
            placeholder = placeholder,
        )

        model.gamblerUsername shouldBe placeholder
    }

    @Test
    fun `given an empty draft when projected then the row shows the placeholder`() {
        val model = UsernamePreview.gamblerScore(
            accountId = accountId,
            draft = "",
            placeholder = placeholder,
        )

        model.gamblerUsername shouldBe placeholder
    }

    @Test
    fun `given a 100-grapheme draft when projected then the row carries the full value unchanged`() {
        val draft = "a".repeat(100)

        val model = UsernamePreview.gamblerScore(
            accountId = accountId,
            draft = draft,
            placeholder = placeholder,
        )

        model.gamblerUsername shouldBe draft
    }

    @Test
    fun `given a long draft when projected then the factory preserves it and leaves truncation to the row`() {
        val draft = "very-long-name ".repeat(20).trim()

        val model = UsernamePreview.gamblerScore(
            accountId = accountId,
            draft = draft,
            placeholder = placeholder,
        )

        model.gamblerUsername shouldBe draft
    }

    @Test
    fun `given identical inputs when projected repeatedly then the result is deterministic and does no repository work`() {
        val first = UsernamePreview.gamblerScore(accountId, "same", placeholder)
        val second = UsernamePreview.gamblerScore(accountId, "same", placeholder)

        // Equal outputs for equal inputs: the projection holds no mutable or fetched state.
        first shouldBe second
        first.poolId shouldBe UsernamePreview.POOL_ID
    }
}
