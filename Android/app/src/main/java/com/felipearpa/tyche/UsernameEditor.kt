package com.felipearpa.tyche

import android.icu.text.BreakIterator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.exception.LocalizedException
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.loading.LoadingContainerView
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.SaveState
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

    LaunchedEffect(Unit) { viewModel.reset() }

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

    if (saveState.isSaving()) {
        LoadingContainerView {
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
            )
        }
    } else {
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
            .fillMaxSize()
            .padding(
                horizontal = LocalBoxSpacing.current.large,
                vertical = LocalBoxSpacing.current.medium,
            ),
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
    ) {
        Text(
            text = stringResource(id = R.string.edit_username_title),
            style = MaterialTheme.typography.titleLarge,
        )

        Box(modifier = Modifier.weight(1f)) {
            UsernameEditorContent(
                draft = draft,
                onDraftChange = onDraftChange,
                saveState = saveState,
                showRequiredError = showRequiredError,
            )
        }

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
    val failure = saveState as? SaveState.Failure

    if (failure != null) {
        ExceptionContent(
            exception = failure.exception as? LocalizedException
                ?: failure.exception.localizedOrDefault(),
        )
    } else {
        UsernameForm(
            draft = draft,
            onDraftChange = onDraftChange,
            showRequiredError = showRequiredError,
            enabled = saveState !is SaveState.Saving,
        )
    }
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
        supportingText = {
            if (showRequiredError) {
                Text(text = stringResource(id = R.string.username_required_error))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ExceptionContent(exception: LocalizedException) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ExceptionView(localizedException = exception)
    }
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

@PreviewLightDark
@Composable
fun UsernameEditorInitialPreview() {
    TycheTheme(dynamicColor = false) {
        Surface {
            UsernameEditor(
                initialUsername = "felipearpa",
                saveState = SaveState.Idle,
                onSave = {},
                onRetry = {},
                onResetError = {},
                onDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
fun UsernameEditorLoadingPreview() {
    TycheTheme(dynamicColor = false) {
        Surface {
            UsernameEditor(
                initialUsername = "felipearpa",
                saveState = SaveState.Saving("felipearpa"),
                onSave = {},
                onRetry = {},
                onResetError = {},
                onDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
fun UsernameEditorFailurePreview() {
    TycheTheme(dynamicColor = false) {
        Surface {
            UsernameEditor(
                initialUsername = "felipearpa",
                saveState = SaveState.Failure("felipearpa", UnknownLocalizedException()),
                onSave = {},
                onRetry = {},
                onResetError = {},
                onDismiss = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
