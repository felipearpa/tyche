package com.felipearpa.tyche.session

import com.felipearpa.foundation.emptyString
import kotlinx.serialization.Serializable

@Serializable
data class AccountBundle(val accountId: String, val externalAccountId: String)

fun emptyAccountBundle() =
    AccountBundle(accountId = emptyString(), externalAccountId = emptyString())
