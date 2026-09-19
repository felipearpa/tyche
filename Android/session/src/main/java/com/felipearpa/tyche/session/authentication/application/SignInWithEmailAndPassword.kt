package com.felipearpa.tyche.session.authentication.application

import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.CurrentAccountCoordinator
import com.felipearpa.tyche.session.authentication.domain.AccountLink
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository

class SignInWithEmailAndPassword(
    private val authenticationRepository: AuthenticationRepository,
    private val currentAccountCoordinator: CurrentAccountCoordinator,
) {
    suspend fun execute(email: Email, password: String): Result<AccountBundle> {
        val externalAccountId =
            authenticationRepository.signInWithEmailAndPassword(
                email = email.value,
                password = password,
            ).onFailure { exception -> return Result.failure(exception) }
                .getOrNull()!!

        val accountBundle = authenticationRepository.linkAccount(
            accountLink = AccountLink(
                email = email,
                externalAccountId = externalAccountId,
            ),
        ).onFailure { exception -> return Result.failure(exception) }
            .getOrNull()!!

        currentAccountCoordinator.install(account = accountBundle)

        return Result.success(accountBundle)
    }
}
