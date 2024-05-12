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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.R
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun DrawerView(viewModel: DrawerViewModel, onLogout: () -> Unit) {
    DrawerView(
        modifier = Modifier.fillMaxSize(),
        logout = {
            viewModel.logout()
            onLogout()
        }
    )
}

@Composable
private fun DrawerView(modifier: Modifier = Modifier, logout: () -> Unit = {}) {
    Column(
        modifier = modifier.padding(all = LocalBoxSpacing.current.large),
        verticalArrangement = Arrangement.Bottom
    ) {
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalBoxSpacing.current.medium),
            onClick = logout
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logout),
                contentDescription = emptyString()
            )
            Text(text = stringResource(id = R.string.log_out_action))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InitialDrawerViewPreview() {
    Surface {
        DrawerView(modifier = Modifier.fillMaxSize())
    }
}
