package com.felipearpa.tyche.session.authentication.application

import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository

class UpdateUsername(
    private val authenticationRepository: AuthenticationRepository,
    private val accountStorage: AccountStorage,
) {
    suspend fun execute(username: String): Result<String> {
        val bundle = accountStorage.retrieve()
            ?: return Result.failure(IllegalStateException("No account in storage"))

        return authenticationRepository
            .updateUsername(accountId = bundle.accountId, username = username)
            .map {
                accountStorage.store(bundle.withUsername(username))
                username
            }
    }
}
