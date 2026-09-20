package com.felipearpa.tyche

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UsernameFieldValueTest {
    @Test
    fun `given a stored username when initialized then the field is populated with a collapsed caret after the final character`() {
        val value = initialUsernameFieldValue("felipearpa")

        value.text shouldBe "felipearpa"
        value.selection shouldBe TextRange("felipearpa".length)
        value.selection.collapsed.shouldBeTrue()
    }

    @Test
    fun `given an empty stored username when initialized then the caret is placed at position 0`() {
        val value = initialUsernameFieldValue("")

        value.text shouldBe ""
        value.selection shouldBe TextRange(0)
        value.selection.collapsed.shouldBeTrue()
    }

    @Test
    fun `given an edit within the limit then the value and the gambler's selection are returned unchanged`() {
        val edited = TextFieldValue(text = "felipearpa", selection = TextRange(2, 5))

        val result = clampedUsernameFieldValue(edited)

        result shouldBe edited
    }

    @Test
    fun `given a collapsed caret moved by the gambler then it is preserved across the clamp`() {
        val edited = TextFieldValue(text = "felipearpa", selection = TextRange(3))

        val result = clampedUsernameFieldValue(edited)

        result.selection shouldBe TextRange(3)
    }

    @Test
    fun `given an edit over the limit then the text is clamped to 100 graphemes and the selection is coerced into bounds`() {
        val overLimit = "a".repeat(150)
        val edited = TextFieldValue(text = overLimit, selection = TextRange(overLimit.length))

        val result = clampedUsernameFieldValue(edited)

        UsernameDraftRules.graphemeCount(result.text) shouldBe 100
        result.selection shouldBe TextRange(result.text.length)
    }

    @Test
    fun `given a selection inside the clamped bounds then the clamp keeps it where the gambler put it`() {
        val overLimit = "a".repeat(150)
        val edited = TextFieldValue(text = overLimit, selection = TextRange(10, 20))

        val result = clampedUsernameFieldValue(edited)

        result.selection shouldBe TextRange(10, 20)
    }
}
