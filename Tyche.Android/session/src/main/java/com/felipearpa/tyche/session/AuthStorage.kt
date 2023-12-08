package com.felipearpa.tyche.session

interface AuthStorage {
    suspend fun store(authBundle: AuthBundle)
    suspend fun delete()
    suspend fun retrieve(): AuthBundle?
}