package com.felipearpa.tyche.pool.joiner

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.exception.LocalizedException

data object PoolJoinRequiresSignInException : LocalizedException() {
    @Suppress("unused")
    private fun readResolve(): Any = PoolJoinRequiresSignInException

    override val errorDescription: String
        @Composable get() = stringResource(id = R.string.pool_join_requires_sign_in_failure_description)

    override val failureReason: String
        @Composable get() = stringResource(id = R.string.pool_join_requires_sign_in_failure_reason)

    override val recoverySuggestion: String
        @Composable get() = stringResource(id = R.string.pool_join_requires_sign_in_failure_recovery_suggestion)
}
