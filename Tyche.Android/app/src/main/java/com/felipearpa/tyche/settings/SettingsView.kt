package com.felipearpa.tyche.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.R
import com.felipearpa.tyche.core.emptyString

@Composable
fun SettingsView(viewModel: SettingsViewModel, onLogout: () -> Unit) {
    SettingsView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        logout = {
            viewModel.logout()
            onLogout()
        }
    )
}

@Composable
fun SettingsView(modifier: Modifier = Modifier, logout: () -> Unit = {}) {
    Column(modifier = modifier) {
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = logout) {
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
fun SettingsViewPreview() {
    SettingsView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    )
}