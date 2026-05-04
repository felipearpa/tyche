package com.felipearpa.tyche.pool.joiner

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.data.pool.domain.JoinPoolException
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class JoinPoolLocalizedException : LocalizedException() {
    class AlreadyJoined : JoinPoolLocalizedException() {
        override val errorDescription: String
            @Composable get() =
                stringResource(id = R.string.pool_already_joined_error_description)

        override val failureReason: String
            @Composable get() =
                stringResource(id = R.string.pool_already_joined_failure_reason)

        override val recoverySuggestion: String?
            @Composable get() = stringResource(id = R.string.pool_already_joined_recovery_suggestion)
    }
}

fun Throwable.asJoinPoolLocalizedException() =
    when (this) {
        is JoinPoolException.AlreadyJoined -> JoinPoolLocalizedException.AlreadyJoined()
        else -> this
    }
