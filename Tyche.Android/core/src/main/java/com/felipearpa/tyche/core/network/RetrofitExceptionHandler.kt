package com.felipearpa.tyche.core.network

import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class RetrofitExceptionHandler @Inject constructor() : NetworkExceptionHandler {
    override suspend fun <Value> handle(block: suspend () -> Value): Result<Value> {
        return try {
            Result.success(block())
        } catch (httpException: HttpException) {
            Result.failure(
                NetworkException.Http(
                    httpStatusCode = httpException.code().toHtpStatusCode(),
                ),
            )
        } catch (_: UnknownHostException) {
            Result.failure(NetworkException.RemoteCommunication)
        } catch (_: SocketTimeoutException) {
            Result.failure(NetworkException.RemoteCommunication)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
