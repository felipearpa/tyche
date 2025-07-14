package com.felipearpa.network.retrofit

import com.felipearpa.network.HttpStatus
import com.felipearpa.network.NetworkException
import com.felipearpa.network.NetworkExceptionHandler
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RetrofitExceptionHandler : NetworkExceptionHandler {
    override suspend fun <Value> handle(block: suspend () -> Value): Result<Value> {
        return try {
            Result.success(block())
        } catch (httpException: HttpException) {
            Result.failure(
                NetworkException.Http(httpStatus = HttpStatus(code = httpException.code())),
            )
        } catch (_: UnknownHostException) {
            Result.failure(NetworkException.RemoteCommunication)
        } catch (_: SocketTimeoutException) {
            Result.failure(NetworkException.RemoteCommunication)
        } catch (exception: Exception) {
            println(exception)
            Result.failure(exception)
        }
    }
}
