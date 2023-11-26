package com.felipearpa.tyche.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
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
import com.felipearpa.tyche.account.R
import com.felipearpa.data.account.type.Username

@Composable
fun UsernameTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isValid by remember { mutableStateOf(true) }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= 16) {
                    onValueChanged(newValue)
                }
            },
            label = {
                Text(text = stringResource(id = R.string.username_text))
            },
            isError = !isValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    isValid = Username.isValid(value)
                }
            ),
            modifier = modifier
        )

        if (!isValid) {
            Text(
                text = stringResource(id = R.string.username_validation_failure_message),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}