package com.felipearpa.user.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.felipearpa.core.emptyString
import com.felipearpa.user.R
import com.felipearpa.user.type.Password

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    label: @Composable () -> Unit,
    validationFailureContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isError by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val passwordIconResource =
        if (isPasswordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= 100) {
                    onValueChanged(newValue)
                }
            },
            label = label,
            isError = isError,
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    isError = !Password.hasPattern(value)
                }
            ),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        painter = painterResource(id = passwordIconResource),
                        contentDescription = emptyString(),
                        modifier = Modifier.size(24.dp, 24.dp)
                    )
                }
            },
            modifier = modifier
        )

        if (isError) {
            validationFailureContent()
        }
    }
}