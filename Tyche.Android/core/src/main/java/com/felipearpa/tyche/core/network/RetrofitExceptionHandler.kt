package com.felipearpa.tyche.core.network

import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class RetrofitExceptionHandler @Inject constructor() : NetworkExceptionHandler {
    override suspend fun <T> handle(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (httpException: HttpException) {
            Result.failure(
                NetworkException.Http(
                    httpStatusCode = httpException.code().toHtpStatusCode()
                )
            )
        } catch (ignored: UnknownHostException) {
            Result.failure(NetworkException.RemoteCommunication)
        } catch (ignored: SocketTimeoutException) {
            Result.failure(NetworkException.RemoteCommunication)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}