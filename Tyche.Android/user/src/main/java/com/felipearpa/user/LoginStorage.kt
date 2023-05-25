package com.felipearpa.user

interface LoginStorage {

    suspend fun store(loginProfile: LoginProfile?)

    suspend fun get(): LoginProfile?
}