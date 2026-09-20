package com.felipearpa.tyche

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UsernameDraftRulesTest {
    // "e" + combining acute accent (U+0301): a single grapheme cluster made of two scalars.
    private val combiningGrapheme = "é"

    @Test
    fun `given a draft within the limit when clamped then it is returned unchanged`() {
        val draft = "a".repeat(100)
        UsernameDraftRules.clamp(draft) shouldBe draft
    }

    @Test
    fun `given a draft over the limit when clamped then it is cut to 100 graphemes`() {
        val draft = "a".repeat(150)
        UsernameDraftRules.graphemeCount(UsernameDraftRules.clamp(draft)) shouldBe 100
    }

    @Test
    fun `given multi-scalar graphemes when counted then whole clusters are counted once`() {
        UsernameDraftRules.graphemeCount(combiningGrapheme) shouldBe 1
        UsernameDraftRules.graphemeCount("abc") shouldBe 3
    }

    @Test
    fun `given multi-scalar graphemes over the limit when clamped then whole clusters are preserved`() {
        val draft = combiningGrapheme.repeat(120)
        UsernameDraftRules.graphemeCount(UsernameDraftRules.clamp(draft)) shouldBe 100
    }

    @Test
    fun `given a whitespace-only draft when checked then it is empty`() {
        UsernameDraftRules.isEmpty("   \n ") shouldBe true
        UsernameDraftRules.isEmpty(" a ") shouldBe false
    }

    @Test
    fun `given an empty trimmed draft then save is not eligible`() {
        UsernameDraftRules.canSave(draft = "   ", initial = "old", isSaving = false) shouldBe false
    }

    @Test
    fun `given a draft equal to the initial username then save is not eligible`() {
        UsernameDraftRules.canSave(draft = "  neptune  ", initial = "neptune", isSaving = false) shouldBe false
    }

    @Test
    fun `given an in-flight save then save is not eligible even when changed`() {
        UsernameDraftRules.canSave(draft = "changed", initial = "old", isSaving = true) shouldBe false
    }

    @Test
    fun `given a changed non-empty draft and no save in flight then save is eligible`() {
        UsernameDraftRules.canSave(draft = "  changed ", initial = "old", isSaving = false) shouldBe true
    }
}
