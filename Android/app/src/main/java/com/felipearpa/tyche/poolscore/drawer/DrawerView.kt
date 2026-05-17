package com.felipearpa.tyche.poolscore.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.foundation.emptyString
import com.felipearpa.tyche.AccountEditDialog
import com.felipearpa.tyche.AccountHeaderDrawer
import com.felipearpa.tyche.R
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun DrawerView(viewModel: DrawerViewModel, onSignOut: () -> Unit) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val isSavingUsername by viewModel.isSavingUsername.collectAsStateWithLifecycle()
    val usernameError by viewModel.usernameError.collectAsStateWithLifecycle()

    DrawerView(
        modifier = Modifier.fillMaxSize(),
        email = email,
        username = username,
        isSavingUsername = isSavingUsername,
        usernameError = usernameError,
        onSaveUsername = viewModel::changeUsername,
        onClearUsernameError = viewModel::clearUsernameError,
        logout = {
            viewModel.logout()
            onSignOut()
        },
    )
}


@Composable
private fun DrawerView(
    modifier: Modifier = Modifier,
    email: String = emptyString(),
    username: String = emptyString(),
    isSavingUsername: Boolean = false,
    usernameError: String? = null,
    onSaveUsername: (String, () -> Unit) -> Unit = { _, _ -> },
    onClearUsernameError: () -> Unit = {},
    logout: () -> Unit = {},
) {
    var isEditingAccount by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(all = LocalBoxSpacing.current.medium),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AccountHeaderDrawer(
            username = username,
            email = email,
            onEditAccount = {
                onClearUsernameError()
                isEditingAccount = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LocalBoxSpacing.current.large),
        )

        SignOutButton(
            onSignOut = logout,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (isEditingAccount) {
        val resolvedError = when (usernameError) {
            DrawerViewModel.USERNAME_UPDATE_FAILED ->
                stringResource(id = R.string.update_account_failure_message)

            else -> null
        }
        AccountEditDialog(
            initialUsername = username,
            isSaving = isSavingUsername,
            serverError = resolvedError,
            onSave = { value: String ->
                onSaveUsername(value) { isEditingAccount = false }
            },
            onClearError = onClearUsernameError,
            onDismiss = {
                onClearUsernameError()
                isEditingAccount = false
            },
        )
    }
}

@Composable
private fun SignOutButton(onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        modifier = modifier,
        onClick = onSignOut,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.sign_out),
            contentDescription = null,
        )
        Text(
            text = stringResource(id = R.string.sign_out_action),
            modifier = Modifier.padding(start = LocalBoxSpacing.current.small),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InitialDrawerViewPreview() {
    Surface {
        DrawerView(
            modifier = Modifier.fillMaxSize(),
            email = "felipearpa@email.com",
            username = "felipearpa",
        )
    }
}
