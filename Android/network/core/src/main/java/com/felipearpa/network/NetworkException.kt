package com.felipearpa.network

sealed class NetworkException : Throwable() {
    data class Http(val httpStatus: HttpStatus) : NetworkException()
    data object RemoteCommunication : NetworkException() {
        @Suppress("unused")
        private fun readResolve(): Any = RemoteCommunication
    }
}
