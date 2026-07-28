package com.felipearpa.tyche.account

/**
 * Avatar objects live at a deterministic key, so the URL is a pure function of the account id.
 * Swapping the host (e.g. to a CDN) is a one-constant change.
 */
object AvatarUrl {
    fun of(accountId: String): String = "$HOST/avatars/$accountId.jpg"
}

private const val HOST = "https://tyche-avatars.s3.us-east-2.amazonaws.com"
