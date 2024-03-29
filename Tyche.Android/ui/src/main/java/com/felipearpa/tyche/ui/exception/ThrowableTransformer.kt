package com.felipearpa.tyche.ui.exception

import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.ui.network.toNetworkLocalizedException

fun Throwable.orLocalizedException() =
    when (this) {
        is LocalizedException -> this
        is NetworkException -> this.toNetworkLocalizedException()
        else -> UnknownLocalizedException()
    }

fun Throwable.localizedExceptionOrNull(): LocalizedException? {
    if (this is LocalizedException)
        return this
    return null
}