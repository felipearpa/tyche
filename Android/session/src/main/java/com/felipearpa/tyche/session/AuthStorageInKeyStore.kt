package com.felipearpa.tyche.session

import com.felipearpa.tyche.core.data.StorageInKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class AuthStorageInKeyStore(private val storageInKeyStore: StorageInKeyStore) :
    AuthStorage {
    override suspend fun store(authBundle: AuthBundle) = withContext(Dispatchers.IO) {
        storageInKeyStore.store(Json.encodeToString(authBundle))
    }

    override suspend fun delete() = withContext(Dispatchers.IO) {
        storageInKeyStore.delete()
    }

    override suspend fun retrieve(): AuthBundle? = withContext(Dispatchers.IO) {
        storageInKeyStore.retrieve()?.let { raw -> Json.decodeFromString(raw) }
    }
}
