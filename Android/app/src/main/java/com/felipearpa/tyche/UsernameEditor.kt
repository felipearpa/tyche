package com.felipearpa.tyche

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.tyche.pool.gamblerscore.GamblerScoreItem
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import com.felipearpa.tyche.ui.loading.BallSpinner
import com.felipearpa.tyche.ui.network.NetworkLocalizedException
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.SaveState
import com.felipearpa.ui.state.isFailure
import com.felipearpa.ui.state.isSaved
import com.felipearpa.ui.state.isSaving

@Composable
fun UsernameEditor(
    accountId: String,
    initialUsername: String,
    viewModel: UsernameEditorViewModel,
    onSaved: (String) -> Unit,
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
        accountId = accountId,
        initialUsername = initialUsername,
        saveState = saveState,
        onSave = { viewModel.save(it) },
        onRetry = { viewModel.retry() },
        onResetError = { viewModel.resetError() },
        modifier = modifier,
    )
}

@Composable
private fun UsernameEditor(
    accountId: String,
    initialUsername: String,
    saveState: SaveState<String>,
    onSave: (String) -> Unit,
    onRetry: () -> Unit,
    onResetError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Populated before focus is requested: the field starts with the stored username and a
    // collapsed caret after its final character (position 0 when empty). Initialization is
    // one-shot per presentation — recomposition and SaveState transitions never rewrite this
    // state, so a caret or selection the gambler produces afterwards is preserved. Saveable so
    // a configuration change (dark mode, font scale, rotation) restores the draft and selection
    // instead of resetting to the stored username.
    var draft by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(initialUsernameFieldValue(initialUsername))
    }
    val focusRequester = remember { FocusRequester() }

    val isSaving = saveState.isSaving()
    val placeholder = stringResource(id = R.string.username_preview_placeholder)

    // While a save is in flight the back action is disabled so the request cannot be abandoned.
    BackHandler(enabled = isSaving) {}

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(
                horizontal = LocalBoxSpacing.current.large,
                vertical = LocalBoxSpacing.current.medium,
            ),
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
    ) {
        Text(text = stringResource(id = R.string.edit_user_name_subtitle))

        PoolPreviewSection(
            model = usernamePreviewModel(
                accountId = accountId,
                draft = draft.text,
                placeholder = placeholder,
            ),
        )

        UsernameFieldSection(
            draft = draft,
            saveState = saveState,
            enabled = !isSaving,
            focusRequester = focusRequester,
            onDraftChange = { next ->
                draft = clampedUsernameFieldValue(next)
                if (saveState.isFailure()) onResetError()
            },
        )

        UsernameSaveAction(
            saveState = saveState,
            canSave = UsernameDraftRules.canSave(
                draft = draft.text,
                initial = initialUsername,
                isSaving = isSaving,
            ),
            onSave = { onSave(draft.text.trim()) },
            onRetry = onRetry,
        )
    }
}

@Composable
private fun usernamePreviewModel(
    accountId: String,
    draft: String,
    placeholder: String,
) = UsernamePreview.gamblerScore(
    accountId = accountId,
    draft = draft,
    placeholder = placeholder,
)

@Composable
private fun PoolPreviewSection(
    model: com.felipearpa.tyche.pool.PoolGamblerScoreModel,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.username_preview_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(id = R.string.username_preview_live),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = LocalExtendedColorScheme.current.currentUser,
            )
        }

        GamblerScoreItem(
            poolGamblerScore = model,
            isCurrentUser = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun UsernameFieldSection(
    draft: TextFieldValue,
    saveState: SaveState<String>,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onDraftChange: (TextFieldValue) -> Unit,
) {
    val isEmpty = UsernameDraftRules.isEmpty(draft.text)
    val failureMessage = (saveState as? SaveState.Failure)?.let { failure ->
        if (failure.exception.orDefaultLocalized() is NetworkLocalizedException) {
            stringResource(id = R.string.username_save_network_error)
        } else {
            stringResource(id = R.string.username_save_unknown_error)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.username_label),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${UsernameDraftRules.graphemeCount(draft.text)}/${UsernameDraftRules.MAX_GRAPHEMES}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            singleLine = true,
            enabled = enabled,
            isError = failureMessage != null || isEmpty,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )

        when {
            failureMessage != null -> Text(
                text = failureMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            isEmpty -> Text(
                text = stringResource(id = R.string.username_empty_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            else -> Text(
                text = stringResource(id = R.string.username_field_helper),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UsernameSaveAction(
    saveState: SaveState<String>,
    canSave: Boolean,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    val isSaving = saveState.isSaving()
    val isFailure = saveState.isFailure()
    val savingDescription = stringResource(id = R.string.username_saving_accessibility)

    Button(
        onClick = { if (isFailure) onRetry() else onSave() },
        enabled = isFailure || canSave,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                if (isSaving) contentDescription = savingDescription
            },
    ) {
        if (isSaving) {
            BallSpinner(modifier = Modifier.size(iconSize))
        } else {
            Text(
                text = stringResource(
                    id = if (isFailure) {
                        R.string.username_retry_action
                    } else {
                        R.string.save_username_action
                    },
                ),
            )
        }
    }
}

private val iconSize = 20.dp

@PreviewLightDark
@Composable
private fun UsernameEditorInitialPreview() {
    TycheTheme {
        Surface {
            UsernameEditor(
                accountId = "preview-account",
                initialUsername = "felipearpa",
                saveState = SaveState.Idle,
                onSave = {},
                onRetry = {},
                onResetError = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun UsernameEditorSavingPreview() {
    TycheTheme {
        Surface {
            UsernameEditor(
                accountId = "preview-account",
                initialUsername = "felipearpa",
                saveState = SaveState.Saving("felipe"),
                onSave = {},
                onRetry = {},
                onResetError = {},
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
                accountId = "preview-account",
                initialUsername = "felipearpa",
                saveState = SaveState.Failure("felipe", UnknownLocalizedException()),
                onSave = {},
                onRetry = {},
                onResetError = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
