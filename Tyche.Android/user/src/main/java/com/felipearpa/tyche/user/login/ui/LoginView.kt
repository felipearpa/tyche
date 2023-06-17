package com.felipearpa.tyche.user.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.ui.Message
import com.felipearpa.tyche.ui.ViewState
import com.felipearpa.tyche.ui.isFailure
import com.felipearpa.tyche.ui.isLoading
import com.felipearpa.tyche.ui.isSuccess
import com.felipearpa.tyche.ui.toUIMessage
import com.felipearpa.tyche.user.R
import com.felipearpa.tyche.user.UserProfile
import com.felipearpa.tyche.user.ui.PasswordTextField
import com.felipearpa.tyche.user.ui.UsernameTextField
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun LoginView(
    viewModel: LoginViewModel,
    onLoggedIn: (UserProfile) -> Unit,
    back: () -> Unit
) {
    val viewState by viewModel.state.collectAsState(initial = ViewState.Initial)

    var loginCredential by remember {
        mutableStateOf(
            LoginCredentialModel(
                username = emptyString(),
                password = emptyString()
            )
        )
    }

    var hasErrors by remember { mutableStateOf(true) }

    val onUserEdited: (LoginCredentialModel) -> Unit = { newUser ->
        loginCredential = newUser
        hasErrors = loginCredential.hasErrors()
    }

    val onDoneClick: () -> Unit = {
        viewModel.login(loginCredentialCredential = loginCredential)
    }

    Scaffold(topBar = {
        AppTopBar(
            onBackClick = if (viewState !is ViewState.Success) back else null,
            onDoneClick = if (viewState !is ViewState.Success && !hasErrors) onDoneClick else null
        )
    }) { paddingValues ->

        Column(modifier = Modifier.padding(paddingValues = paddingValues)) {
            if (!viewState.isSuccess()) {
                LoginContent(
                    state = viewState,
                    loginCredential = loginCredential,
                    onUserEdited = onUserEdited,
                    onDismissFailure = viewModel::resetState
                )
            } else {
                SuccessContent(
                    onLoggedIn = onLoggedIn,
                    loggedInUserProfile = (viewState as ViewState.Success).invoke()
                )
            }
        }
    }
}

@Composable
private fun LoginContent(
    state: ViewState<UserProfile>,
    loginCredential: LoginCredentialModel,
    onUserEdited: (LoginCredentialModel) -> Unit,
    onDismissFailure: () -> Unit
) {
    if (state.isLoading()) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }

    LoginView(
        loginCredential = loginCredential,
        onUserEdited = onUserEdited,
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp)
            .fillMaxWidth()
    )

    if (state.isFailure()) {
        Failure(
            failure = (state as ViewState.Failure).invoke(),
            onConfirmClick = onDismissFailure
        )
    }
}

@Composable
private fun SuccessContent(
    onLoggedIn: (UserProfile) -> Unit,
    loggedInUserProfile: UserProfile
) {
    Message(
        iconResourceId = R.drawable.ic_sentiment_very_satisfied,
        messageResourceId = R.string.user_success_login_message
    )

    LaunchedEffect(Unit) {
        delay(5.seconds)
        onLoggedIn(loggedInUserProfile)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(onBackClick: (() -> Unit)?, onDoneClick: (() -> Unit)?) {
    TopAppBar(title = { Text(text = stringResource(id = R.string.login_title)) },
        navigationIcon = {
            TextButton(onClick = onBackClick ?: {}, enabled = onBackClick != null) {
                Text(text = stringResource(id = R.string.back_action))
            }
        },
        actions = {
            TextButton(onClick = onDoneClick ?: {}, enabled = onDoneClick != null) {
                Text(text = stringResource(id = R.string.login_action))
            }
        })
}

@Composable
private fun LoginView(
    loginCredential: LoginCredentialModel,
    onUserEdited: (loginCredential: LoginCredentialModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UsernameTextField(
            value = loginCredential.username,
            onValueChanged = { newValue -> onUserEdited(loginCredential.copy(username = newValue)) },
            modifier = Modifier.fillMaxWidth()
        )

        PasswordTextField(
            value = loginCredential.password,
            onValueChanged = { newValue -> onUserEdited(loginCredential.copy(password = newValue)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Failure(failure: Throwable, onConfirmClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = onConfirmClick,
        confirmButton = {
            TextButton(onClick = onConfirmClick)
            { Text(text = stringResource(id = R.string.done_action)) }
        },
        text = {
            Text(
                text = when (failure) {
                    is LoginAppException.InvalidCredential -> stringResource(id = R.string.invalid_credential_failure_message)
                    else -> failure.toUIMessage()
                }
            )
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_sentiment_sad),
                contentDescription = emptyString(),
                modifier = Modifier.size(40.dp, 40.dp)
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun FailurePreview() {
    Failure(failure = LoginAppException.InvalidCredential, onConfirmClick = {})
}

@Preview(showBackground = true)
@Composable
private fun AppTopBarPreview() {
    AppTopBar(
        onBackClick = {},
        onDoneClick = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginViewPreview() {
    LoginView(
        loginCredential = LoginCredentialModel(username = emptyString(), password = emptyString()),
        onUserEdited = {},
        modifier = Modifier.fillMaxWidth()
    )
}