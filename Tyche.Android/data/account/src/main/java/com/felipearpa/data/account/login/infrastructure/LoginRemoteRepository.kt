package com.felipearpa.data.account.login.infrastructure

import com.felipearpa.data.account.LoginBundle
import com.felipearpa.data.account.login.domain.LoginCredential
import com.felipearpa.data.account.login.domain.LoginException
import com.felipearpa.data.account.login.domain.LoginRemoteDataSource
import com.felipearpa.data.account.login.domain.LoginRepository
import com.felipearpa.data.account.login.domain.toLoginRequest
import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.recoverHttpException
import javax.inject.Inject

internal class LoginRemoteRepository @Inject constructor(
    private val loginRemoteDataSource: LoginRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) :
    LoginRepository {

    override suspend fun login(loginCredential: LoginCredential): Result<LoginBundle> {
        return networkExceptionHandler.handle {
            loginRemoteDataSource.login(loginRequest = loginCredential.toLoginRequest())
                .toLoginBundle()
        }.recoverHttpException { exception ->
            return when (exception.httpStatusCode) {
                HttpStatusCode.CONFLICT -> Result.failure(LoginException.InvalidCredential)
                else -> Result.failure(exception)
            }
        }
    }
}