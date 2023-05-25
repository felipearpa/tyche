package com.felipearpa.user.login.infrastructure

import com.felipearpa.core.network.HttpStatusCode
import com.felipearpa.core.network.NetworkException
import com.felipearpa.core.network.NetworkExceptionHandler
import com.felipearpa.user.LoginResponse
import com.felipearpa.user.User
import com.felipearpa.user.account.domain.CreateUserException
import com.felipearpa.user.login.domain.LoginRemoteDataSource
import com.felipearpa.user.type.Password
import com.felipearpa.user.type.Username
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginRemoteRepositoryTest {

    private val loginRemoteDataSource = mockk<LoginRemoteDataSource>()

    private val networkExceptionHandler = mockk<NetworkExceptionHandler>()

    private val loginRemoteRepository = LoginRemoteRepository(
        loginRemoteDataSource = loginRemoteDataSource,
        networkExceptionHandler = networkExceptionHandler
    )

    @Before
    fun setUp() {
        clearAllMocks()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given a http exception with code 'conflict' when login is performed then an user already registered failure result is returned`() =
        runTest {
            val httpException =
                NetworkException.HttpException(httpStatusCode = HttpStatusCode.CONFLICT)

            coEvery { networkExceptionHandler.handle<LoginResponse>(block = any()) } returns
                    Result.failure(
                        httpException
                    )

            val result = loginRemoteRepository.login(
                user = User(
                    username = Username("tyche-user"),
                    password = Password("$1Pass1$")
                )
            )

            assertTrue(result.isFailure)
            assertEquals(CreateUserException.UserAlreadyRegistered, result.exceptionOrNull())
        }
}