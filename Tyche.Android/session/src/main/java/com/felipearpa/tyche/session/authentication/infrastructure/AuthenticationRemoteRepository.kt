package com.felipearpa.tyche.session.authentication.infrastructure

import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.authentication.domain.AccountLink
import com.felipearpa.tyche.session.authentication.domain.AuthenticationExternalDataSource
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRemoteDataSource
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository
import com.felipearpa.tyche.session.authentication.domain.ExternalAccountId
import com.felipearpa.tyche.session.authentication.domain.SignInWithEmailLinkException
import com.felipearpa.tyche.session.authentication.domain.toLinkAccountRequest
import javax.inject.Inject

internal class AuthenticationRemoteRepository @Inject constructor(
    private val authenticationExternalDataSource: AuthenticationExternalDataSource,
    private val authenticationRemoteDataSource: AuthenticationRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) : AuthenticationRepository {
    override suspend fun sendSignInLinkToEmail(email: String) =
        handleFirebaseSignInWithEmail {
            authenticationExternalDataSource.sendSignInLinkToEmail(email = email)
        }

    override suspend fun signInWithEmailLink(
        email: String,
        emailLink: String
    ): Result<ExternalAccountId> {
        if (!authenticationExternalDataSource.isSignInWithEmailLink(emailLink = emailLink))
            return Result.failure(SignInWithEmailLinkException.InvalidEmailLink)

        val externalAccountId = handleFirebaseSignInWithEmailLink {
            authenticationExternalDataSource.signInWithEmailLink(
                email = email,
                emailLink = emailLink
            )
        }
        return externalAccountId
    }

    override suspend fun logout(): Result<Unit> {
        authenticationExternalDataSource.logout()
        return Result.success(Unit)
    }

    override suspend fun linkAccount(accountLink: AccountLink): Result<AccountBundle> =
        networkExceptionHandler.handle {
            authenticationRemoteDataSource.linkAccount(request = accountLink.toLinkAccountRequest())
                .run {
                    AccountBundle(
                        accountId = this.accountId,
                        externalAccountId = accountLink.externalAccountId
                    )
                }
        }
}