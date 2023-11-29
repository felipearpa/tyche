package com.felipearpa.session

interface AccountStorage {
    suspend fun store(accountBundle: AccountBundle)
    suspend fun delete()
    suspend fun retrieve(): AccountBundle?
}