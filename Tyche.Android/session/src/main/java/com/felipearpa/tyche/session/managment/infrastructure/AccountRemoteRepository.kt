package com.felipearpa.tyche.session.managment.infrastructure

import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.AuthBundle
import com.felipearpa.tyche.session.LoginBundle
import com.felipearpa.tyche.session.managment.domain.Account
import com.felipearpa.tyche.session.managment.domain.AccountRemoteDataSource
import com.felipearpa.tyche.session.managment.domain.AccountRepository
import com.felipearpa.tyche.session.managment.domain.AccountCreationResponse
import com.felipearpa.tyche.session.managment.domain.toCreateAccountRequest
import com.felipearpa.tyche.session.managment.domain.toCreateFirebaseAccountRequest
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import javax.inject.Inject

internal class AccountRemoteRepository @Inject constructor(
    private val accountRemoteDataSource: AccountRemoteDataSource,
    private val accountFirebaseDataSource: AccountFirebaseDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) :
    AccountRepository {

    override suspend fun create(account: Account): Result<LoginBundle> {
        val remoteAccountCreationResult = createRemoteAccount(account)
        if (remoteAccountCreationResult.isFailure)
            return Result.failure(remoteAccountCreationResult.exceptionOrNull()!!)

        val firebaseAccountCreationResult = createFirebaseAccount(account)
        if (firebaseAccountCreationResult.isFailure)
            return Result.failure(firebaseAccountCreationResult.exceptionOrNull()!!)

        return buildResult(
            firebaseAccountCreationResult = firebaseAccountCreationResult,
            remoteAccountCreationResult = remoteAccountCreationResult
        )
    }

    private suspend fun createFirebaseAccount(account: Account): Result<String> =
        handleFirebaseAccountCreation {
            accountFirebaseDataSource.create(account.toCreateFirebaseAccountRequest())
        }

    private suspend fun createRemoteAccount(account: Account): Result<AccountCreationResponse> =
        networkExceptionHandler.handleRemoteAccountCreation {
            accountRemoteDataSource.create(account.toCreateAccountRequest())
        }

    private fun buildResult(
        firebaseAccountCreationResult: Result<String>,
        remoteAccountCreationResult: Result<AccountCreationResponse>
    ): Result<LoginBundle> {
        val authToken = firebaseAccountCreationResult.getOrNull()!!
        val userId = remoteAccountCreationResult.getOrNull()!!.user.userId

        return Result.success(
            LoginBundle(
                authBundle = AuthBundle(authToken = authToken),
                accountBundle = AccountBundle(userId = userId)
            )
        )
    }
}