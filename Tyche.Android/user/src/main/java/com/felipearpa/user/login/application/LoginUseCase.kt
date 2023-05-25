package com.felipearpa.user.login.application

import com.felipearpa.user.LoginStorage
import com.felipearpa.user.UserProfile
import com.felipearpa.user.login.domain.LoginRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val loginStorage: LoginStorage
) {

    suspend fun execute(loginInput: LoginInput): Result<UserProfile> {
        val maybeLoginProfile = loginRepository.login(user = loginInput.toUser())
        maybeLoginProfile.onSuccess { loginProfile -> loginStorage.store(loginProfile = loginProfile) }
        return maybeLoginProfile.map { logingProfile -> logingProfile.user }
    }
}