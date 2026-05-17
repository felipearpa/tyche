package com.felipearpa.tyche.session

import com.felipearpa.foundation.emptyString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountBundle(
    val accountId: String,
    val externalAccountId: String,
    val email: String = emptyString(),
    @SerialName("username") private val storedUsername: String = emptyString(),
) {
    val username: String
        get() = storedUsername.ifEmpty { email }

    fun withUsername(value: String): AccountBundle = copy(storedUsername = value)
}

fun emptyAccountBundle() =
    AccountBundle(
        accountId = emptyString(),
        externalAccountId = emptyString(),
        email = emptyString(),
        storedUsername = emptyString(),
    )
