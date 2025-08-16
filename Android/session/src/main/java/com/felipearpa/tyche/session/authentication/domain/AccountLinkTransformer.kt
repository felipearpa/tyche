package com.felipearpa.tyche.session.authentication.domain

internal fun AccountLink.toLinkAccountRequest() =
    LinkAccountRequest(
        email = this.email.value,
        externalAccountId = this.externalAccountId
    )