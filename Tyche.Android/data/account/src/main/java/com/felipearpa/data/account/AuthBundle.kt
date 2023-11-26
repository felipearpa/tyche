package com.felipearpa.data.account

import kotlinx.serialization.Serializable

@Serializable
data class AuthBundle(val token: String)