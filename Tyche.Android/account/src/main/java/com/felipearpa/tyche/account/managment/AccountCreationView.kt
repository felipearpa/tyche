package com.felipearpa.tyche.account.managment

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
fun AccountCreationView(
    viewModel: AccountCreationViewModel,
    onAccountCreated: (AccountBundle) -> Unit,
    onBackRequested: () -> Unit
) {
    val viewState by viewModel.state.collectAsState(initial = LoadableViewState.Initial)
    AccountCreationView(
        viewState = viewState,
        create = viewModel::create,
        onAccountCreated = onAccountCreated,
        reset = viewModel::reset,
        onBackRequested = onBackRequested
    )
}

@Composable
private fun AccountCreationView(
    viewState: LoadableViewState<AccountBundle>,
    create: (AccountModel) -> Unit,
    onAccountCreated: (AccountBundle) -> Unit,
    reset: () -> Unit,
    onBackRequested: () -> Unit
) {
    var account by remember {
        mutableStateOf(
            AccountModel(
                username = emptyString(),
                password = emptyString()
            )
        )
    }

    var hasErrors by remember { mutableStateOf(true) }

    val onEdited: (AccountModel) -> Unit = { newAccount ->
        account = newAccount
        hasErrors = account.hasErrors()
    }

    LaunchedEffect(viewState) {
        if (viewState.isSuccess())
            onAccountCreated(viewState.valueOrNull()!!)
    }

    Scaffold(topBar = {
        AppTopBar(
            back = onBackRequested,
            create = if (viewState is LoadableViewState.Loading || hasErrors) null else {
                { create(account) }
            }
        )
    }) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .padding(all = 8.dp)
        ) {
            when (viewState) {
                LoadableViewState.Initial -> AccountCreationView(
                    account = account,
                    onEdited = onEdited
                )

                LoadableViewState.Loading, is LoadableViewState.Success -> ProgressContainerView {
                    AccountCreationView(account = account)
                }

                is LoadableViewState.Failure -> Column(modifier = Modifier.fillMaxWidth()) {
                    AccountCreationView(account = account)
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
private fun AppTopBar(back: (() -> Unit)?, create: (() -> Unit)?) {
    TopAppBar(title = { Text(text = stringResource(id = R.string.user_creation_title)) },
        navigationIcon = {
            TextButton(onClick = back ?: {}, enabled = back != null) {
                Text(text = stringResource(id = R.string.back_action))
            }
        },
        actions = {
            TextButton(onClick = create ?: {}, enabled = create != null) {
                Text(text = stringResource(id = R.string.create_action))
            }
        })
}

@Composable
private fun AccountCreationView(
    modifier: Modifier = Modifier,
    account: AccountModel,
    onEdited: (AccountModel) -> Unit = {}
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UsernameTextField(
            value = account.username,
            onValueChanged = { newUsername -> onEdited(account.copy(username = newUsername)) },
            modifier = Modifier.fillMaxWidth()
        )

        PasswordTextField(
            value = account.password,
            onValueChanged = { newPassword -> onEdited(account.copy(password = newPassword)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun AccountCreationViewPreview() {
    Surface {
        AccountCreationView(
            viewState = LoadableViewState.Initial,
            create = {},
            onAccountCreated = {},
            reset = {},
            onBackRequested = {})
    }
}