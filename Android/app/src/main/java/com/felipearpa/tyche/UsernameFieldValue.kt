package com.felipearpa.tyche

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The username field's initial value for a new screen presentation: the stored username with a
 * collapsed insertion caret immediately after the final character, or at `0` when empty. The
 * field is populated by this value before focus is requested, and initialization happens once
 * per presentation — afterwards the gambler owns the selection.
 */
fun initialUsernameFieldValue(initialUsername: String) = TextFieldValue(
    text = initialUsername,
    selection = TextRange(initialUsername.length),
)

/**
 * Applies the 100-grapheme clamp to an edited field value while preserving the selection the
 * gambler produced. A value already within the limit is returned unchanged — the editor never
 * moves a caret or resets a selection it did not have to. When the clamp shortens the text,
 * the selection is coerced into the clamped bounds.
 */
fun clampedUsernameFieldValue(next: TextFieldValue): TextFieldValue {
    val clamped = UsernameDraftRules.clamp(next.text)
    if (clamped == next.text) return next
    return next.copy(
        text = clamped,
        selection = TextRange(
            next.selection.start.coerceIn(0, clamped.length),
            next.selection.end.coerceIn(0, clamped.length),
        ),
    )
}
