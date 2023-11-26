package com.felipearpa.tyche.account.account.infrastructure

import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.data.account.login.domain.LoginResponse
import com.felipearpa.data.account.managment.domain.Account
import com.felipearpa.data.account.managment.domain.AccountRemoteDataSource
import com.felipearpa.data.account.managment.infrastructure.AccountRemoteRepository
import com.felipearpa.data.account.login.domain.LoginException
import com.felipearpa.data.account.type.Password
import com.felipearpa.data.account.type.Username
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountRemoteRepositoryTest {

    private val accountRemoteDataSource = mockk<AccountRemoteDataSource>()

    private val networkExceptionHandler = mockk<NetworkExceptionHandler>()

    private val accountRemoteRepository = AccountRemoteRepository(
        accountRemoteDataSource = accountRemoteDataSource,
        networkExceptionHandler = networkExceptionHandler
    )

    @Before
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `given a http exception with code 'bad request' when createUser is performed then an invalid credentials failure result is returned`() =
        runTest {
            val httpException =
                NetworkException.Http(httpStatusCode = HttpStatusCode.BAD_REQUEST)

            coEvery { networkExceptionHandler.handle<LoginResponse>(block = any()) } returns
                    Result.failure(
                        httpException
                    )

            val result = accountRemoteRepository.create(
                account = Account(
                    username = Username("tyche-user"),
                    password = Password("$1Pass1$")
                )
            )

            assertTrue(result.isFailure)
            assertEquals(LoginException.InvalidCredential, result.exceptionOrNull())
        }
}