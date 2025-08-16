package com.felipearpa.tyche.session.authentication.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class LinkAccountResponse(val accountId: String)
