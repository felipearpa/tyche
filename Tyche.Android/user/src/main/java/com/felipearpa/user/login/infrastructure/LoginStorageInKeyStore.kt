package com.felipearpa.user.login.infrastructure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.felipearpa.user.LoginProfile
import com.felipearpa.user.LoginStorage
import com.google.gson.Gson
import javax.inject.Inject

private const val FILE_NAME = "login"
private const val LOGIN_PROFILE_KEY = "loginProfile"

class LoginStorageInKeyStore @Inject constructor(private val context: Context) : LoginStorage {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    override suspend fun store(loginProfile: LoginProfile?) {
        val sharedPreferences = createSharedPreferences()
        if (loginProfile != null) {
            sharedPreferences.edit()
                .putString(LOGIN_PROFILE_KEY, Gson().toJson(loginProfile))
                .apply()
        } else {
            sharedPreferences.edit()
                .remove(LOGIN_PROFILE_KEY)
                .apply()
        }
    }

    override suspend fun get(): LoginProfile? {
        val sharedPreferences = createSharedPreferences()
        return sharedPreferences.getString(LOGIN_PROFILE_KEY, null).let { encodedLoginProfile ->
            if (encodedLoginProfile == null) {
                return@let null
            }
            return@let Gson().fromJson(
                encodedLoginProfile,
                LoginProfile::class.java
            )
        }
    }

    private fun createSharedPreferences(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}