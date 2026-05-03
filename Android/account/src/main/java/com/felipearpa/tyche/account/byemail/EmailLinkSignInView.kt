package com.felipearpa.tyche.account.byemail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
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
        email = email,
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
    email: String,
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
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = LocalBoxSpacing.current.medium),
            )
        }

        LoadableViewState.Initial, LoadableViewState.Loading -> {
            LoadingContainerView {}
        }

        is LoadableViewState.Success ->
            SuccessContent(
                email = email,
                onStart = { onStart(state.value) },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = LocalBoxSpacing.current.medium),
            )
    }
}

@Composable
private fun SuccessContent(email: String, onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.mark_email_read),
                contentDescription = emptyString(),
                modifier = Modifier.size(iconSize),
            )

            Spacer(modifier = Modifier.height(LocalBoxSpacing.current.large))

            Text(
                text = stringResource(id = R.string.account_verified_title),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(LocalBoxSpacing.current.medium))

            Text(
                text = stringResource(id = R.string.account_verified_description),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(LocalBoxSpacing.current.large))

            VerifiedEmailPill(email = email)
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LocalBoxSpacing.current.medium),
        ) {
            Text(text = stringResource(id = R.string.start_action))
        }
    }
}

@Composable
private fun VerifiedEmailPill(email: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(LocalBoxSpacing.current.large))
            .background(LocalExtendedColorScheme.current.successContainer)
            .padding(
                horizontal = LocalBoxSpacing.current.medium,
                vertical = LocalBoxSpacing.current.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
    ) {
        Icon(
            painter = painterResource(id = SharedR.drawable.done),
            contentDescription = emptyString(),
            modifier = Modifier.size(pillIconSize),
            tint = LocalExtendedColorScheme.current.onSuccessContainer,
        )
        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalExtendedColorScheme.current.onSuccessContainer,
        )
    }
}

private val iconSize = 64.dp
private val pillIconSize = 16.dp

@Composable
private fun FailureContent(
    localizedException: LocalizedException,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ExceptionView(localizedException = localizedException)
        }

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LocalBoxSpacing.current.medium),
        ) {
            Text(text = stringResource(id = SharedR.string.retry_action))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuccessSignInWithEmailLinkViewPreview() {
    EmailLinkSignInView(
        state = LoadableViewState.Success(emptyAccountBundle()),
        email = "preview@example.com",
        onRetry = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun FailureSignInWithEmailLinkViewPreview() {
    EmailLinkSignInView(
        state = LoadableViewState.Failure(UnknownLocalizedException()),
        email = "preview@example.com",
        onRetry = {},
    )
}
