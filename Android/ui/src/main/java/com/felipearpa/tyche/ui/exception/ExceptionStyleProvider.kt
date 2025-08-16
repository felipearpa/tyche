package com.felipearpa.tyche.ui.exception

import androidx.compose.material3.Typography

val Typography.errorDescription
    get() = this.titleLarge

val Typography.failureReason
    get() = this.bodyMedium

val Typography.recoverySuggestion
    get() = this.bodySmall
