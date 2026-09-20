package com.felipearpa.tyche

import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.ui.state.SaveState
import com.felipearpa.ui.state.isSaving
import com.felipearpa.tyche.ui.R as SharedR

/**
 * The pushed Username screen: a pinned top app bar over the scrolling [UsernameEditor].
 *
 * The screen and the editor observe the same [UsernameEditorViewModel] instance and therefore
 * the same [SaveState], so the toolbar's back button and the editor's system-back guard can
 * never disagree about an in-flight save. Back navigation only leaves the route; the draft is
 * local to the editor, so leaving discards it without submitting a save or a retry, and
 * reopening seeds the field from the stored username again.
 */
@Composable
fun UsernameEditorScreen(
    accountId: String,
    initialUsername: String,
    viewModel: UsernameEditorViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    UsernameEditorScaffold(
        canNavigateBack = !saveState.isSaving(),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        UsernameEditor(
            accountId = accountId,
            initialUsername = initialUsername,
            viewModel = viewModel,
            onSaved = onSaved,
            modifier = contentModifier,
        )
    }
}

@Composable
internal fun UsernameEditorScreen(
    accountId: String,
    initialUsername: String,
    saveState: SaveState<String>,
    onSave: (String) -> Unit,
    onRetry: () -> Unit,
    onResetError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UsernameEditorScaffold(
        canNavigateBack = !saveState.isSaving(),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        UsernameEditor(
            accountId = accountId,
            initialUsername = initialUsername,
            saveState = saveState,
            onSave = onSave,
            onRetry = onRetry,
            onResetError = onResetError,
            modifier = contentModifier,
        )
    }
}

/**
 * Hosts the top app bar above the editor content. The bar is pinned: only the editor's column
 * scrolls, and the editor applies its own IME padding, so the back button stays visible while
 * the content scrolls and while the software keyboard is open.
 *
 * While a save is in flight the button stays visible but disabled — TalkBack still reaches it
 * and announces the disabled state — matching the editor's system-back guard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsernameEditorScaffold(
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    ResizeWindowForKeyboard()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.username_label)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = canNavigateBack,
                    ) {
                        Icon(
                            painter = painterResource(id = SharedR.drawable.arrow_back),
                            contentDescription = stringResource(id = R.string.navigate_back_action),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        content(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

/**
 * Keeps the top app bar on screen while the software keyboard is open.
 *
 * The activity is edge to edge but keeps the platform's default soft-input mode, which a
 * Compose window resolves to panning: on a small viewport or at a large font scale the whole
 * window slides up when the keyboard opens and takes the top app bar with it. The editor
 * already pads for the IME and scrolls, so this route asks for resize while it is on screen
 * and restores the previous mode when it leaves — every other screen keeps the mode it has.
 */
@Composable
private fun ResizeWindowForKeyboard() {
    val window = LocalActivity.current?.window ?: return

    DisposableEffect(window) {
        val previousMode = window.attributes.softInputMode
        window.applySoftInputAdjustment(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        onDispose { window.applySoftInputMode(previousMode) }
    }
}

/** Replaces only the adjustment bits, leaving the window's soft-input state bits alone. */
private fun Window.applySoftInputAdjustment(adjustment: Int) {
    val mode = attributes.softInputMode and
        WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv()
    applySoftInputMode(mode or adjustment)
}

/**
 * Writes the mode through the window attributes rather than `Window.setSoftInputMode`, which
 * silently ignores a mode of `0` — the platform default this screen has to restore.
 */
private fun Window.applySoftInputMode(mode: Int) {
    attributes = attributes.apply { softInputMode = mode }
}
