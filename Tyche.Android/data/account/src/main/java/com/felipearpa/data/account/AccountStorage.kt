package com.felipearpa.data.account

interface AccountStorage {
    suspend fun store(accountBundle: AccountBundle)
    suspend fun delete()
    suspend fun retrieve(): AccountBundle?
}