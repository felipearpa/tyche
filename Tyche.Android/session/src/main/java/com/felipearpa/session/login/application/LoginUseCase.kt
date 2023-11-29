package com.felipearpa.session.login.application

import com.felipearpa.session.AccountBundle
import com.felipearpa.session.AccountStorage
import com.felipearpa.session.AuthStorage
import com.felipearpa.session.login.domain.LoginCredential
import com.felipearpa.session.login.domain.LoginRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val authStorage: AuthStorage,
    private val accountStorage: AccountStorage
) {
    suspend fun execute(loginCredential: LoginCredential): Result<AccountBundle> {
        val loginResult = loginRepository.login(loginCredential = loginCredential)
        loginResult.onSuccess { loginBundle ->
            authStorage.store(authBundle = loginBundle.authBundle)
            accountStorage.store(accountBundle = loginBundle.accountBundle)
        }
        return loginResult.map { loginBundle -> loginBundle.accountBundle }
    }
}