package com.felipearpa.tyche.account.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.account.EmailTextField
import com.felipearpa.tyche.account.PasswordTextField
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.ui.exception.ExceptionAlertDialog
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.loading.LoadingContainerView
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.ui.state.LoadableViewState
import com.felipearpa.ui.state.isLoading
import com.felipearpa.ui.state.onFailure
import com.felipearpa.ui.state.onSuccess
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun EmailAndPasswordSignInView(
    viewModel: EmailAndPasswordSignInViewModel,
    onBack: () -> Unit,
    onAuthenticate: (AccountBundle) -> Unit,
) {
    val viewState by viewModel.state.collectAsState(initial = LoadableViewState.Initial)
    EmailAndPasswordSignInView(
        viewState = viewState,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
        onBack = onBack,
        onReset = viewModel::reset,
        onSignIn = viewModel::signInWithEmailAndPassword,
        onAuthenticate = onAuthenticate,
    )
}

@Composable
private fun EmailAndPasswordSignInView(
    viewState: LoadableViewState<AccountBundle>,
    modifier: Modifier = Modifier,
    onSignIn: (String, String) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onAuthenticate: (AccountBundle) -> Unit,
) {
    var email by remember { mutableStateOf(emptyString()) }
    var password by remember { mutableStateOf(emptyString()) }
    var isValid by remember { mutableStateOf(false) }

    val updateEmail: (String) -> Unit = { newEmail ->
        email = newEmail
        isValid = Email.isValid(email)
    }

    val updatePassword: (String) -> Unit = { newPassword ->
        password = newPassword
    }

    val signIn = if (viewState.isLoading() || !isValid) null else {
        { onSignIn(email, password) }
    }

    LaunchedEffect(viewState) {
        viewState.onSuccess { accountBundle ->
            onAuthenticate(accountBundle)
        }
    }

    Scaffold(topBar = { TopBar(onBack = onBack) }) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .fillMaxWidth(),
        ) {
            when (viewState) {
                LoadableViewState.Initial ->
                    EmailAndPasswordSignInView(
                        modifier = modifier,
                        email = email,
                        onEmailChanged = updateEmail,
                        password = password,
                        onPasswordChanged = updatePassword,
                        onSignIn = { signIn?.invoke() },
                    )

                LoadableViewState.Loading, is LoadableViewState.Success -> LoadingContainerView {
                    EmailAndPasswordSignInView(
                        modifier = modifier,
                        email = email,
                        password = password,
                    )
                }

                is LoadableViewState.Failure -> FailureContent(
                    email = email,
                    password = password,
                    viewState = viewState,
                    reset = onReset,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun EmailAndPasswordSignInView(
    modifier: Modifier = Modifier,
    email: String,
    onEmailChanged: (String) -> Unit = {},
    password: String,
    onPasswordChanged: (String) -> Unit = {},
    onSignIn: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    ) {
        EmailTextField(
            value = email,
            onValueChange = { newEmail -> onEmailChanged(newEmail) },
            modifier = Modifier.fillMaxWidth(),
        )

        PasswordTextField(
            value = password,
            onValueChange = onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onSignIn ?: {},
            enabled = onSignIn != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.sign_in_action))
        }
    }
}

@Composable
private fun FailureContent(
    modifier: Modifier = Modifier,
    email: String,
    password: String,
    viewState: LoadableViewState<Unit>,
    reset: () -> Unit,
) {
    viewState.onFailure { exception ->
        Column(modifier = modifier) {
            EmailAndPasswordSignInView(
                email = email,
                password = password,
                modifier = Modifier.fillMaxWidth(),
            )
            ExceptionAlertDialog(
                exception = exception.localizedOrDefault(),
                onDismiss = { reset() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onBack: (() -> Unit)?) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.sign_in_title)) },
        navigationIcon = {
            IconButton(onClick = { onBack?.invoke() }, enabled = onBack != null) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.arrow_back),
                    contentDescription = emptyString(),
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun InitialEmailAndPasswordSignInViewPreview() {
    EmailAndPasswordSignInView(
        viewState = LoadableViewState.Initial,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
        onSignIn = { _, _ -> },
        onBack = {},
        onReset = {},
        onAuthenticate = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun LoadingEmailAndPasswordSignInViewPreview() {
    EmailAndPasswordSignInView(
        viewState = LoadableViewState.Loading,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
        onSignIn = { _, _ -> },
        onBack = {},
        onReset = {},
        onAuthenticate = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun SuccessEmailAndPasswordSignInViewPreview() {
    EmailAndPasswordSignInView(
        viewState = LoadableViewState.Success(
            AccountBundle(
                accountId = emptyString(),
                externalAccountId = emptyString(),
            ),
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
        onSignIn = { _, _ -> },
        onBack = {},
        onReset = {},
        onAuthenticate = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun FailureEmailAndPasswordSignInViewPreview() {
    EmailAndPasswordSignInView(
        viewState = LoadableViewState.Failure(UnknownLocalizedException()),
        modifier = Modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
        onSignIn = { _, _ -> },
        onBack = {},
        onReset = {},
        onAuthenticate = {},
    )
}
