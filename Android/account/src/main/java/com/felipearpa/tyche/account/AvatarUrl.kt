package com.felipearpa.tyche.account

/**
 * Avatar objects live at a deterministic key under [host], so the URL is a pure function of the
 * account id.
 */
class AvatarUrl(host: String) {
    private val pathPrefix = "$host/avatars/"

    fun of(accountId: String): String = "$pathPrefix$accountId$EXTENSION"

    /** The account an avatar URL belongs to, or `null` for any other URL. */
    fun accountIdOf(url: String): String? =
        url.substringBefore('?')
            .takeIf { path -> path.startsWith(pathPrefix) && path.endsWith(EXTENSION) }
            ?.removePrefix(pathPrefix)
            ?.removeSuffix(EXTENSION)
            ?.takeIf { accountId -> accountId.isNotEmpty() && '/' !in accountId }
}

private const val EXTENSION = ".jpg"
