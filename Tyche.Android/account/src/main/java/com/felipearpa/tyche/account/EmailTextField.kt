package com.felipearpa.tyche.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.ui.theme.boxSpacing

private const val EMAIL_MAX_LENGTH = 320

@Composable
fun EmailTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isValid by remember { mutableStateOf(value.isInitiallyValidEmail()) }

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.small)) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= EMAIL_MAX_LENGTH) {
                    onValueChanged(newValue)
                }
                isValid = value.isValidEmail()
            },
            label = {
                Text(text = stringResource(id = R.string.email_text))
            },
            isError = !isValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            modifier = modifier
        )

        AnimatedVisibility(visible = !isValid) {
            Text(
                text = stringResource(id = R.string.email_validation_failure_message),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun String.isInitiallyValidEmail() = if (this.isEmpty()) true else Email.isValid(this)

private fun String.isValidEmail() = Email.isValid(this)

@Preview(showBackground = true)
@Composable
fun EmailTextFieldWithValidValuePreview() {
    EmailTextField(
        value = "email@tyche.com",
        onValueChanged = {},
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun EmailTextFieldWithInvalidValuePreview() {
    EmailTextField(value = "invalid", onValueChanged = {}, modifier = Modifier.fillMaxWidth())
}