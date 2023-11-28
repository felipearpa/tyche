package com.felipearpa.tyche.bet

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign

@Composable
fun BetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }

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
    )
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