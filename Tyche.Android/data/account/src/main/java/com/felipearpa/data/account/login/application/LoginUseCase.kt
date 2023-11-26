package com.felipearpa.data.account.login.application

import com.felipearpa.data.account.AccountBundle
import com.felipearpa.data.account.AccountStorage
import com.felipearpa.data.account.AuthStorage
import com.felipearpa.data.account.login.domain.LoginCredential
import com.felipearpa.data.account.login.domain.LoginRepository
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