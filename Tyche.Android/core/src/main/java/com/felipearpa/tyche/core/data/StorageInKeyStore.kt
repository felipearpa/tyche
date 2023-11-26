package com.felipearpa.tyche.core.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class StorageInKeyStore(
    private val context: Context,
    private val key: String,
    private val filename: String
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    fun store(raw: String) {
        val sharedPreferences = createSharedPreferences()
        sharedPreferences.edit()
            .putString(key, raw)
            .apply()
    }

    fun delete() {
        val sharedPreferences = createSharedPreferences()
        sharedPreferences.edit()
            .remove(key)
            .apply()
    }

    fun retrieve(): String? {
        val sharedPreferences = createSharedPreferences()
        return sharedPreferences.getString(key, null)
    }

    private fun createSharedPreferences(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            filename,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}