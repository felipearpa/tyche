package com.felipearpa.tyche.ui.network

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.network.NetworkException
import com.felipearpa.tyche.ui.R
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class NetworkLocalizedException : LocalizedException() {
    data object RemoteCommunication : NetworkLocalizedException() {
        private fun readResolve(): Any = RemoteCommunication

        override val errorDescription: String
            @Composable get() = stringResource(id = R.string.remote_communication_failure_description)

        override val failureReason: String
            @Composable get() = stringResource(id = R.string.remote_communication_failure_reason)

        override val recoverySuggestion: String
            @Composable get() = stringResource(id = R.string.remote_communication_failure_recovery_suggestion)
    }
}

fun NetworkException.toNetworkLocalizedException() =
    NetworkLocalizedException.RemoteCommunication
