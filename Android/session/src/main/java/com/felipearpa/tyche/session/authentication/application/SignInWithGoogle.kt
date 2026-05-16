package com.felipearpa.tyche.session.authentication.application

import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.authentication.domain.AccountLink
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInException

class SignInWithGoogle(
    private val authenticationRepository: AuthenticationRepository,
    private val accountStorage: AccountStorage,
) {
    suspend fun execute(idToken: String): Result<AccountBundle> {
        val googleResult =
            authenticationRepository.signInWithGoogle(idToken = idToken)
                .onFailure { exception -> return Result.failure(exception) }
                .getOrNull()!!

        if (!Email.isValid(googleResult.email)) {
            return Result.failure(GoogleSignInException.InvalidCredential)
        }
        val email = Email(googleResult.email)

        val accountBundle = authenticationRepository.linkAccount(
            accountLink = AccountLink(
                email = email,
                externalAccountId = googleResult.externalAccountId,
            ),
        ).onFailure { exception -> return Result.failure(exception) }
            .getOrNull()!!

        accountStorage.store(accountBundle = accountBundle)

        return Result.success(accountBundle)
    }
}
