package com.felipearpa.tyche.user.login.domain

import com.felipearpa.tyche.user.LoginProfile

interface LoginRepository {

    suspend fun login(loginCredential: LoginCredential): Result<LoginProfile>
}