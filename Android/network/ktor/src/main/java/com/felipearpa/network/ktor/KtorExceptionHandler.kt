package com.felipearpa.network.ktor

import com.felipearpa.network.HttpStatus
import com.felipearpa.network.NetworkException
import com.felipearpa.network.NetworkExceptionHandler
import io.ktor.client.plugins.ResponseException
import java.net.ConnectException
import java.net.SocketTimeoutException

class KtorExceptionHandler : NetworkExceptionHandler {
    override suspend fun <Value> handle(block: suspend () -> Value): Result<Value> {
        return try {
            Result.success(block())
        } catch (responseException: ResponseException) {
            Result.failure(
                NetworkException.Http(httpStatus = HttpStatus(code = responseException.response.status.value)),
            )
        } catch (_: ConnectException) {
            Result.failure(NetworkException.RemoteCommunication)
        } catch (_: SocketTimeoutException) {
            Result.failure(NetworkException.RemoteCommunication)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
