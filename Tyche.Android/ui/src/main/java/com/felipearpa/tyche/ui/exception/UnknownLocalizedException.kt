package com.felipearpa.tyche.ui.exception

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.ui.R

class UnknownLocalizedException : LocalizedException() {
    override val errorDescription: String?
        @Composable get() = stringResource(id = R.string.unknown_failure_description)

    override val failureReason: String?
        @Composable get() = stringResource(id = R.string.unknown_failure_reason)
}