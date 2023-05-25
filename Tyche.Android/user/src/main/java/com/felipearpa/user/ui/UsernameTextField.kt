package com.felipearpa.user.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.felipearpa.user.type.Username

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    label: @Composable () -> Unit,
    validationFailureContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var isError by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= 16) {
                    onValueChanged(newValue)
                }
            },
            label = label,
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    isError = !Username.hasPattern(value)
                    println("isError: $isError")
                }
            ),
            modifier = modifier
        )

        if (isError) {
            validationFailureContent()
        }
    }
}