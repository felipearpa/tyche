package com.felipearpa.user.account.infrastructure

import com.felipearpa.core.network.HttpStatusCode
import com.felipearpa.core.network.NetworkExceptionHandler
import com.felipearpa.core.network.recoverHttpException
import com.felipearpa.user.LoginProfile
import com.felipearpa.user.User
import com.felipearpa.user.account.domain.AccountRemoteDataSource
import com.felipearpa.user.account.domain.AccountRepository
import com.felipearpa.user.account.domain.toCreateUserRequest
import com.felipearpa.user.login.domain.LoginException
import com.felipearpa.user.toProfile
import javax.inject.Inject

class AccountRemoteRepository @Inject constructor(
    private val accountRemoteDataSource: AccountRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) :
    AccountRepository {

    override suspend fun create(user: User): Result<LoginProfile> {
        return networkExceptionHandler.handle {
            accountRemoteDataSource.create(user.toCreateUserRequest()).toProfile()
        }.recoverHttpException { exception ->
            return when (exception.httpStatusCode) {
                HttpStatusCode.BAD_REQUEST -> Result.failure(LoginException.InvalidCredentials)
                else -> Result.failure(exception)
            }
        }
    }
}