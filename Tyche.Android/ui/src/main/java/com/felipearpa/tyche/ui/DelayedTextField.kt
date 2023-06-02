package com.felipearpa.tyche.ui

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.core.emptyString
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@Composable
fun DelayedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDelayedValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<Job?>(null) }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            currentJob?.cancel()
            currentJob = scope.async {
                delay(timeMillis = 700)
                if (newValue != value) {
                    onDelayedValueChange(newValue)
                }
            }
            onValueChange(newValue)
        },
        label = label,
        singleLine = true,
        modifier = modifier.testTag("textField")
    )
}

@Preview(showBackground = true)
@Composable
fun DelayedTextFieldPreview() {
    DelayedTextField(
        value = emptyString(),
        onValueChange = {},
        onDelayedValueChange = {},
        label = { Text(text = "Enter some text") }
    )
}