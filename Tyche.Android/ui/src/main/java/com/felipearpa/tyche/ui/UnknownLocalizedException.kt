package com.felipearpa.tyche.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

class UnknownLocalizedException : LocalizedException() {

    override val errorDescription: String?
        @Composable get() = stringResource(id = R.string.unknown_failure_message)

    override val failureReason: String?
        @Composable get() = stringResource(id = R.string.unknown_failure_title)
}