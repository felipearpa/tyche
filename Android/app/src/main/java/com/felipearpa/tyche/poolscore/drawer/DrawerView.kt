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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipearpa.tyche.AccountHeaderDrawer
import com.felipearpa.tyche.R
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun DrawerView(
    viewModel: DrawerViewModel,
    onSignOut: () -> Unit,
    onProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DrawerView(
        uiState = uiState,
        modifier = Modifier.fillMaxSize(),
        onProfile = onProfile,
        logout = {
            viewModel.logout()
            onSignOut()
        },
    )
}


@Composable
private fun DrawerView(
    uiState: PoolScoreListDrawerUiState,
    modifier: Modifier = Modifier,
    onProfile: () -> Unit = {},
    logout: () -> Unit = {},
) {
    Column(
        modifier = modifier.padding(all = LocalBoxSpacing.current.medium),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AccountHeaderDrawer(
            accountId = uiState.accountId,
            username = uiState.username,
            email = uiState.email,
            onProfile = onProfile,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LocalBoxSpacing.current.large),
        )

        SignOutButton(
            onSignOut = logout,
            modifier = Modifier.fillMaxWidth(),
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
            uiState = PoolScoreListDrawerUiState(
                accountId = "account-1",
                email = "felipearpa@email.com",
                username = "felipearpa",
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
