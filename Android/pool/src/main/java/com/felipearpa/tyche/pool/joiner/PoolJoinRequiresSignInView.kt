package com.felipearpa.tyche.pool.joiner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun PoolJoinRequiresSignInView(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(all = LocalBoxSpacing.current.medium),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
        ) {
            ExceptionView(localizedException = PoolJoinRequiresSignInException)

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.got_it_action))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolJoinRequiresSignInViewPreview() {
    PoolJoinRequiresSignInView(onDismiss = {})
}
