package com.felipearpa.tyche.account.login.infrastructure

import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.session.authentication.domain.LoginResponse
import com.felipearpa.tyche.session.managment.domain.Account
import com.felipearpa.tyche.session.authentication.domain.LoginException
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRemoteDataSource
import com.felipearpa.tyche.session.authentication.infrastructure.AuthenticationRemoteRepository
import com.felipearpa.tyche.session.type.Password
import com.felipearpa.tyche.session.type.Username
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginRemoteRepositoryTest {

    private val authenticationRemoteDataSource = mockk<AuthenticationRemoteDataSource>()

    private val networkExceptionHandler = mockk<NetworkExceptionHandler>()

    private val loginRemoteRepository = AuthenticationRemoteRepository(
        authenticationRemoteDataSource = authenticationRemoteDataSource,
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
                    email = Username("tyche-user"),
                    password = Password("$1Pass1$")
                )
            )

            assertTrue(result.isFailure)
            assertEquals(LoginException.InvalidCredential, result.exceptionOrNull())
        }
}