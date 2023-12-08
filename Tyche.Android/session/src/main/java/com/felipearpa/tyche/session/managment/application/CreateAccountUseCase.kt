package com.felipearpa.tyche.session.managment.application

import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.managment.domain.Account
import com.felipearpa.tyche.session.managment.domain.AccountRepository
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accountStorage: AccountStorage
) {
    suspend fun execute(account: Account): Result<AccountBundle> {
        val creationResult = accountRepository.create(account = account)
        creationResult.onSuccess { loginBundle ->
            accountStorage.store(accountBundle = loginBundle.accountBundle)
        }
        return creationResult.map { loginBundle -> loginBundle.accountBundle }
    }
}