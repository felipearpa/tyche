package com.felipearpa.tyche.ui.network

import com.felipearpa.tyche.core.network.NetworkException

sealed class NetworkAppException : Throwable() {

    object RemoteCommunicationFailure : NetworkAppException()
}

fun NetworkException.toNetworkAppException(): Throwable {
    return when (this) {
        NetworkException.RemoteCommunication -> NetworkAppException.RemoteCommunicationFailure
        else -> this
    }
}