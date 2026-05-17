package com.felipearpa.tyche

import android.icu.text.BreakIterator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.loading.BallSpinner
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.SaveState
import com.felipearpa.ui.state.isFailure
import com.felipearpa.ui.state.isSaved
import com.felipearpa.ui.state.isSaving
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun UsernameEditor(
    initialUsername: String,
    viewModel: UsernameEditorViewModel,
    onSaved: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { viewModel.reset() }
    }

    LaunchedEffect(saveState) {
        val current = saveState
        if (current.isSaved()) onSaved(current.value)
    }

    UsernameEditor(
        initialUsername = initialUsername,
        saveState = saveState,
        onSave = { viewModel.save(it) },
        onRetry = { viewModel.retry() },
        onResetError = { viewModel.resetError() },
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

@Composable
private fun UsernameEditor(
    initialUsername: String,
    saveState: SaveState<String>,
    onSave: (String) -> Unit,
    onRetry: () -> Unit,
    onResetError: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(initialUsername) }
    var showRequiredError by remember { mutableStateOf(false) }

    val trimmed = draft.trim()

    fun save() {
        if (trimmed.isEmpty()) {
            showRequiredError = true
            return
        }
        if (trimmed == initialUsername.trim()) {
            onDismiss()
            return
        }
        onSave(trimmed)
    }

    UsernameEditorLayout(
        draft = draft,
        onDraftChange = { next ->
            draft = clampToGraphemes(next, USERNAME_MAX_GRAPHEMES)
            if (showRequiredError && draft.trim().isNotEmpty()) {
                showRequiredError = false
            }
        },
        saveState = saveState,
        showRequiredError = showRequiredError && trimmed.isEmpty(),
        onSave = ::save,
        onRetry = onRetry,
        onDismiss = onDismiss,
        onResetError = onResetError,
        modifier = modifier,
    )
}

@Composable
private fun UsernameEditorLayout(
    draft: String,
    onDraftChange: (String) -> Unit,
    saveState: SaveState<String>,
    showRequiredError: Boolean,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onResetError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(
                horizontal = LocalBoxSpacing.current.large,
                vertical = LocalBoxSpacing.current.medium,
            ),
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.edit_username_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )

            if (saveState.isSaving()) {
                BallSpinner(modifier = Modifier.size(iconSize))
            } else if (saveState.isFailure()) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.error),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                )
            }
        }

        Text(text = stringResource(id = R.string.edit_user_name_subtitle))

        UsernameEditorContent(
            draft = draft,
            onDraftChange = onDraftChange,
            saveState = saveState,
            showRequiredError = showRequiredError,
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            UsernameEditorConfirmButton(
                saveState = saveState,
                onSave = onSave,
                onRetry = onRetry,
            )

            UsernameEditorDismissButton(
                saveState = saveState,
                onDismiss = onDismiss,
                onResetError = onResetError,
            )
        }
    }
}

@Composable
private fun UsernameEditorContent(
    draft: String,
    onDraftChange: (String) -> Unit,
    saveState: SaveState<String>,
    showRequiredError: Boolean,
) {
    UsernameForm(
        draft = draft,
        onDraftChange = onDraftChange,
        showRequiredError = showRequiredError,
        enabled = saveState !is SaveState.Saving,
    )
}

@Composable
private fun UsernameEditorConfirmButton(
    saveState: SaveState<String>,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    val isSaving = saveState is SaveState.Saving
    val failure = saveState as? SaveState.Failure

    if (failure != null) {
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = SharedR.string.retry_action))
        }
    } else {
        Button(
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = SharedR.string.save_action))
        }
    }
}

@Composable
private fun UsernameEditorDismissButton(
    saveState: SaveState<String>,
    onDismiss: () -> Unit,
    onResetError: () -> Unit,
) {
    val isSaving = saveState is SaveState.Saving
    val failure = saveState as? SaveState.Failure

    if (failure != null) {
        OutlinedButton(
            onClick = onResetError,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = SharedR.string.cancel_action))
        }
    } else {
        OutlinedButton(
            onClick = onDismiss,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = SharedR.string.cancel_action))
        }
    }
}

@Composable
private fun UsernameForm(
    draft: String,
    onDraftChange: (String) -> Unit,
    showRequiredError: Boolean,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = draft,
        onValueChange = onDraftChange,
        label = { Text(text = stringResource(id = R.string.username_label)) },
        singleLine = true,
        enabled = enabled,
        isError = showRequiredError,
        modifier = Modifier.fillMaxWidth(),
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

private const val USERNAME_MAX_GRAPHEMES = 100
private val iconSize = 24.dp

@PreviewLightDark
@Composable
private fun UsernameEditorInitialPreview() {
    TycheTheme {
        Surface {
            UsernameEditor(
                initialUsername = "felipearpa",
                saveState = SaveState.Idle,
                onSave = {},
                onRetry = {},
                onResetError = {},
                onDismiss = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun UsernameEditorLoadingPreview() {
    TycheTheme {
        Surface {
            UsernameEditor(
                initialUsername = "felipearpa",
                saveState = SaveState.Saving("felipearpa"),
                onSave = {},
                onRetry = {},
                onResetError = {},
                onDismiss = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun UsernameEditorFailurePreview() {
    TycheTheme {
        Surface {
            UsernameEditor(
                initialUsername = "felipearpa",
                saveState = SaveState.Failure("felipearpa", UnknownLocalizedException()),
                onSave = {},
                onRetry = {},
                onResetError = {},
                onDismiss = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
