package com.felipearpa.tyche.session.authentication.application

import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.CurrentAccountCoordinator
import com.felipearpa.tyche.session.authentication.domain.AccountLink
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository

class SignInWithEmailLink(
    private val authenticationRepository: AuthenticationRepository,
    private val currentAccountCoordinator: CurrentAccountCoordinator
) {
    suspend fun execute(email: Email, emailLink: String): Result<AccountBundle> {
        val externalAccountId =
            authenticationRepository.signInWithEmailLink(email = email.value, emailLink = emailLink)
                .onFailure { exception -> return Result.failure(exception) }
                .getOrNull()!!

        val accountBundle = authenticationRepository.linkAccount(
            accountLink = AccountLink(
                email = email,
                externalAccountId = externalAccountId
            )
        )
            .onFailure { exception -> return Result.failure(exception) }
            .getOrNull()!!

        currentAccountCoordinator.install(account = accountBundle)

        return Result.success(accountBundle)
    }
}
