package com.felipearpa.session.managment.application

import com.felipearpa.session.AccountBundle
import com.felipearpa.session.AccountStorage
import com.felipearpa.session.AuthStorage
import com.felipearpa.session.managment.domain.Account
import com.felipearpa.session.managment.domain.AccountRepository
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val loginStorage: AuthStorage,
    private val accountStorage: AccountStorage
) {
    suspend fun execute(account: Account): Result<AccountBundle> {
        val creationResult = accountRepository.create(account = account)
        creationResult.onSuccess { loginBundle ->
            loginStorage.store(authBundle = loginBundle.authBundle)
            accountStorage.store(accountBundle = loginBundle.accountBundle)
        }
        return creationResult.map { loginBundle -> loginBundle.accountBundle }
    }
}