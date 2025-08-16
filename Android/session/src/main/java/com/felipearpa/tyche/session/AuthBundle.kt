package com.felipearpa.tyche.session

import kotlinx.serialization.Serializable

@Serializable
data class AuthBundle(val authToken: String)