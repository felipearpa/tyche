package com.felipearpa.tyche.session.authentication.application

import com.felipearpa.tyche.session.CurrentAccountCoordinator
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository

class LogOut(
    private val authenticationRepository: AuthenticationRepository,
    private val currentAccountCoordinator: CurrentAccountCoordinator,
) {
    suspend fun execute(): Result<Unit> {
        val logoutResult = authenticationRepository.logout()
        logoutResult.onSuccess {
            currentAccountCoordinator.clear()
        }
        return logoutResult
    }
}
