package com.felipearpa.tyche.core.network

sealed class NetworkException : Throwable() {
    data class Http(val httpStatusCode: HttpStatusCode) : NetworkException()
    data object RemoteCommunication : NetworkException()
}

inline fun <Target, Value : Target> Result<Value>.recoverNetworkException(transform: (exception: NetworkException) -> Target): Result<Target> {
    return when (val exception = this.exceptionOrNull()) {
        is NetworkException -> Result.success(transform(exception))
        else -> this
    }
}

inline fun <Target, Value : Target> Result<Value>.recoverHttpException(transform: (exception: NetworkException.Http) -> Target): Result<Target> {
    return when (val exception = this.exceptionOrNull()) {
        is NetworkException.Http -> Result.success(transform(exception))
        else -> this
    }
}