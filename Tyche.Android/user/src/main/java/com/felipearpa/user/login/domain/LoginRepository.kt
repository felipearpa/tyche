package com.felipearpa.user.login.domain

import com.felipearpa.user.LoginProfile
import com.felipearpa.user.User

interface LoginRepository {

    suspend fun login(user: User): Result<LoginProfile>
}