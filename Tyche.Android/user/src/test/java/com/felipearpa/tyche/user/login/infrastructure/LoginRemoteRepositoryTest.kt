package com.felipearpa.tyche.user.login.infrastructure

import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.user.LoginResponse
import com.felipearpa.tyche.user.User
import com.felipearpa.tyche.user.login.domain.LoginException
import com.felipearpa.tyche.user.login.domain.LoginRemoteDataSource
import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
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

    @Test
    fun `given a http exception with code 'conflict' when login is performed then an user already registered failure result is returned`() =
        runTest {
            val httpException =
                NetworkException.Http(httpStatusCode = HttpStatusCode.CONFLICT)

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
            assertEquals(LoginException.InvalidCredentials, result.exceptionOrNull())
        }
}