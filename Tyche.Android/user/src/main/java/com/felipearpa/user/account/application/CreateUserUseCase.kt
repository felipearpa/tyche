package com.felipearpa.user.account.application

import com.felipearpa.user.LoginStorage
import com.felipearpa.user.UserProfile
import com.felipearpa.user.account.domain.AccountRepository
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