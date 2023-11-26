package com.felipearpa.data.account

import com.felipearpa.tyche.core.data.StorageInKeyStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class AuthStorageInKeyStore @Inject constructor(private val storageInKeyStore: StorageInKeyStore) :
    AuthStorage {
    override suspend fun store(authBundle: AuthBundle) {
        storageInKeyStore.store(Json.encodeToString(authBundle))
    }

    override suspend fun delete() {
        storageInKeyStore.delete()
    }

    override suspend fun retrieve(): AuthBundle? {
        return storageInKeyStore.retrieve()?.let { raw -> Json.decodeFromString(raw) }
    }
}