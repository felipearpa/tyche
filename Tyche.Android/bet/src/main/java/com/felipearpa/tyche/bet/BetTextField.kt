package com.felipearpa.tyche.bet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDelayedValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<Job?>(null) }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }

    BasicTextField(
        value = textFieldValue,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimary
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
        modifier = modifier
            .testTag("textField")
            .clip(CircleShape)
            .background(color = MaterialTheme.colorScheme.primary),
        onValueChange = { newTextFieldValue ->
            var newValue = newTextFieldValue.text.ifEmpty { "0" }

            if (isValid(newValue)) {
                newValue = newValue.toInt().toString()

                textFieldValue = if (newTextFieldValue.text.isEmpty()) newTextFieldValue.copy(
                    text = newValue,
                    selection = TextRange(index = newValue.length)
                ) else newTextFieldValue.copy(text = newValue)

                if (newValue != value) {
                    onValueChange(newValue)
                }

                currentJob?.cancel()
                currentJob = coroutineScope.launch {
                    delay(timeMillis = 700)
                    if (newValue != value) {
                        onDelayedValueChange(newValue)
                    }
                }
            }
        })
}

private fun isValid(value: String) =
    value.length in 1..3 && value.all { char -> char.isDigit() }

@Preview(showBackground = true)
@Composable
fun BetTextFieldPreview() {
    BetTextField(
        value = "10",
        onValueChange = { },
        onDelayedValueChange = { },
        modifier = Modifier.width(64.dp)
    )
}