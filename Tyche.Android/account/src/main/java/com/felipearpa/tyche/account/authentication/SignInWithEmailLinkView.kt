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
import com.felipearpa.tyche.ui.exception.localizedExceptionOrNull
import com.felipearpa.tyche.ui.progress.ProgressContainerView
import com.felipearpa.tyche.ui.state.LoadableViewState
import com.felipearpa.tyche.ui.state.isInitial
import com.felipearpa.tyche.ui.theme.boxSpacing

@Composable
fun SignInWithEmailLinkView(
    viewModel: SignInWithEmailLinkViewModel,
    email: String,
    emailLink: String,
    onStartRequested: (AccountBundle) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state.isInitial())
            viewModel.signInWithEmailLink(email = email, emailLink = emailLink)
    }

    SignInWithEmailLinkView(viewState = state, onStartRequested = onStartRequested)
}

@Composable
fun SignInWithEmailLinkView(
    viewState: LoadableViewState<AccountBundle>,
    onStartRequested: (AccountBundle) -> Unit = {}
) {
    when (viewState) {
        is LoadableViewState.Failure -> {
            FailureContent(
                localizedException = viewState().localizedExceptionOrNull()!!,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.boxSpacing.medium)
            )
        }

        LoadableViewState.Initial, LoadableViewState.Loading -> {
            ProgressContainerView {}
        }

        is LoadableViewState.Success -> {
            SuccessContent(
                start = { onStartRequested(viewState.value) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.boxSpacing.medium)
            )
        }
    }
}

@Composable
private fun SuccessContent(start: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.large)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mark_email_read),
                    contentDescription = emptyString(),
                    modifier = Modifier.size(width = 64.dp, height = 64.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.medium)
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
        ExceptionView(exception = localizedException)
    }
}

@Preview(showBackground = true)
@Composable
private fun SuccessSignInWithEmailLinkViewPreview() {
    SignInWithEmailLinkView(viewState = LoadableViewState.Success(emptyAccountBundle()))
}

@Preview(showBackground = true)
@Composable
private fun FailureSignInWithEmailLinkViewPreview() {
    SignInWithEmailLinkView(viewState = LoadableViewState.Failure(UnknownLocalizedException()))
}