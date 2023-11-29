package com.felipearpa.session.login.domain

import com.felipearpa.session.LoginBundle

interface LoginRepository {
    suspend fun login(loginCredential: LoginCredential): Result<LoginBundle>
}