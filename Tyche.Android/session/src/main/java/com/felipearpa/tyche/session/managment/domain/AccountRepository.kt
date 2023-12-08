package com.felipearpa.tyche.session.managment.domain

import com.felipearpa.tyche.session.LoginBundle

interface AccountRepository {
    suspend fun create(account: Account): Result<LoginBundle>
}