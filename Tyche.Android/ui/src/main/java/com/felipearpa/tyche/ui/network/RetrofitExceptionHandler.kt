package com.felipearpa.tyche.ui.network

import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.core.network.toHtpStatusCode
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RetrofitExceptionHandler : com.felipearpa.tyche.core.network.NetworkExceptionHandler {

    override suspend fun <T> handle(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (httpException: HttpException) {
            Result.failure(
                NetworkException.HttpException(
                    httpStatusCode = httpException.code().toHtpStatusCode()
                )
            )
        } catch (ignored: UnknownHostException) {
            Result.failure(NetworkException.RemoteCommunicationException)
        } catch (ignored: SocketTimeoutException) {
            Result.failure(NetworkException.RemoteCommunicationException)
        }
    }
}