package com.felipearpa.tyche.session

import kotlinx.coroutines.flow.StateFlow

interface AccountStorage {
    val state: StateFlow<AccountBundle?>
    suspend fun store(accountBundle: AccountBundle)
    suspend fun delete()
    suspend fun retrieve(): AccountBundle?
}
