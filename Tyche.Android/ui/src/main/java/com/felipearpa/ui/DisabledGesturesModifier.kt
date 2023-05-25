package com.felipearpa.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.disabledGestures(disabled: Boolean = true) =
    pointerInput(disabled) {
        if (!disabled) return@pointerInput

        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(pass = PointerEventPass.Initial)
                    .changes
                    .forEach(PointerInputChange::consume)
            }
        }
    }