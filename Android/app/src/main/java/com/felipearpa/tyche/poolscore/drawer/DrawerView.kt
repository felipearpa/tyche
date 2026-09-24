package com.felipearpa.tyche.poolscore.drawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.tyche.DrawerMenu
import com.felipearpa.tyche.DrawerPreviewHost
import com.felipearpa.tyche.DrawerPreviews
import com.felipearpa.tyche.ui.runIfStarted

@Composable
fun DrawerView(
    viewModel: DrawerViewModel,
    onSignOut: () -> Unit,
    onProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val routeLifecycleOwner = LocalLifecycleOwner.current

    DrawerView(
        uiState = uiState,
        onProfile = onProfile,
        logout = {
            // Signing out leaves the route, so a second press before it recomposes does nothing.
            routeLifecycleOwner.runIfStarted {
                viewModel.logout()
                onSignOut()
            }
        },
    )
}

@Composable
private fun DrawerView(
    uiState: PoolScoreListDrawerUiState,
    modifier: Modifier = Modifier,
    onProfile: () -> Unit = {},
    logout: () -> Unit = {},
) {
    DrawerMenu(
        accountId = uiState.accountId,
        username = uiState.username,
        email = uiState.email,
        onProfile = onProfile,
        onSignOut = logout,
        modifier = modifier,
    )
}

private val previewUiState = PoolScoreListDrawerUiState(
    accountId = "account-1",
    email = "felipearpa@email.com",
    username = "felipearpa",
)

@DrawerPreviews
@Composable
private fun DrawerViewPreview() {
    DrawerPreviewHost {
        DrawerView(uiState = previewUiState)
    }
}

@Preview(name = "Right-to-left")
@Composable
private fun RightToLeftDrawerViewPreview() {
    DrawerPreviewHost(layoutDirection = LayoutDirection.Rtl) {
        DrawerView(uiState = previewUiState)
    }
}
