package com.felipearpa.tyche.session.authentication.application

import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.authentication.domain.LoginCredential
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
    private val accountStorage: AccountStorage
) {
    suspend fun execute(loginCredential: LoginCredential): Result<AccountBundle> {
        val loginResult = authenticationRepository.login(loginCredential = loginCredential)
        loginResult.onSuccess { loginBundle ->
            accountStorage.store(accountBundle = loginBundle.accountBundle)
        }
        return loginResult.map { loginBundle -> loginBundle.accountBundle }
    }
}