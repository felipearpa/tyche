package com.felipearpa.tyche.ui.exception

import com.felipearpa.network.NetworkException
import com.felipearpa.tyche.ui.network.toNetworkLocalizedException

fun Throwable.orDefaultLocalized() =
    when (this) {
        is LocalizedException -> this
        is NetworkException -> this.toNetworkLocalizedException()
        else -> UnknownLocalizedException()
    }

fun Throwable.mapOrDefaultLocalized(transform: (Throwable) -> Throwable) {
    val transformedError = transform(this)
    when (transformedError) {
        is LocalizedException -> transformedError
        is NetworkException -> transformedError.toNetworkLocalizedException()
        else -> UnknownLocalizedException()
    }
}

fun Throwable.localizedOrDefault(): LocalizedException {
    if (this is LocalizedException)
        return this
    return UnknownLocalizedException()
}
