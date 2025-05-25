package com.felipearpa.tyche.ui.exception

import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.ui.network.toNetworkLocalizedException

fun Throwable.orDefaultLocalized() =
    when (this) {
        is LocalizedException -> this
        is NetworkException -> this.toNetworkLocalizedException()
        else -> UnknownLocalizedException()
    }

fun Throwable.localizedOrNull(): LocalizedException? {
    if (this is LocalizedException)
        return this
    return null
}

fun Throwable.localizedOrDefault(): LocalizedException {
    if (this is LocalizedException)
        return this
    return UnknownLocalizedException()
}
