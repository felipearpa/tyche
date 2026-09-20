package com.felipearpa.tyche.session

import com.felipearpa.tyche.core.data.StorageInKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class AccountStorageInKeyStore(private val storageInKeyStore: StorageInKeyStore) :
    AccountStorage {

    override suspend fun store(snapshot: CurrentAccountSnapshot) = withContext(Dispatchers.IO) {
        storageInKeyStore.store(Json.encodeToString(snapshot))
    }

    override suspend fun delete() = withContext(Dispatchers.IO) {
        storageInKeyStore.delete()
    }

    override suspend fun retrieve(): CurrentAccountSnapshot? = withContext(Dispatchers.IO) {
        storageInKeyStore.retrieve()?.let(::decodeCurrentAccountSnapshot)
    }
}
