package com.felipearpa.tyche.account.byemail

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
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.emptyAccountBundle
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.exception.LocalizedException
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.loading.LoadingContainerView
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.ui.state.LoadableViewState
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun EmailLinkSignInView(
    viewModel: EmailLinkSignInViewModel,
    email: String,
    emailLink: String,
    onStart: (AccountBundle) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val signInWithEmailAndLink =
        { viewModel.signInWithEmailLink(email = email, emailLink = emailLink) }

    EmailLinkSignInView(
        state = state,
        onStart = onStart,
        onRetry = { signInWithEmailAndLink() },
    )

    LaunchedEffect(Unit) {
        signInWithEmailAndLink()
    }
}

@Composable
fun EmailLinkSignInView(
    state: LoadableViewState<AccountBundle>,
    onStart: (AccountBundle) -> Unit = {},
    onRetry: () -> Unit,
) {
    when (state) {
        is LoadableViewState.Failure -> {
            FailureContent(
                localizedException = state().localizedOrDefault(),
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = LocalBoxSpacing.current.medium),
            )
        }

        LoadableViewState.Initial, LoadableViewState.Loading -> {
            LoadingContainerView {}
        }

        is LoadableViewState.Success ->
            SuccessContent(
                onStart = { onStart(state.value) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = LocalBoxSpacing.current.medium),
            )
    }
}

@Composable
private fun SuccessContent(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mark_email_read),
                    contentDescription = emptyString(),
                    modifier = Modifier.size(iconSize),
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                ) {
                    Text(
                        text = stringResource(id = R.string.account_verified_title),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Text(text = stringResource(id = R.string.account_verified_description))
                }

                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.start_action))
                }
            }
        }
    }
}

private val iconSize = 64.dp

@Composable
private fun FailureContent(
    localizedException: LocalizedException,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large)) {
            ExceptionView(localizedException = localizedException)

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = SharedR.string.retry_action))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuccessSignInWithEmailLinkViewPreview() {
    EmailLinkSignInView(state = LoadableViewState.Success(emptyAccountBundle()), onRetry = {})
}

@Preview(showBackground = true)
@Composable
private fun FailureSignInWithEmailLinkViewPreview() {
    EmailLinkSignInView(
        state = LoadableViewState.Failure(UnknownLocalizedException()),
        onRetry = {},
    )
}
