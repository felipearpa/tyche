package com.pipel.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.pipel.core.empty
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@Composable
fun SearcherTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<Job?>(null) }

    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            currentJob?.cancel()
            currentJob = scope.async {
                delay(700)
            }
            onValueChange(text)
        },
        label = label,
        singleLine = true,
        modifier = modifier.testTag("textField")
    )
}

@Preview(showBackground = true)
@Composable
fun SearcherTextFieldPreview() {
    MaterialTheme {
        SearcherTextField(
            value = String.empty(),
            onValueChange = {},
            label = { Text(text = "Enter some text") }
        )
    }
}