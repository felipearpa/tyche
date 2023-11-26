package com.felipearpa.data.account

import com.felipearpa.tyche.core.data.StorageInKeyStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class AccountStorageInKeyStore @Inject constructor(private val storageInKeyStore: StorageInKeyStore) :
    AccountStorage {
    override suspend fun store(accountBundle: AccountBundle) {
        storageInKeyStore.store(Json.encodeToString(accountBundle))
    }

    override suspend fun delete() {
        storageInKeyStore.delete()
    }

    override suspend fun retrieve(): AccountBundle? {
        return storageInKeyStore.retrieve()?.let { raw -> Json.decodeFromString(raw) }
    }
}