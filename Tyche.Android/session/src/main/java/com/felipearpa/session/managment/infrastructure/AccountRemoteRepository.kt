package com.felipearpa.session.managment.infrastructure

import com.felipearpa.session.LoginBundle
import com.felipearpa.session.managment.domain.Account
import com.felipearpa.session.managment.domain.AccountRemoteDataSource
import com.felipearpa.session.managment.domain.AccountRepository
import com.felipearpa.session.managment.domain.toCreateUserRequest
import com.felipearpa.session.login.domain.LoginException
import com.felipearpa.session.login.domain.toLoginBundle
import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.recoverHttpException
import javax.inject.Inject

internal class AccountRemoteRepository @Inject constructor(
    private val accountRemoteDataSource: AccountRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) :
    AccountRepository {

    override suspend fun create(account: Account): Result<LoginBundle> {
        return networkExceptionHandler.handle {
            accountRemoteDataSource.create(account.toCreateUserRequest()).toLoginBundle()
        }.recoverHttpException { exception ->
            return when (exception.httpStatusCode) {
                HttpStatusCode.BAD_REQUEST -> Result.failure(LoginException.InvalidCredential)
                else -> Result.failure(exception)
            }
        }
    }
}