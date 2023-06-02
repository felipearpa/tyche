package com.felipearpa.tyche.user.account.domain

import com.felipearpa.tyche.user.LoginProfile
import com.felipearpa.tyche.user.User

interface AccountRepository {

    suspend fun create(user: User): Result<LoginProfile>
}