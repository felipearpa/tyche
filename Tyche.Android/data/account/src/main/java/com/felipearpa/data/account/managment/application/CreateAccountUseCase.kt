package com.felipearpa.data.account.managment.application

import com.felipearpa.data.account.AccountBundle
import com.felipearpa.data.account.AccountStorage
import com.felipearpa.data.account.AuthStorage
import com.felipearpa.data.account.managment.domain.Account
import com.felipearpa.data.account.managment.domain.AccountRepository
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