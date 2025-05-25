package com.felipearpa.tyche.account.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.emptyAccountBundle
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.exception.LocalizedException
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrNull
import com.felipearpa.tyche.ui.loading.LoadingContainerView
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.ui.state.LoadableViewState
import com.felipearpa.ui.state.isInitial

@Composable
fun EmailLinkSignInView(
    viewModel: EmailLinkSignInViewModel,
    email: String,
    emailLink: String,
    onStartRequested: (AccountBundle) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state.isInitial())
            viewModel.signInWithEmailLink(email = email, emailLink = emailLink)
    }

    EmailLinkSignInView(viewState = state, onStartRequested = onStartRequested)
}

@Composable
fun EmailLinkSignInView(
    viewState: LoadableViewState<AccountBundle>,
    onStartRequested: (AccountBundle) -> Unit = {}
) {
    when (viewState) {
        is LoadableViewState.Failure -> {
            FailureContent(
                localizedException = viewState().localizedOrNull()!!,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = LocalBoxSpacing.current.medium)
            )
        }

        LoadableViewState.Initial, LoadableViewState.Loading -> {
            LoadingContainerView {}
        }

        is LoadableViewState.Success ->
            SuccessContent(
                start = { onStartRequested(viewState.value) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = LocalBoxSpacing.current.medium)
            )
    }
}

@Composable
private fun SuccessContent(start: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mark_email_read),
                    contentDescription = emptyString(),
                    modifier = Modifier.size(width = 64.dp, height = 64.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium)
                ) {
                    Text(
                        text = stringResource(id = R.string.account_verified_title),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = stringResource(id = R.string.account_verified_description),
                        textAlign = TextAlign.Center
                    )
                }

                Button(onClick = start, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.start_action))
                }
            }
        }
    }
}

@Composable
private fun FailureContent(localizedException: LocalizedException, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ExceptionView(localizedException = localizedException)
    }
}

@Preview(showBackground = true)
@Composable
private fun SuccessSignInWithEmailLinkViewPreview() {
    EmailLinkSignInView(viewState = LoadableViewState.Success(emptyAccountBundle()))
}

@Preview(showBackground = true)
@Composable
private fun FailureSignInWithEmailLinkViewPreview() {
    EmailLinkSignInView(viewState = LoadableViewState.Failure(UnknownLocalizedException()))
}