package com.felipearpa.tyche.session.authentication.application

import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.authentication.domain.AccountLink
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository

class SignInWithEmailAndPasswordUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val accountStorage: AccountStorage
) {
    suspend fun execute(email: Email, password: String): Result<AccountBundle> {
        val externalAccountId =
            authenticationRepository.signInWithEmailAndPassword(
                email = email.value,
                password = password
            )
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

        accountStorage.store(accountBundle = accountBundle)

        return Result.success(accountBundle)
    }
}