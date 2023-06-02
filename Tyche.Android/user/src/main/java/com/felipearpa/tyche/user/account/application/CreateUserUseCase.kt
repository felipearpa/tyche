package com.felipearpa.tyche.user.account.application

import com.felipearpa.tyche.user.LoginStorage
import com.felipearpa.tyche.user.UserProfile
import com.felipearpa.tyche.user.account.domain.AccountRepository
import javax.inject.Inject

class CreateUserUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val loginStorage: LoginStorage
) {

    suspend fun execute(createUserInput: CreateUserInput): Result<UserProfile> {
        val maybeLoginProfile = accountRepository.create(createUserInput.toUser())
        maybeLoginProfile.onSuccess { loginProfile -> loginStorage.store(loginProfile = loginProfile) }
        return maybeLoginProfile.map { loginProfile -> loginProfile.user }
    }
}