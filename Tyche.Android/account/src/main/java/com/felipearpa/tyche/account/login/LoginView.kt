package com.felipearpa.tyche.account.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.data.account.AccountBundle
import com.felipearpa.tyche.account.PasswordTextField
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.account.UsernameTextField
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.ui.exception.FailureAlertDialog
import com.felipearpa.tyche.ui.state.LoadableViewState
import com.felipearpa.tyche.ui.progress.ProgressContainerView
import com.felipearpa.tyche.ui.state.exceptionOrNull
import com.felipearpa.tyche.ui.state.isSuccess
import com.felipearpa.tyche.ui.exception.localizedExceptionOrNull
import com.felipearpa.tyche.ui.state.valueOrNull

@Composable
fun LoginView(
    viewModel: LoginViewModel,
    onLoggedIn: (AccountBundle) -> Unit,
    onBackRequested: () -> Unit
) {
    val viewState by viewModel.state.collectAsState(initial = LoadableViewState.Initial)
    LoginView(
        viewState = viewState,
        login = viewModel::login,
        onLoggedIn = onLoggedIn,
        reset = viewModel::reset,
        onBackRequested = onBackRequested
    )
}

@Composable
private fun LoginView(
    viewState: LoadableViewState<AccountBundle>,
    login: (LoginCredentialModel) -> Unit,
    onLoggedIn: (AccountBundle) -> Unit,
    reset: () -> Unit,
    onBackRequested: () -> Unit
) {
    var loginCredential by remember {
        mutableStateOf(
            LoginCredentialModel(
                username = emptyString(),
                password = emptyString()
            )
        )
    }

    var hasErrors by remember { mutableStateOf(true) }

    val onEdited: (LoginCredentialModel) -> Unit = { newLoginCredential ->
        loginCredential = newLoginCredential
        hasErrors = loginCredential.hasErrors()
    }

    LaunchedEffect(viewState) {
        if (viewState.isSuccess())
            onLoggedIn(viewState.valueOrNull()!!)
    }

    Scaffold(topBar = {
        AppTopBar(
            back = onBackRequested,
            login = if (viewState is LoadableViewState.Loading || hasErrors) null else {
                { login(loginCredential) }
            }
        )
    }) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .padding(all = 8.dp)
        ) {
            when (viewState) {
                LoadableViewState.Initial -> LoginView(
                    loginCredential = loginCredential,
                    onEdited = onEdited
                )

                LoadableViewState.Loading, is LoadableViewState.Success -> ProgressContainerView {
                    LoginView(loginCredential = loginCredential)
                }

                is LoadableViewState.Failure -> Column(modifier = Modifier.fillMaxWidth()) {
                    LoginView(loginCredential = loginCredential)
                    FailureAlertDialog(
                        exception = viewState.exceptionOrNull()!!.localizedExceptionOrNull()!!,
                        onDismiss = { reset() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(back: (() -> Unit)?, login: (() -> Unit)?) {
    TopAppBar(title = { Text(text = stringResource(id = R.string.login_title)) },
        navigationIcon = {
            TextButton(onClick = back ?: {}, enabled = back != null) {
                Text(text = stringResource(id = R.string.back_action))
            }
        },
        actions = {
            TextButton(onClick = login ?: {}, enabled = login != null) {
                Text(text = stringResource(id = R.string.login_action))
            }
        })
}

@Composable
private fun LoginView(
    modifier: Modifier = Modifier,
    loginCredential: LoginCredentialModel,
    onEdited: (LoginCredentialModel) -> Unit = {}
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UsernameTextField(
            value = loginCredential.username,
            onValueChanged = { newUsername -> onEdited(loginCredential.copy(username = newUsername)) },
            modifier = Modifier.fillMaxWidth()
        )

        PasswordTextField(
            value = loginCredential.password,
            onValueChanged = { newPassword -> onEdited(loginCredential.copy(password = newPassword)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun LoginViewPreview() {
    Surface {
        LoginView(
            viewState = LoadableViewState.Initial,
            login = {},
            onLoggedIn = {},
            reset = {},
            onBackRequested = {})
    }
}