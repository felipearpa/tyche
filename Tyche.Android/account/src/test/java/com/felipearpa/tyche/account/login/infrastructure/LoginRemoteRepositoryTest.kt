package com.felipearpa.tyche.account.login.infrastructure

import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.session.login.domain.LoginResponse
import com.felipearpa.session.managment.domain.Account
import com.felipearpa.session.login.domain.LoginException
import com.felipearpa.session.login.domain.LoginRemoteDataSource
import com.felipearpa.session.login.infrastructure.LoginRemoteRepository
import com.felipearpa.session.type.Password
import com.felipearpa.session.type.Username
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
                user = Account(
                    username = Username("tyche-user"),
                    password = Password("$1Pass1$")
                )
            )

            assertTrue(result.isFailure)
            assertEquals(LoginException.InvalidCredential, result.exceptionOrNull())
        }
}