package com.felipearpa.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.core.emptyString
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
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