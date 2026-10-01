package com.felipearpa.tyche.session.authentication.application

import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.CurrentAccountCoordinator
import com.felipearpa.tyche.session.CurrentAccountSnapshot
import com.felipearpa.tyche.session.authentication.domain.AccountLink
import com.felipearpa.tyche.session.authentication.domain.AuthenticationRepository
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInException
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * The Google sign-in pipeline: one Firebase sign-in with the returned token, the existing account
 * link, then installation of the current account. Returned failures stop the pipeline at their
 * step, an installation failure is thrown, and cancellation of the caller is never returned.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignInWithGoogleTest {
    private val repository = mockk<AuthenticationRepository>()

    @Test
    fun `given Firebase and the account link succeed then the linked account is installed and returned`() =
        runTest {
            val fixture = fixture()
            coEvery { repository.signInWithGoogle(idToken = ID_TOKEN) } returns Result.success(googleResult)
            coEvery { repository.linkAccount(accountLink = accountLink) } returns Result.success(accountBundle)

            fixture.useCase.execute(idToken = ID_TOKEN) shouldBeSuccess accountBundle

            coVerify(exactly = 1) { repository.signInWithGoogle(idToken = ID_TOKEN) }
            coVerify(exactly = 1) { repository.linkAccount(accountLink = accountLink) }
            fixture.coordinator.state.value shouldBe accountBundle
            fixture.storage.stored?.account shouldBe accountBundle
        }

    @Test
    fun `given Firebase fails then the failure is returned and no account is linked`() = runTest {
        val fixture = fixture()
        coEvery { repository.signInWithGoogle(idToken = ID_TOKEN) } returns
            Result.failure(GoogleSignInException.AccountExistsWithDifferentCredential)

        fixture.useCase.execute(idToken = ID_TOKEN) shouldBeFailure
            GoogleSignInException.AccountExistsWithDifferentCredential

        coVerify(exactly = 0) { repository.linkAccount(accountLink = any()) }
        fixture.coordinator.state.value.shouldBeNull()
    }

    @Test
    fun `given the account link fails then the failure is returned and no account is installed`() = runTest {
        val fixture = fixture()
        val failure = IllegalStateException("account link rejected")
        coEvery { repository.signInWithGoogle(idToken = ID_TOKEN) } returns Result.success(googleResult)
        coEvery { repository.linkAccount(accountLink = accountLink) } returns Result.failure(failure)

        fixture.useCase.execute(idToken = ID_TOKEN).exceptionOrNull() shouldBeSameInstanceAs failure

        fixture.coordinator.state.value.shouldBeNull()
        fixture.storage.stored.shouldBeNull()
    }

    @Test
    fun `given account installation fails then the storage failure is thrown`() = runTest {
        val failure = IllegalStateException("account storage unavailable")
        val fixture = fixture(storageFailure = failure)
        coEvery { repository.signInWithGoogle(idToken = ID_TOKEN) } returns Result.success(googleResult)
        coEvery { repository.linkAccount(accountLink = accountLink) } returns Result.success(accountBundle)

        // Coroutine stack-trace recovery may rethrow a copy, so the failure is matched by message.
        shouldThrow<IllegalStateException> { fixture.useCase.execute(idToken = ID_TOKEN) }
            .message shouldBe failure.message
    }

    @Test
    fun `given the caller is canceled during the account link then the cancellation is not returned as a failure`() =
        runTest {
            val fixture = fixture()
            coEvery { repository.signInWithGoogle(idToken = ID_TOKEN) } returns Result.success(googleResult)
            // The shared network handler reports the request's cancellation as a returned failure.
            coEvery { repository.linkAccount(accountLink = accountLink) } coAnswers {
                runCatching { awaitCancellation() }
            }
            val attempt = async { fixture.useCase.execute(idToken = ID_TOKEN) }
            testScheduler.runCurrent()

            attempt.cancel()
            testScheduler.runCurrent()

            attempt.isCancelled shouldBe true
            shouldThrow<CancellationException> { attempt.await() }
            fixture.coordinator.state.value.shouldBeNull()
        }

    @Test
    fun `given the account link returns a cancellation while the caller is active then it is returned as a failure`() =
        runTest {
            val fixture = fixture()
            val failure = CancellationException("request canceled by another owner")
            coEvery { repository.signInWithGoogle(idToken = ID_TOKEN) } returns Result.success(googleResult)
            coEvery { repository.linkAccount(accountLink = accountLink) } returns Result.failure(failure)

            fixture.useCase.execute(idToken = ID_TOKEN).exceptionOrNull() shouldBeSameInstanceAs failure
        }

    private fun TestScope.fixture(storageFailure: Throwable? = null): Fixture {
        val storage = RecordingAccountStorage(failure = storageFailure)
        val coordinator = CurrentAccountCoordinator(
            accountStorage = storage,
            authenticationRepository = repository,
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            nowEpochMillis = { 0L },
        )
        return Fixture(
            storage = storage,
            coordinator = coordinator,
            useCase = SignInWithGoogle(
                authenticationRepository = repository,
                currentAccountCoordinator = coordinator,
            ),
        )
    }

    private class Fixture(
        val storage: RecordingAccountStorage,
        val coordinator: CurrentAccountCoordinator,
        val useCase: SignInWithGoogle,
    )

    private companion object {
        const val ID_TOKEN = "google-id-token"
        const val EMAIL = "gambler@fortuna.test"
        val googleResult = GoogleSignInResult(externalAccountId = "external-id", email = EMAIL)
        val accountLink = AccountLink(email = Email(EMAIL), externalAccountId = "external-id")
        val accountBundle = AccountBundle(accountId = "account-id", externalAccountId = "external-id", email = EMAIL)
    }
}

private class RecordingAccountStorage(private val failure: Throwable?) : AccountStorage {
    var stored: CurrentAccountSnapshot? = null
        private set

    override suspend fun store(snapshot: CurrentAccountSnapshot) {
        failure?.let { throw it }
        stored = snapshot
    }

    override suspend fun delete() {
        stored = null
    }

    override suspend fun retrieve(): CurrentAccountSnapshot? = stored
}
