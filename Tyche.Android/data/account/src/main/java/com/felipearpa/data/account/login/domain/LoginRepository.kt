package com.felipearpa.data.account.login.domain

import com.felipearpa.data.account.LoginBundle

interface LoginRepository {
    suspend fun login(loginCredential: LoginCredential): Result<LoginBundle>
}