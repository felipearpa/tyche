package com.felipearpa.tyche.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.ui.TextFieldValidation
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun RawEmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    EmailTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        validation = null,
    )
}

@Composable
fun EmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    validation: TextFieldValidation? = TextFieldValidation(
        isValid = { email -> email.isNotBlank() },
        errorMessage = stringResource(id = R.string.email_validation_failure_message),
    ),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isTouched by interactionSource.collectIsFocusedAsState()
    val shouldShowError by remember(value) {
        derivedStateOf { isTouched && !(validation?.isValid(value) ?: true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small)) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= EMAIL_MAX_LENGTH) {
                    onValueChange(newValue)
                }
            },
            label = { Text(text = stringResource(id = R.string.email_label)) },
            isError = shouldShowError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            interactionSource = interactionSource,
            modifier = modifier,
        )

        validation?.errorMessage?.let {
            AnimatedVisibility(visible = shouldShowError) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private const val EMAIL_MAX_LENGTH = 320

@Preview(showBackground = true)
@Composable
private fun RawEmailTextFieldPreview() {
    RawEmailTextField(
        value = "email@tyche.com",
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
private fun EmailTextFieldPreview() {
    EmailTextField(
        value = "email@tyche.com",
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
