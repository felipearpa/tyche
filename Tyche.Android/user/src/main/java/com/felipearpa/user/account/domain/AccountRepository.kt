package com.felipearpa.user.account.domain

import com.felipearpa.user.LoginProfile
import com.felipearpa.user.User

interface AccountRepository {

    suspend fun create(user: User): Result<LoginProfile>
}