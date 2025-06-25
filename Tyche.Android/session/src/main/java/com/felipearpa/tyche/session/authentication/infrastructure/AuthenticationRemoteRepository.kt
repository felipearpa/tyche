package com.felipearpa.tyche.session.authentication.infrastructure

import com.felipearpa.network.NetworkExceptionHandler
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
    private val networkExceptionHandler: NetworkExceptionHandler,
) : AuthenticationRepository {
    override suspend fun sendSignInLinkToEmail(email: String) =
        handleFirebaseSendSignInLinkToEmail {
            authenticationExternalDataSource.sendSignInLinkToEmail(email = email)
        }

    override suspend fun signInWithEmailLink(
        email: String,
        emailLink: String,
    ): Result<ExternalAccountId> {
        if (!authenticationExternalDataSource.isSignInWithEmailLink(emailLink = emailLink))
            return Result.failure(SignInWithEmailLinkException.InvalidEmailLink)

        return handleFirebaseSignInWithEmailLink {
            authenticationExternalDataSource.signInWithEmailLink(
                email = email,
                emailLink = emailLink,
            )
        }
    }

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<ExternalAccountId> {
        return handleFirebaseSignInWithEmailAndPassword {
            authenticationExternalDataSource.signInWithEmailAndPassword(
                email = email,
                password = password,
            )
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            authenticationExternalDataSource.signOut()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun linkAccount(accountLink: AccountLink): Result<AccountBundle> =
        networkExceptionHandler.handle {
            authenticationRemoteDataSource.linkAccount(request = accountLink.toLinkAccountRequest())
                .run {
                    AccountBundle(
                        accountId = this.accountId,
                        externalAccountId = accountLink.externalAccountId,
                    )
                }
        }
}
