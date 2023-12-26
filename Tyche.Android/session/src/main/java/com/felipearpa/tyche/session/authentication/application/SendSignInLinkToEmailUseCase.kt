package com.felipearpa.tyche.session.authentication.application

import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository

class SendSignInLinkToEmailUseCase(private val authenticationRepository: AuthenticationRepository) {
    suspend fun execute(email: Email) =
        authenticationRepository.sendSignInLinkToEmail(email = email.value)
}