package com.felipearpa.tyche.session.authentication.domain

sealed class SendSignInLinkToEmailException: Throwable() {
    data object TooManyRequests : SendSignInLinkToEmailException() {
        @Suppress("unused")
        private fun readResolve(): Any = TooManyRequests
    }
}
