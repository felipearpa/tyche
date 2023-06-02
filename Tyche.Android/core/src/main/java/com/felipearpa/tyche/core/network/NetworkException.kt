package com.felipearpa.tyche.core.network

sealed class NetworkException : Throwable() {

    data class HttpException(val httpStatusCode: HttpStatusCode) : NetworkException()

    object RemoteCommunicationException : NetworkException()
}

inline fun <R, T : R> Result<T>.recoverNetworkException(transform: (exception: NetworkException) -> R): Result<R> {
    return when (val exception = this.exceptionOrNull()) {
        is NetworkException -> Result.success(transform(exception))
        else -> this
    }
}

inline fun <R, T : R> Result<T>.recoverHttpException(transform: (exception: NetworkException.HttpException) -> R): Result<R> {
    return when (val exception = this.exceptionOrNull()) {
        is NetworkException.HttpException -> Result.success(transform(exception))
        else -> this
    }
}