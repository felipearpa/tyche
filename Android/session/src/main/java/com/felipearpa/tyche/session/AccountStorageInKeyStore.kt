package com.felipearpa.tyche.session

import com.felipearpa.tyche.core.data.StorageInKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class AccountStorageInKeyStore(private val storageInKeyStore: StorageInKeyStore) :
    AccountStorage {

    private val _state = MutableStateFlow(load())
    override val state: StateFlow<AccountBundle?> = _state.asStateFlow()

    override suspend fun store(accountBundle: AccountBundle) = withContext(Dispatchers.IO) {
        storageInKeyStore.store(Json.encodeToString(accountBundle))
        _state.value = accountBundle
    }

    override suspend fun delete() = withContext(Dispatchers.IO) {
        storageInKeyStore.delete()
        _state.value = null
    }

    override suspend fun retrieve(): AccountBundle? = withContext(Dispatchers.IO) {
        storageInKeyStore.retrieve()?.let { raw -> Json.decodeFromString(raw) }
    }

    private fun load(): AccountBundle? =
        storageInKeyStore.retrieve()?.let { raw -> Json.decodeFromString(raw) }
}
