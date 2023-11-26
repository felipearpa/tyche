package com.felipearpa.data.account.managment.domain

import com.felipearpa.data.account.LoginBundle

interface AccountRepository {
    suspend fun create(account: Account): Result<LoginBundle>
}