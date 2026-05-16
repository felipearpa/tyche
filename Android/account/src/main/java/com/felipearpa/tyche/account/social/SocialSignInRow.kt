package com.felipearpa.tyche.account.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.account.bygoogle.googleSignInViewModel
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.ui.exception.ExceptionAlertDialog
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.LoadableViewState
import com.felipearpa.ui.state.isLoading
import com.felipearpa.ui.state.onFailure
import com.felipearpa.ui.state.onSuccess

@Composable
fun SocialSignInRow(
    onAuthenticate: (AccountBundle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val googleViewModel = googleSignInViewModel()
    val googleState by googleViewModel.state.collectAsState(initial = LoadableViewState.Initial)
    val context = LocalContext.current

    SocialSignInRow(
        googleState = googleState,
        onSignInWithGoogle = { googleViewModel.signInWithGoogle(context = context) },
        onResetGoogleState = { googleViewModel.reset() },
        onAuthenticate = onAuthenticate,
        modifier = modifier,
    )
}

@Composable
fun SocialSignInRow(
    googleState: LoadableViewState<AccountBundle>,
    onSignInWithGoogle: () -> Unit,
    onResetGoogleState: () -> Unit,
    onAuthenticate: (AccountBundle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOverlayVisible = googleState.isLoading() || googleState is LoadableViewState.Success

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = LocalBoxSpacing.current.medium,
            alignment = Alignment.CenterHorizontally,
        ),
    ) {
        IconButton(
            onClick = onSignInWithGoogle,
            enabled = !isOverlayVisible,
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
    }

    googleState.onFailure { exception ->
        ExceptionAlertDialog(
            exception = exception.localizedOrDefault(),
            onDismiss = onResetGoogleState,
        )
    }

    LaunchedEffect(googleState) {
        googleState.onSuccess { accountBundle ->
            onAuthenticate(accountBundle)
        }
    }
}

private val iconSize = 32.dp

@Preview(showBackground = true, name = "Initial")
@Composable
private fun InitialSocialSignInRowPreview() {
    TycheTheme {
        SocialSignInRow(
            googleState = LoadableViewState.Initial,
            onSignInWithGoogle = {},
            onResetGoogleState = {},
            onAuthenticate = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun LoadingSocialSignInRowPreview() {
    TycheTheme {
        SocialSignInRow(
            googleState = LoadableViewState.Loading,
            onSignInWithGoogle = {},
            onResetGoogleState = {},
            onAuthenticate = {},
        )
    }
}

@Preview(showBackground = true, name = "Failure")
@Composable
private fun FailureSocialSignInRowPreview() {
    TycheTheme {
        SocialSignInRow(
            googleState = LoadableViewState.Failure(UnknownLocalizedException()),
            onSignInWithGoogle = {},
            onResetGoogleState = {},
            onAuthenticate = {},
        )
    }
}
