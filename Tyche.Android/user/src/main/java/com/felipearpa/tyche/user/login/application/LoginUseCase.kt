package com.felipearpa.tyche.user.login.application

import com.felipearpa.tyche.user.LoginStorage
import com.felipearpa.tyche.user.UserProfile
import com.felipearpa.tyche.user.login.domain.LoginRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val loginStorage: LoginStorage
) {

    suspend fun execute(loginInput: LoginInput): Result<UserProfile> {
        val maybeLoginProfile = loginRepository.login(user = loginInput.toUser())
        maybeLoginProfile.onSuccess { loginProfile -> loginStorage.store(loginProfile = loginProfile) }
        return maybeLoginProfile.map { loginProfile -> loginProfile.user }
    }
}