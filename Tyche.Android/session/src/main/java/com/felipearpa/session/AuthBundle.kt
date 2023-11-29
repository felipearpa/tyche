package com.felipearpa.session

import kotlinx.serialization.Serializable

@Serializable
data class AuthBundle(val token: String)