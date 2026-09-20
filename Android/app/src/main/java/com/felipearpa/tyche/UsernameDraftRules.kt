package com.felipearpa.tyche

import java.text.BreakIterator

/**
 * Pure rules that govern the username draft: the grapheme limit, the visible counter, and
 * when the "Save username" action is eligible. Kept free of Compose and view-model state so
 * the behavior is unit-testable on the JVM.
 *
 * Grapheme boundaries use [java.text.BreakIterator], which is ICU-backed on Android and
 * available in plain JVM unit tests.
 */
object UsernameDraftRules {
    const val MAX_GRAPHEMES = 100

    /** Number of grapheme clusters in the draft, matching what the counter displays. */
    fun graphemeCount(value: String): Int {
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(value)
        var count = 0
        while (iterator.next() != BreakIterator.DONE) count++
        return count
    }

    /**
     * Clamps the draft to at most [max] grapheme clusters, preserving the original value when
     * it already fits and never splitting a cluster.
     */
    fun clamp(value: String, max: Int = MAX_GRAPHEMES): String {
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(value)
        var count = 0
        var lastBoundary = 0
        var boundary = iterator.next()
        while (boundary != BreakIterator.DONE) {
            count++
            if (count > max) return value.substring(0, lastBoundary)
            lastBoundary = boundary
            boundary = iterator.next()
        }
        return value
    }

    /** Whether the trimmed draft is empty. */
    fun isEmpty(value: String): Boolean = value.trim().isEmpty()

    /**
     * "Save username" is eligible only when the trimmed draft is non-empty, differs from the
     * initial trimmed username, and no save is in progress.
     */
    fun canSave(draft: String, initial: String, isSaving: Boolean): Boolean {
        val trimmedDraft = draft.trim()
        return trimmedDraft.isNotEmpty() && trimmedDraft != initial.trim() && !isSaving
    }
}
