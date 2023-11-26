package com.felipearpa.tyche.core.network

interface NetworkExceptionHandler {
    suspend fun <Value> handle(block: suspend () -> Value): Result<Value>
}