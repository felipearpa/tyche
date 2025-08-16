package com.felipearpa.network

interface NetworkExceptionHandler {
    suspend fun <Value> handle(block: suspend () -> Value): Result<Value>
}
