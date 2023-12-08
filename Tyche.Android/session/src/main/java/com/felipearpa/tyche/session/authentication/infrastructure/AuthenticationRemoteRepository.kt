package com.felipearpa.tyche.session.authentication.infrastructure

import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.AuthBundle
import com.felipearpa.tyche.session.LoginBundle
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRemoteDataSource
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository
import com.felipearpa.tyche.session.authentication.domain.LoginCredential
import com.felipearpa.tyche.session.authentication.domain.LoginResponse
import com.felipearpa.tyche.session.authentication.domain.toFirebaseLoginRequest
import com.felipearpa.tyche.session.authentication.domain.toLoginRequest
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import javax.inject.Inject

internal class AuthenticationRemoteRepository @Inject constructor(
    private val authenticationRemoteDataSource: AuthenticationRemoteDataSource,
    private val authenticationFirebaseDataSource: AuthenticationFirebaseDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) :
    AuthenticationRepository {

    override suspend fun login(loginCredential: LoginCredential): Result<LoginBundle> {
        val firebaseLoginResult = loginFirebase(loginCredential)
        if (firebaseLoginResult.isFailure)
            return Result.failure(firebaseLoginResult.exceptionOrNull()!!)

        val remoteLoginResult = loginRemote(loginCredential)
        if (remoteLoginResult.isFailure)
            return Result.failure(remoteLoginResult.exceptionOrNull()!!)

        return buildResult(
            firebaseLoginResult = firebaseLoginResult,
            remoteLoginResult = remoteLoginResult
        )
    }

    override suspend fun logout(): Result<Unit> {
        authenticationFirebaseDataSource.logout()
        return Result.success(Unit)
    }

    private suspend fun loginFirebase(loginCredential: LoginCredential): Result<String> =
        handleFirebaseLogin {
            authenticationFirebaseDataSource.login(loginCredential.toFirebaseLoginRequest())
        }

    private suspend fun loginRemote(loginCredential: LoginCredential) =
        networkExceptionHandler.handleRemoteLogin {
            authenticationRemoteDataSource.login(loginRequest = loginCredential.toLoginRequest())
        }

    private suspend fun buildResult(
        firebaseLoginResult: Result<String>,
        remoteLoginResult: Result<LoginResponse>
    ): Result<LoginBundle> {
        val authToken = firebaseLoginResult.getOrNull()!!
        val userId = remoteLoginResult.getOrNull()!!.user.userId

        return Result.success(
            LoginBundle(
                authBundle = AuthBundle(authToken = authToken),
                accountBundle = AccountBundle(userId = userId)
            )
        )
    }
}