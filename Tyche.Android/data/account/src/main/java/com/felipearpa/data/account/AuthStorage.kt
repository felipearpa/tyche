package com.felipearpa.data.account

interface AuthStorage {
    suspend fun store(authBundle: AuthBundle)
    suspend fun delete()
    suspend fun retrieve(): AuthBundle?
}