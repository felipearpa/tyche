package com.felipearpa.tyche.session.authentication.domain

import com.felipearpa.tyche.session.LoginBundle

interface AuthenticationRepository {
    suspend fun login(loginCredential: LoginCredential): Result<LoginBundle>
    suspend fun logout(): Result<Unit>
}