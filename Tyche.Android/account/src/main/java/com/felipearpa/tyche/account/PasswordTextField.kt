package com.felipearpa.tyche.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val passwordIconResource =
        if (isPasswordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
    val interactionSource = remember { MutableInteractionSource() }
    val isTouched by interactionSource.collectIsFocusedAsState()
    val shouldShowError by remember(value) { derivedStateOf { isTouched && !Email.isValid(value) } }

    Column(verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small)) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= 100) {
                    onValueChange(newValue)
                }
            },
            label = { Text(text = stringResource(id = R.string.password_text)) },
            isError = shouldShowError,
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        painter = painterResource(id = passwordIconResource),
                        contentDescription = emptyString(),
                        modifier = Modifier.size(24.dp, 24.dp),
                    )
                }
            },
            interactionSource = interactionSource,
            modifier = modifier,
        )

        AnimatedVisibility(visible = shouldShowError) {
            Text(
                text = stringResource(id = R.string.password_validation_failure_message),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PasswordTextFieldPreview() {
    PasswordTextField(
        value = "#Valid2Password#",
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
