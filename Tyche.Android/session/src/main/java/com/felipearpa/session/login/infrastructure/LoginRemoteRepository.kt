package com.felipearpa.session.login.infrastructure

import com.felipearpa.session.LoginBundle
import com.felipearpa.session.login.domain.LoginCredential
import com.felipearpa.session.login.domain.LoginException
import com.felipearpa.session.login.domain.LoginRemoteDataSource
import com.felipearpa.session.login.domain.LoginRepository
import com.felipearpa.session.login.domain.toLoginBundle
import com.felipearpa.session.login.domain.toLoginRequest
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