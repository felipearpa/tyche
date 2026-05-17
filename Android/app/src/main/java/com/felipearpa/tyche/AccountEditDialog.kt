package com.felipearpa.tyche

import android.icu.text.BreakIterator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.loading.BallSpinner
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun AccountEditDialog(
    initialUsername: String,
    isSaving: Boolean,
    serverError: String?,
    onSave: (String) -> Unit,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(initialUsername) }
    var showRequiredError by remember { mutableStateOf(false) }

    val trimmed = draft.trim()
    val isRequiredError = showRequiredError && trimmed.isEmpty()
    val isError = isRequiredError || serverError != null

    fun save() {
        if (isSaving) return
        if (trimmed.isEmpty()) {
            showRequiredError = true
            return
        }
        onSave(trimmed)
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.edit_profile_title),
                    modifier = Modifier.weight(1f),
                )

                if (isSaving) {
                    BallSpinner(modifier = Modifier.size(SAVE_SPINNER_SIZE))
                }
            }
        },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { next ->
                    draft = clampToGraphemes(next, USERNAME_MAX_GRAPHEMES)
                    if (showRequiredError && draft.trim().isNotEmpty()) {
                        showRequiredError = false
                    }
                    if (serverError != null) onClearError()
                },
                label = { Text(text = stringResource(id = R.string.username_label)) },
                singleLine = true,
                enabled = !isSaving,
                isError = isError,
                supportingText = {
                    when {
                        isRequiredError -> Text(
                            text = stringResource(id = R.string.username_required_error),
                        )

                        serverError != null -> Text(text = serverError)
                        else -> {}
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = ::save,
                enabled = !isSaving,
            ) {
                Text(text = stringResource(id = SharedR.string.save_action))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSaving,
            ) {
                Text(text = stringResource(id = SharedR.string.cancel_action))
            }
        },
    )
}

private fun clampToGraphemes(value: String, max: Int): String {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(value)
    var count = 0
    var lastBoundary = 0
    var boundary = iterator.next()
    while (boundary != BreakIterator.DONE) {
        count++
        if (count > max) return value.substring(0, lastBoundary)
        lastBoundary = boundary
        boundary = iterator.next()
    }
    return value
}

private val SAVE_SPINNER_SIZE = 24.dp
private const val USERNAME_MAX_GRAPHEMES = 100

@Preview(showSystemUi = true)
@Composable
private fun AccountEditDialogPreview() {
    TycheTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AccountEditDialog(
                initialUsername = "felipearpa",
                isSaving = false,
                serverError = null,
                onSave = {},
                onClearError = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AccountEditDialogSavingPreview() {
    TycheTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AccountEditDialog(
                initialUsername = "felipearpa",
                isSaving = true,
                serverError = null,
                onSave = {},
                onClearError = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AccountEditDialogErrorPreview() {
    TycheTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AccountEditDialog(
                initialUsername = "felipearpa",
                isSaving = false,
                serverError = "Couldn't save your changes. Please try again.",
                onSave = {},
                onClearError = {},
                onDismiss = {},
            )
        }
    }
}
