package com.felipearpa.network

inline fun <Value> Result<Value>.recoverNetworkException(transform: (exception: NetworkException) -> Throwable): Result<Value> {
    return when (val exception = this.exceptionOrNull()) {
        is NetworkException -> Result.failure(transform(exception))
        else -> this
    }
}

inline fun <Value> Result<Value>.recoverHttpException(transform: (exception: NetworkException.Http) -> Throwable): Result<Value> {
    return when (val exception = this.exceptionOrNull()) {
        is NetworkException.Http -> Result.failure(transform(exception))
        else -> this
    }
}
