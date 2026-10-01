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
import com.felipearpa.tyche.account.bygoogle.GoogleSignInState
import com.felipearpa.tyche.account.bygoogle.googleSignInViewModel
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.ui.exception.ExceptionAlertDialog
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun SocialSignInRow(
    onAuthenticate: (AccountBundle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val googleViewModel = googleSignInViewModel()
    val googleState by googleViewModel.state.collectAsState()
    val context = LocalContext.current

    SocialSignInRow(
        googleState = googleState,
        onSignInWithGoogle = { googleViewModel.signInWithGoogle(activityContext = context) },
        onDismissGoogleFailure = googleViewModel::dismissFailure,
        onAuthenticate = onAuthenticate,
        onAuthenticationHandled = googleViewModel::authenticationHandled,
        modifier = modifier,
    )
}

/**
 * Google sign-in actions for [googleState]. The Google button is disabled while an attempt is
 * busy. An [GoogleSignInState.Authenticated] account is passed to [onAuthenticate] once, then
 * reported through [onAuthenticationHandled] so the state moves on and is not delivered again.
 */
@Composable
fun SocialSignInRow(
    googleState: GoogleSignInState,
    onSignInWithGoogle: () -> Unit,
    onDismissGoogleFailure: () -> Unit,
    onAuthenticate: (AccountBundle) -> Unit,
    onAuthenticationHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = LocalBoxSpacing.current.medium,
            alignment = Alignment.CenterHorizontally,
        ),
    ) {
        IconButton(
            onClick = onSignInWithGoogle,
            enabled = !googleState.isBusy,
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
    }

    if (googleState is GoogleSignInState.Failed) {
        ExceptionAlertDialog(
            exception = googleState.exception,
            onDismiss = onDismissGoogleFailure,
        )
    }

    if (googleState is GoogleSignInState.Authenticated) {
        LaunchedEffect(googleState) {
            onAuthenticate(googleState.accountBundle)
            onAuthenticationHandled()
        }
    }
}

private val iconSize = 32.dp

@Preview(showBackground = true, name = "Initial")
@Composable
private fun InitialSocialSignInRowPreview() {
    TycheTheme {
        SocialSignInRow(
            googleState = GoogleSignInState.Idle,
            onSignInWithGoogle = {},
            onDismissGoogleFailure = {},
            onAuthenticate = {},
            onAuthenticationHandled = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun LoadingSocialSignInRowPreview() {
    TycheTheme {
        SocialSignInRow(
            googleState = GoogleSignInState.InProgress,
            onSignInWithGoogle = {},
            onDismissGoogleFailure = {},
            onAuthenticate = {},
            onAuthenticationHandled = {},
        )
    }
}

@Preview(showBackground = true, name = "Failure")
@Composable
private fun FailureSocialSignInRowPreview() {
    TycheTheme {
        SocialSignInRow(
            googleState = GoogleSignInState.Failed(UnknownLocalizedException()),
            onSignInWithGoogle = {},
            onDismissGoogleFailure = {},
            onAuthenticate = {},
            onAuthenticationHandled = {},
        )
    }
}
