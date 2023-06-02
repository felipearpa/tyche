package com.felipearpa.tyche.user

interface LoginStorage {

    suspend fun store(loginProfile: LoginProfile?)

    suspend fun get(): LoginProfile?
}