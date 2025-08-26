package com.felipearpa.tyche.ui.exception

import androidx.compose.runtime.Composable

open class LocalizedException : Throwable() {
    open val errorDescription: String?
        @Composable get() {
            return null
        }

    open val failureReason: String?
        @Composable get() {
            return null
        }

    open val recoverySuggestion: String?
        @Composable get() {
            return null
        }

    open val helpAnchor: String?
        @Composable get() {
            return null
        }
}
