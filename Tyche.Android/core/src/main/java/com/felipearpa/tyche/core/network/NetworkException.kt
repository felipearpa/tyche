package com.felipearpa.tyche.core.network

sealed class NetworkException : Throwable() {
    data class Http(val httpStatusCode: HttpStatusCode) : NetworkException()
    data object RemoteCommunication : NetworkException()
}

fun <Value> Result<Value>.recoverNetworkException(transform: (exception: NetworkException) -> Throwable): Result<Value> {
    return when (val exception = this.exceptionOrNull()) {
        is NetworkException -> Result.failure(transform(exception))
        else -> this
    }
}

fun <Value> Result<Value>.recoverHttpException(transform: (exception: NetworkException.Http) -> Throwable): Result<Value> {
    return when (val exception = this.exceptionOrNull()) {
        is NetworkException.Http -> Result.failure(transform(exception))
        else -> this
    }
}