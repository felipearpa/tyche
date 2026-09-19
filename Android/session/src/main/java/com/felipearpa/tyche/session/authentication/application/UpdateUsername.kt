package com.felipearpa.tyche.session.authentication.application

import com.felipearpa.tyche.session.CurrentAccountCoordinator

class UpdateUsername(
    private val currentAccountCoordinator: CurrentAccountCoordinator,
) {
    suspend fun execute(username: String): Result<String> =
        currentAccountCoordinator.updateUsername(username = username)
}
