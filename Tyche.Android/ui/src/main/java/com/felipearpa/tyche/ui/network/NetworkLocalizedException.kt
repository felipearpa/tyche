package com.felipearpa.tyche.ui.network

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.ui.exception.LocalizedException
import com.felipearpa.tyche.ui.R

sealed class NetworkLocalizedException : LocalizedException() {

    object RemoteCommunication : NetworkLocalizedException()

    override val errorDescription: String?
        @Composable get() = when (this) {
            RemoteCommunication -> stringResource(id = R.string.remote_communication_failure_message)
        }

    override val failureReason: String?
        @Composable get() = when (this) {
            RemoteCommunication -> stringResource(id = R.string.remote_communication_failure_title)
        }
}

fun NetworkException.toNetworkLocalizedException() =
    NetworkLocalizedException.RemoteCommunication
