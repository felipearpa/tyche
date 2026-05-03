package com.felipearpa.tyche.pool.joiner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = LocalBoxSpacing.current.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ExceptionView(localizedException = PoolJoinRequiresSignInException)
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LocalBoxSpacing.current.medium),
        ) {
            Text(text = stringResource(id = R.string.got_it_action))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolJoinRequiresSignInViewPreview() {
    PoolJoinRequiresSignInView(onDismiss = {})
}
