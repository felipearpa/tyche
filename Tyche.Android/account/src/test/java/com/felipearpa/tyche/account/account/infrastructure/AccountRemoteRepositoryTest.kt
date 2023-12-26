package com.felipearpa.tyche.account.account.infrastructure

import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.session.authentication.domain.LinkAccountResponse
import com.felipearpa.tyche.session.managment.domain.Account
import com.felipearpa.tyche.session.managment.domain.AccountRemoteDataSource
import com.felipearpa.tyche.session.managment.infrastructure.AccountRemoteRepository
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

            coEvery { networkExceptionHandler.handle<LinkAccountResponse>(block = any()) } returns
                    Result.failure(
                        httpException
                    )

            val result = accountRemoteRepository.create(
                account = Account(
                    email = Username("tyche-user"),
                    password = Password("$1Pass1$")
                )
            )

            assertTrue(result.isFailure)
            assertEquals(LoginException.InvalidCredential, result.exceptionOrNull())
        }
}