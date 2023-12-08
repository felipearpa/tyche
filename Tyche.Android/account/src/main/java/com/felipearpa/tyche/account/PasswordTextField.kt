package com.felipearpa.tyche.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.session.type.Password
import com.felipearpa.tyche.core.emptyString

@Composable
fun PasswordTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isValid by remember { mutableStateOf(value.isInitiallyValidPassword()) }
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
            label = { Text(text = stringResource(id = R.string.password_text)) },
            isError = !isValid,
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { isValid = value.isValidPassword() }
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

        if (!isValid) {
            Text(
                text = stringResource(id = R.string.password_validation_failure_message),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun String.isInitiallyValidPassword() = if (this.isEmpty()) true else Password.isValid(this)

private fun String.isValidPassword() = Password.isValid(this)

@Preview(showBackground = true)
@Composable
fun PasswordTextFieldWithValidValuePreview() {
    PasswordTextField(
        value = "#Valid2Password#",
        onValueChanged = {},
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun PasswordTextFieldWithInvalidValuePreview() {
    PasswordTextField(value = "invalid", onValueChanged = {}, modifier = Modifier.fillMaxWidth())
}