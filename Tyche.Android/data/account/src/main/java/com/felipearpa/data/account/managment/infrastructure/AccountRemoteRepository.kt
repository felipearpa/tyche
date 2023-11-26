package com.felipearpa.data.account.managment.infrastructure

import com.felipearpa.data.account.LoginBundle
import com.felipearpa.data.account.managment.domain.Account
import com.felipearpa.data.account.managment.domain.AccountRemoteDataSource
import com.felipearpa.data.account.managment.domain.AccountRepository
import com.felipearpa.data.account.managment.domain.toCreateUserRequest
import com.felipearpa.data.account.login.domain.LoginException
import com.felipearpa.data.account.login.infrastructure.toLoginBundle
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