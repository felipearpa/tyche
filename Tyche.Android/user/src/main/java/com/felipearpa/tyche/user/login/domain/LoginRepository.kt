package com.felipearpa.tyche.user.login.domain

import com.felipearpa.tyche.user.LoginProfile
import com.felipearpa.tyche.user.User

interface LoginRepository {

    suspend fun login(user: User): Result<LoginProfile>
}