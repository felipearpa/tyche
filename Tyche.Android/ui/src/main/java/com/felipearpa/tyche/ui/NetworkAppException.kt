package com.felipearpa.tyche.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.ui.network.NetworkAppException

@Composable
fun Throwable.toUIMessage(): String {
    return when (this) {
        is NetworkAppException.RemoteCommunicationFailure -> stringResource(id = R.string.remote_communication_failure_message)
        else -> stringResource(id = R.string.unknown_failure_message)
    }
}