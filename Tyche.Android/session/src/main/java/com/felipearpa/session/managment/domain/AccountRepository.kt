package com.felipearpa.session.managment.domain

import com.felipearpa.session.LoginBundle

interface AccountRepository {
    suspend fun create(account: Account): Result<LoginBundle>
}