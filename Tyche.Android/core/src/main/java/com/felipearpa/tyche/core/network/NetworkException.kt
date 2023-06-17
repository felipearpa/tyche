package com.felipearpa.tyche.core.network

sealed class NetworkException : Throwable() {

    data class Http(val httpStatusCode: HttpStatusCode) : NetworkException()

    object RemoteCommunication : NetworkException()
}

inline fun <R, T : R> Result<T>.recoverNetworkException(transform: (exception: NetworkException) -> R): Result<R> {
    return when (val exception = this.exceptionOrNull()) {
        is NetworkException -> Result.success(transform(exception))
        else -> this
    }
}

inline fun <R, T : R> Result<T>.recoverHttpException(transform: (exception: NetworkException.Http) -> R): Result<R> {
    return when (val exception = this.exceptionOrNull()) {
        is NetworkException.Http -> Result.success(transform(exception))
        else -> this
    }
}