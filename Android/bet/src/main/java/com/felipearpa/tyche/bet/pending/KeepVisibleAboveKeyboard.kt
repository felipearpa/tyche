package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * Brings the focused card into view once the keyboard has finished opening, and again whenever
 * the card's height changes while it is open (a failed save's banner appearing), so the whole
 * card, both score boxes, their steppers, and the banner, is not
 * left behind the keyboard near the end of a list fitted above it. A scrolling container also
 * tries to reveal a focused child when its viewport shrinks, but for a card at the end of pending
 * bets that did not happen reliably: the field stayed behind the keyboard in about a third of
 * instrumented runs. [cardHeight] reads the card's current height.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeepVisibleAboveKeyboard(
    hasFocus: Boolean,
    bringIntoViewRequester: BringIntoViewRequester,
    cardHeight: () -> Int,
) {
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    val density = LocalDensity.current
    LaunchedEffect(hasFocus, ime, imeTarget, density) {
        if (!hasFocus) return@LaunchedEffect
        snapshotFlow {
            val bottom = ime.getBottom(density)
            val isKeyboardOpen = bottom > 0 && bottom == imeTarget.getBottom(density)
            if (isKeyboardOpen) cardHeight() else null
        }
            .distinctUntilChanged()
            .filterNotNull()
            .collect {
                // The list fits inside the keyboard's ruler during layout, so the frame that
                // reports the finished inset may not have shrunk the viewport yet; wait for it.
                withFrameNanos {}
                bringIntoViewRequester.bringIntoView()
            }
    }
}

/**
 * Clears focus from the card once the keyboard that opened for it has finished closing, so
 * dismissing the keyboard counts as leaving the card, as it does on iOS. The keyboard counts as
 * open while it shows or animates toward showing, so a restart while focus moves from one box to
 * the other is not taken for a dismissal.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClearFocusWhenKeyboardCloses(hasFocus: Boolean, focusManager: FocusManager) {
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    val density = LocalDensity.current
    LaunchedEffect(hasFocus, ime, imeTarget, density) {
        if (!hasFocus) return@LaunchedEffect
        snapshotFlow { ime.getBottom(density) > 0 || imeTarget.getBottom(density) > 0 }
            .distinctUntilChanged()
            .dropWhile { isKeyboardOpen -> !isKeyboardOpen }
            .drop(1)
            .first()
        focusManager.clearFocus()
    }
}
