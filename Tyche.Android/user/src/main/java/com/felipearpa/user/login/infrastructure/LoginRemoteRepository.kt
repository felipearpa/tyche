package com.felipearpa.user.login.infrastructure

import com.felipearpa.core.network.HttpStatusCode
import com.felipearpa.core.network.NetworkExceptionHandler
import com.felipearpa.core.network.recoverHttpException
import com.felipearpa.user.LoginProfile
import com.felipearpa.user.User
import com.felipearpa.user.account.domain.CreateUserException
import com.felipearpa.user.login.domain.LoginRemoteDataSource
import com.felipearpa.user.login.domain.LoginRepository
import com.felipearpa.user.login.domain.toLoginRequest
import com.felipearpa.user.toProfile
import javax.inject.Inject

class LoginRemoteRepository @Inject constructor(
    private val loginRemoteDataSource: LoginRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) :
    LoginRepository {

    override suspend fun login(user: User): Result<LoginProfile> {
        return networkExceptionHandler.handle {
            loginRemoteDataSource.login(loginRequest = user.toLoginRequest()).toProfile()
        }.recoverHttpException { exception ->
            return when (exception.httpStatusCode) {
                HttpStatusCode.CONFLICT -> Result.failure(CreateUserException.UserAlreadyRegistered)
                else -> Result.failure(exception)
            }
        }
    }
}