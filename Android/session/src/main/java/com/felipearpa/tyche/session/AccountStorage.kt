package com.felipearpa.tyche.session

interface AccountStorage {
    suspend fun store(accountBundle: AccountBundle)
    suspend fun delete()
    suspend fun retrieve(): AccountBundle?
}