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
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.account.EmailTextField
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.ui.exception.ExceptionAlertDialog
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.exception.localizedOrDefault
import com.felipearpa.tyche.ui.loading.LoadingContainerView
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.ui.state.LoadableViewState
import com.felipearpa.ui.state.exceptionOrNull
import com.felipearpa.ui.state.isLoading
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun EmailSignInView(
    viewModel: EmailSignInViewModel,
    onBack: () -> Unit,
) {
    val viewState by viewModel.state.collectAsState(initial = LoadableViewState.Initial)

    EmailSignInView(
        viewState = viewState,
        onSignInWithEmail = viewModel::sendSignInLinkToEmail,
        onReset = viewModel::reset,
        onBack = onBack,
    )
}

@Composable
private fun EmailSignInView(
    viewState: LoadableViewState<Unit>,
    onSignInWithEmail: (String) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    var email by remember { mutableStateOf(emptyString()) }
    var isValid by remember { mutableStateOf(false) }
    val edit: (String) -> Unit = { newEmail ->
        email = newEmail
        isValid = Email.isValid(email)
    }
    val signIn = if (viewState.isLoading() || !isValid) null else {
        { onSignInWithEmail(email) }
    }
    val back = if (viewState.isLoading()) null else onBack

    Scaffold(
        topBar = { TopBar(onBack = back) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .fillMaxWidth(),
        ) {
            when (viewState) {
                LoadableViewState.Initial -> EmailSignInView(
                    email = email,
                    onEdit = edit,
                    onSignIn = signIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LocalBoxSpacing.current.medium),
                )

                LoadableViewState.Loading -> LoadingContainerView {
                    EmailSignInView(
                        email = email,
                        modifier = Modifier.padding(horizontal = LocalBoxSpacing.current.medium),
                    )
                }

                is LoadableViewState.Success -> SuccessContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = LocalBoxSpacing.current.medium),
                )

                is LoadableViewState.Failure -> FailureContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LocalBoxSpacing.current.medium),
                    email = email,
                    viewState = viewState,
                    onReset = onReset,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onBack: (() -> Unit)?) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.sign_in_title)) },
        navigationIcon = {
            IconButton(onClick = onBack ?: {}, enabled = onBack != null) {
                Icon(
                    painter = painterResource(id = SharedR.drawable.arrow_back),
                    contentDescription = emptyString(),
                )
            }
        },
    )
}

@Composable
private fun EmailSignInView(
    modifier: Modifier = Modifier,
    email: String,
    onEdit: (String) -> Unit = {},
    onSignIn: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    ) {
        EmailTextField(
            value = email,
            onValueChange = { newEmail -> onEdit(newEmail) },
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
private fun SuccessContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outgoing_mail),
                    contentDescription = emptyString(),
                    modifier = Modifier.size(width = iconSize, height = iconSize),
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                ) {
                    Text(
                        text = stringResource(id = R.string.verification_email_sent_title),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Text(text = stringResource(id = R.string.verification_email_sent_description))
                }
            }
        }
    }
}

@Composable
private fun FailureContent(
    modifier: Modifier = Modifier,
    email: String,
    viewState: LoadableViewState<Unit>,
    onReset: () -> Unit,
) {
    Column(modifier = modifier) {
        EmailSignInView(
            email = email,
            modifier = Modifier.fillMaxWidth(),
        )
        ExceptionAlertDialog(
            exception = viewState.exceptionOrNull()!!.localizedOrDefault(),
            onDismiss = { onReset() },
        )
    }
}

private val iconSize = 64.dp

@Preview(showBackground = true, name = "Initial")
@Composable
private fun InitialEmailSignInViewPreview() {
    EmailSignInView(
        viewState = LoadableViewState.Initial,
        onSignInWithEmail = {},
        onReset = {},
        onBack = {},
    )
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun LoadingEmailSignInViewPreview() {
    EmailSignInView(
        viewState = LoadableViewState.Loading,
        onSignInWithEmail = {},
        onReset = {},
        onBack = {},
    )
}

@Preview(showBackground = true, name = "Success")
@Composable
private fun SuccessEmailSignInViewPreview() {
    EmailSignInView(
        viewState = LoadableViewState.Success(Unit),
        onSignInWithEmail = {},
        onReset = {},
        onBack = {},
    )
}

@Preview(showBackground = true, name = "Failure")
@Composable
private fun FailureEmailSignInViewPreview() {
    EmailSignInView(
        viewState = LoadableViewState.Failure(UnknownLocalizedException()),
        onSignInWithEmail = {},
        onReset = {},
        onBack = {},
    )
}
