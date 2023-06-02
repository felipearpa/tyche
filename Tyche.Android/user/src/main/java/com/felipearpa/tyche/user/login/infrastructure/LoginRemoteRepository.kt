package com.felipearpa.tyche.user.login.infrastructure

import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.recoverHttpException
import com.felipearpa.tyche.user.LoginProfile
import com.felipearpa.tyche.user.User
import com.felipearpa.tyche.user.account.domain.CreateUserException
import com.felipearpa.tyche.user.login.domain.LoginRemoteDataSource
import com.felipearpa.tyche.user.login.domain.LoginRepository
import com.felipearpa.tyche.user.login.domain.toLoginRequest
import com.felipearpa.tyche.user.toProfile
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