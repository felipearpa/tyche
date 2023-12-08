package com.felipearpa.tyche.ui.network

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.ui.R
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class NetworkLocalizedException : LocalizedException() {
    data object RemoteCommunication : NetworkLocalizedException()

    override val errorDescription: String?
        @Composable get() = when (this) {
            RemoteCommunication -> stringResource(id = R.string.remote_communication_failure_description)
        }

    override val failureReason: String?
        @Composable get() = when (this) {
            RemoteCommunication -> stringResource(id = R.string.remote_communication_failure_reason)
        }

    override val recoverySuggestion: String?
        @Composable get() = when (this) {
            RemoteCommunication -> stringResource(id = R.string.remote_communication_failure_recovery_suggestion)
        }
}

fun NetworkException.toNetworkLocalizedException() =
    NetworkLocalizedException.RemoteCommunication
