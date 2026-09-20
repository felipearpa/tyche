package com.felipearpa.tyche.account

/**
 * Avatar objects live at a deterministic key, so the URL is a pure function of the account id.
 * Swapping the host (e.g. to a CDN) is a one-constant change.
 */
object AvatarUrl {
    fun of(accountId: String): String = "$PATH_PREFIX$accountId$EXTENSION"

    /** The account an avatar URL belongs to, or `null` for any other URL. */
    fun accountIdOf(url: String): String? =
        url.substringBefore('?')
            .takeIf { path -> path.startsWith(PATH_PREFIX) && path.endsWith(EXTENSION) }
            ?.removePrefix(PATH_PREFIX)
            ?.removeSuffix(EXTENSION)
            ?.takeIf { accountId -> accountId.isNotEmpty() && '/' !in accountId }
}

private const val HOST = "https://tyche-avatars.s3.us-east-2.amazonaws.com"
private const val PATH_PREFIX = "$HOST/avatars/"
private const val EXTENSION = ".jpg"
