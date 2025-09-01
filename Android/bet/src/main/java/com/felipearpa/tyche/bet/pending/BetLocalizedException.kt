package com.felipearpa.tyche.bet.pending

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.data.bet.domain.BetException
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class BetLocalizedException : LocalizedException() {
    data object Forbidden : BetLocalizedException() {
        @Suppress("unused")
        private fun readResolve(): Any = Forbidden

        override val errorDescription: String
            @Composable get() =
                stringResource(id = R.string.forbidden_bet_title)

        override val failureReason: String
            @Composable get() =
                stringResource(id = R.string.forbidden_bet_message)
    }
}

fun Throwable.asBetLocalizedExceptionOnMatch() =
    when (this) {
        BetException.Forbidden -> BetLocalizedException.Forbidden
        else -> this
    }
