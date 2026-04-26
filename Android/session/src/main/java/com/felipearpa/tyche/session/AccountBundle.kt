package com.felipearpa.tyche.session

import com.felipearpa.foundation.emptyString
import kotlinx.serialization.Serializable

@Serializable
data class AccountBundle(
    val accountId: String,
    val externalAccountId: String,
    val email: String = emptyString(),
)

fun emptyAccountBundle() =
    AccountBundle(
        accountId = emptyString(),
        externalAccountId = emptyString(),
        email = emptyString(),
    )
