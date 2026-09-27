package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun BetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var isFocused by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    KeepVisibleAboveKeyboard(isFocused = isFocused, bringIntoViewRequester = bringIntoViewRequester)

    OutlinedTextField(
        value = textFieldValue,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        onValueChange = { newTextFieldValue ->
            var newValue = newTextFieldValue.text.ifEmpty { "0" }

            if (newValue.isValid()) {
                newValue = newValue.normalize()
                textFieldValue = newTextFieldValue.apply(value = newValue)
                if (newValue != value) {
                    onValueChange(newValue)
                }
            }
        },
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState -> isFocused = focusState.isFocused },
    )
}

/**
 * Brings the focused field into view once the keyboard has finished opening, so a score near
 * the end of a list fitted above the keyboard is not left behind it. A scrolling container
 * also tries to reveal a focused child when its viewport shrinks, but for a score at the end
 * of pending bets that did not happen reliably: the field stayed behind the keyboard in about
 * a third of instrumented runs.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun KeepVisibleAboveKeyboard(
    isFocused: Boolean,
    bringIntoViewRequester: BringIntoViewRequester,
) {
    val ime = WindowInsets.ime
    val imeTarget = WindowInsets.imeAnimationTarget
    val density = LocalDensity.current
    LaunchedEffect(isFocused, ime, imeTarget, density) {
        if (!isFocused) return@LaunchedEffect
        snapshotFlow {
            val bottom = ime.getBottom(density)
            bottom > 0 && bottom == imeTarget.getBottom(density)
        }
            .distinctUntilChanged()
            .filter { isKeyboardOpen -> isKeyboardOpen }
            .collect { bringIntoViewRequester.bringIntoView() }
    }
}

private fun String.normalize() =
    this.toInt().toString()

private fun TextFieldValue.apply(value: String): TextFieldValue {
    return if (this.text.isEmpty()) this.copy(
        text = value,
        selection = TextRange(index = value.length)
    ) else this.copy(text = value)
}

private fun String.isValid() =
    this.length in 1..3 && this.all { char -> char.isDigit() }