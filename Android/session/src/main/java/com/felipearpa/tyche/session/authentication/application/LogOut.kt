package com.felipearpa.tyche.session.authentication.application

import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository

class LogOut(
    private val authenticationRepository: AuthenticationRepository,
    private val accountStorage: AccountStorage,
) {
    suspend fun execute(): Result<Unit> {
        val logoutResult = authenticationRepository.logout()
        logoutResult.onSuccess {
            accountStorage.delete()
        }
        return logoutResult
    }
}
