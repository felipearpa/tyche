package com.felipearpa.tyche.pool.joiner

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.MessageWithActions
import com.felipearpa.tyche.ui.exception.ExceptionView
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun PoolJoinRequiresSignInView(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    MessageWithActions(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = LocalBoxSpacing.current.medium),
        actions = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = LocalBoxSpacing.current.medium),
            ) {
                Text(text = stringResource(id = R.string.got_it_action))
            }
        },
    ) {
        ExceptionView(localizedException = PoolJoinRequiresSignInException)
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolJoinRequiresSignInViewPreview() {
    PoolJoinRequiresSignInView(onDismiss = {})
}
