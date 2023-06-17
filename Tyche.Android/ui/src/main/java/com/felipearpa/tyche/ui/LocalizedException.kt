package com.felipearpa.tyche.ui

import androidx.compose.runtime.Composable

open class LocalizedException(
    @get:Composable open val errorDescription: String? = null,
    @get:Composable open val failureReason: String? = null,
    @get:Composable open val recoverySuggestion: String? = null,
    @get:Composable open val helpAnchor: String? = null
) : Throwable()