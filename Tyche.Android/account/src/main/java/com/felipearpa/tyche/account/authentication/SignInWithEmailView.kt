package com.felipearpa.tyche.account.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.account.EmailTextField
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.ui.exception.ExceptionAlertDialog
import com.felipearpa.tyche.ui.exception.localizedExceptionOrNull
import com.felipearpa.tyche.ui.progress.ProgressContainerView
import com.felipearpa.tyche.ui.state.LoadableViewState
import com.felipearpa.tyche.ui.state.exceptionOrNull
import com.felipearpa.tyche.ui.theme.boxSpacing
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun SignInWithEmailView(
    viewModel: SignInWithEmailViewModel,
    onBackRequested: () -> Unit
) {
    val viewState by viewModel.state.collectAsState(initial = LoadableViewState.Initial)
    SignInWithEmailView(
        viewState = viewState,
        signInWithEmail = viewModel::sendSignInLinkToEmail,
        reset = viewModel::reset,
        onBackRequested = onBackRequested
    )
}

@Composable
private fun SignInWithEmailView(
    viewState: LoadableViewState<Unit>,
    signInWithEmail: (String) -> Unit,
    reset: () -> Unit,
    onBackRequested: () -> Unit
) {
    var email by remember { mutableStateOf(emptyString()) }
    var isValid by remember { mutableStateOf(false) }
    val onEdited: (String) -> Unit = { newEmail ->
        email = newEmail
        isValid = Email.isValid(email)
    }
    val login = if (viewState is LoadableViewState.Loading || !isValid) null else {
        { signInWithEmail(email) }
    }

    Scaffold(topBar = { TopBar(back = onBackRequested) }) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .fillMaxWidth()
        ) {
            when (viewState) {
                LoadableViewState.Initial -> SignInWithEmailView(
                    email = email,
                    onEdited = onEdited,
                    login = login,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.boxSpacing.medium)
                )

                LoadableViewState.Loading -> ProgressContainerView {
                    SignInWithEmailView(
                        email = email,
                        modifier = Modifier.padding(horizontal = MaterialTheme.boxSpacing.medium)
                    )
                }

                is LoadableViewState.Success -> {
                    SuccessContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = MaterialTheme.boxSpacing.medium)
                    )
                }

                is LoadableViewState.Failure -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.boxSpacing.medium)
                ) {
                    SignInWithEmailView(
                        email = email,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExceptionAlertDialog(
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
private fun TopBar(back: (() -> Unit)?) {
    TopAppBar(title = { Text(text = stringResource(id = R.string.login_title)) },
        navigationIcon = {
            IconButton(onClick = back ?: {}, enabled = back != null) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.arrow_back),
                    contentDescription = emptyString()
                )
            }
        }
    )
}

@Composable
private fun SignInWithEmailView(
    modifier: Modifier = Modifier,
    email: String,
    onEdited: (String) -> Unit = {},
    login: (() -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.medium)
    ) {
        EmailTextField(
            value = email,
            onValueChanged = { newEmail -> onEdited(newEmail) },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = login ?: {}, enabled = login != null, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.login_action))
        }
    }
}

@Composable
private fun SuccessContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.large)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outgoing_mail),
                    contentDescription = emptyString(),
                    modifier = Modifier.size(width = 64.dp, height = 64.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.boxSpacing.medium)
                ) {
                    Text(
                        text = stringResource(id = R.string.verification_email_sent_title),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = stringResource(id = R.string.verification_email_sent_description),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InitialLoginViewPreview() {
    SignInWithEmailView(
        viewState = LoadableViewState.Initial,
        signInWithEmail = {},
        reset = {},
        onBackRequested = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun SuccessLoginViewPreview() {
    SignInWithEmailView(
        viewState = LoadableViewState.Success(Unit),
        signInWithEmail = {},
        reset = {},
        onBackRequested = {}
    )
}