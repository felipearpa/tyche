package com.felipearpa.tyche

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.WindowInsetsRulers
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
 * Hosts the top app bar above the editor content and owns the screen's insets. The bar is
 * pinned: only the editor's column scrolls, so the back button stays visible while the content
 * scrolls and while the software keyboard is open.
 *
 * The activity resizes for the keyboard (`adjustResize`) and draws edge to edge, so the window
 * never pans the bar away; the keyboard is handled here instead. The content consumes the
 * scaffold's system-bar padding, then fits inside the IME ruler: with the keyboard closed the
 * ruler lies beyond the navigation-bar padding and changes nothing, and with it open the
 * content ends at the keyboard's top edge. The keyboard and navigation bar overlap, so neither
 * is added to the other, and the editor scrolls within the space that remains.
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
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fitInside(WindowInsetsRulers.Ime.current),
        )
    }
}
