package com.felipearpa.tyche.user.account.infrastructure

import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.recoverHttpException
import com.felipearpa.tyche.user.LoginProfile
import com.felipearpa.tyche.user.account.domain.AccountRemoteDataSource
import com.felipearpa.tyche.user.account.domain.AccountRepository
import com.felipearpa.tyche.user.account.domain.User
import com.felipearpa.tyche.user.account.domain.toCreateUserRequest
import com.felipearpa.tyche.user.login.domain.LoginException
import com.felipearpa.tyche.user.toLoginProfile
import javax.inject.Inject

class AccountRemoteRepository @Inject constructor(
    private val accountRemoteDataSource: AccountRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) :
    AccountRepository {

    override suspend fun create(user: User): Result<LoginProfile> {
        return networkExceptionHandler.handle {
            accountRemoteDataSource.create(user.toCreateUserRequest()).toLoginProfile()
        }.recoverHttpException { exception ->
            return when (exception.httpStatusCode) {
                HttpStatusCode.BAD_REQUEST -> Result.failure(LoginException.InvalidCredentials)
                else -> Result.failure(exception)
            }
        }
    }
}