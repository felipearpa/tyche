package com.felipearpa.tyche.bet

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.data.bet.domain.BetException
import com.felipearpa.tyche.ui.LocalizedException

sealed class BetLocalizedException : LocalizedException() {
    data object Forbidden : BetLocalizedException()

    override val errorDescription: String?
        @Composable get() =
            when (this) {
                Forbidden ->
                    stringResource(id = R.string.forbidden_bet_title)
            }

    override val failureReason: String?
        @Composable get() =
            when (this) {
                Forbidden ->
                    stringResource(id = R.string.forbidden_bet_message)
            }
}

fun BetException.toBetLocalizedException() =
    when (this) {
        BetException.Forbidden -> BetLocalizedException.Forbidden
    }