package com.felipearpa.tyche.account.bygoogle

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.authentication.application.SignInWithGoogle
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInException
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Attempt ownership of [GoogleSignInViewModel]: busy from the first action, one request at a time,
 * one delivered success, cleared failures on a new attempt, and nothing published once the owning
 * screen is gone. Google's answer and the app authentication are both held open by the test so
 * each step is observed while the attempt is in flight.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSignInViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val credentialProvider = ControlledCredentialProvider()
    private val signInWithGoogle = mockk<SignInWithGoogle>()
    private val activityContext = mockk<Context>()
    private val viewModelStore = ViewModelStore()
    private lateinit var viewModel: GoogleSignInViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ViewModelProvider.create(
            store = viewModelStore,
            factory = viewModelFactory {
                initializer {
                    GoogleSignInViewModel(
                        credentialProvider = credentialProvider,
                        signInWithGoogle = signInWithGoogle,
                    )
                }
            },
        )[GoogleSignInViewModel::class]
    }

    @AfterEach
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `given an action then the attempt is busy before Google returns a credential`() = runTest(dispatcher) {
        viewModel.signInWithGoogle(activityContext = activityContext)

        viewModel.state.value shouldBe GoogleSignInState.InProgress
        credentialProvider.requestCount shouldBe 1
    }

    @Test
    fun `given an attempt waiting for Google then repeated actions keep the original request`() = runTest(dispatcher) {
        viewModel.signInWithGoogle(activityContext = activityContext)

        viewModel.signInWithGoogle(activityContext = activityContext)
        viewModel.signInWithGoogle(activityContext = activityContext)

        credentialProvider.requestCount shouldBe 1
        credentialProvider.canceledRequestCount shouldBe 0
        viewModel.state.value shouldBe GoogleSignInState.InProgress
    }

    @Test
    fun `given app authentication in flight then repeated actions start no competing attempt`() = runTest(dispatcher) {
        val authentication = holdAuthentication()
        viewModel.signInWithGoogle(activityContext = activityContext)
        credentialProvider.respond(Result.success(ID_TOKEN))

        viewModel.signInWithGoogle(activityContext = activityContext)

        credentialProvider.requestCount shouldBe 1
        coVerify(exactly = 1) { signInWithGoogle.execute(idToken = ID_TOKEN) }
        viewModel.state.value shouldBe GoogleSignInState.InProgress
        authentication.complete(Result.success(accountBundle))
        viewModel.state.value shouldBe GoogleSignInState.Authenticated(accountBundle)
    }

    @Test
    fun `given app authentication succeeds then the account is delivered once`() = runTest(dispatcher) {
        coEvery { signInWithGoogle.execute(idToken = ID_TOKEN) } returns Result.success(accountBundle)
        viewModel.signInWithGoogle(activityContext = activityContext)
        credentialProvider.respond(Result.success(ID_TOKEN))

        viewModel.state.value shouldBe GoogleSignInState.Authenticated(accountBundle)
        viewModel.authenticationHandled()

        viewModel.state.value shouldBe GoogleSignInState.Completed
        viewModel.authenticationHandled()
        viewModel.signInWithGoogle(activityContext = activityContext)
        viewModel.state.value shouldBe GoogleSignInState.Completed
        credentialProvider.requestCount shouldBe 1
        coVerify(exactly = 1) { signInWithGoogle.execute(idToken = ID_TOKEN) }
    }

    @Test
    fun `given the gambler cancels Google then the attempt returns to idle without a failure`() = runTest(dispatcher) {
        viewModel.signInWithGoogle(activityContext = activityContext)

        credentialProvider.respond(Result.failure(GoogleSignInException.Cancelled))

        viewModel.state.value shouldBe GoogleSignInState.Idle
        credentialProvider.requestCount shouldBe 1
        coVerify(exactly = 0) { signInWithGoogle.execute(idToken = any()) }
    }

    @Test
    fun `given Google fails then the attempt ends in a failure that is not busy`() = runTest(dispatcher) {
        viewModel.signInWithGoogle(activityContext = activityContext)

        credentialProvider.respond(Result.failure(GoogleSignInException.NetworkError))

        val state = viewModel.state.value.shouldBeInstanceOf<GoogleSignInState.Failed>()
        state.exception shouldBe GoogleSignInLocalizedException.NetworkError
        state.isBusy shouldBe false
        credentialProvider.requestCount shouldBe 1
    }

    @Test
    fun `given app authentication fails then the attempt ends in a failure without delivering an account`() =
        runTest(dispatcher) {
            coEvery { signInWithGoogle.execute(idToken = ID_TOKEN) } returns
                Result.failure(GoogleSignInException.AccountExistsWithDifferentCredential)
            viewModel.signInWithGoogle(activityContext = activityContext)

            credentialProvider.respond(Result.success(ID_TOKEN))

            viewModel.state.value shouldBe
                GoogleSignInState.Failed(GoogleSignInLocalizedException.AccountExistsWithDifferentCredential)
        }

    @Test
    fun `given an earlier failure then a new action clears it and starts one attempt`() = runTest(dispatcher) {
        viewModel.signInWithGoogle(activityContext = activityContext)
        credentialProvider.respond(Result.failure(GoogleSignInException.NetworkError))

        viewModel.signInWithGoogle(activityContext = activityContext)

        viewModel.state.value shouldBe GoogleSignInState.InProgress
        credentialProvider.requestCount shouldBe 2
    }

    @Test
    fun `given a failure is dismissed then the attempt is idle`() = runTest(dispatcher) {
        viewModel.signInWithGoogle(activityContext = activityContext)
        credentialProvider.respond(Result.failure(GoogleSignInException.NetworkError))

        viewModel.dismissFailure()

        viewModel.state.value shouldBe GoogleSignInState.Idle
    }

    @Test
    fun `given an attempt in flight then dismissing a failure does not release it`() = runTest(dispatcher) {
        viewModel.signInWithGoogle(activityContext = activityContext)

        viewModel.dismissFailure()
        viewModel.signInWithGoogle(activityContext = activityContext)

        viewModel.state.value shouldBe GoogleSignInState.InProgress
        credentialProvider.requestCount shouldBe 1
    }

    @Test
    fun `given the screen is destroyed while Google is open then the request is canceled and a late token is ignored`() =
        runTest(dispatcher) {
            viewModel.signInWithGoogle(activityContext = activityContext)

            viewModelStore.clear()
            credentialProvider.respond(Result.success(ID_TOKEN))

            credentialProvider.canceledRequestCount shouldBe 1
            viewModel.state.value shouldBe GoogleSignInState.InProgress
            coVerify(exactly = 0) { signInWithGoogle.execute(idToken = any()) }
        }

    @Test
    fun `given the screen is destroyed during app authentication then a late result is not published`() =
        runTest(dispatcher) {
            val authentication = holdAuthentication()
            viewModel.signInWithGoogle(activityContext = activityContext)
            credentialProvider.respond(Result.success(ID_TOKEN))

            viewModelStore.clear()
            authentication.complete(Result.success(accountBundle))

            viewModel.state.value shouldBe GoogleSignInState.InProgress
        }

    @Test
    fun `given a provider reports cancellation as a failure after the screen is destroyed then no failure is published`() =
        runTest(dispatcher) {
            credentialProvider.reportsCancellationAsFailure = true
            viewModel.signInWithGoogle(activityContext = activityContext)

            viewModelStore.clear()

            credentialProvider.canceledRequestCount shouldBe 1
            viewModel.state.value shouldBe GoogleSignInState.InProgress
        }

    @Test
    fun `given Google returns no credential then the failure is the missing credential, not a malformed one`() =
        runTest(dispatcher) {
            viewModel.signInWithGoogle(activityContext = activityContext)

            credentialProvider.respond(Result.failure(GoogleSignInException.NoCredential))

            viewModel.state.value shouldBe GoogleSignInState.Failed(GoogleSignInLocalizedException.NoCredential)
            credentialProvider.requestCount shouldBe 1
            coVerify(exactly = 0) { signInWithGoogle.execute(idToken = any()) }
        }

    @Test
    fun `given the Google provider fails for another reason then the generic failure is shown without a retry`() =
        runTest(dispatcher) {
            viewModel.signInWithGoogle(activityContext = activityContext)

            credentialProvider.respond(Result.failure(IllegalStateException("provider unavailable")))

            val state = viewModel.state.value.shouldBeInstanceOf<GoogleSignInState.Failed>()
            state.exception.shouldBeInstanceOf<UnknownLocalizedException>()
            credentialProvider.requestCount shouldBe 1
            coVerify(exactly = 0) { signInWithGoogle.execute(idToken = any()) }
        }

    @Test
    fun `given the Google provider throws then the attempt ends in a failure that is not busy`() =
        runTest(dispatcher) {
            viewModel.signInWithGoogle(activityContext = activityContext)

            credentialProvider.fail(IllegalStateException("provider crashed"))

            val state = viewModel.state.value.shouldBeInstanceOf<GoogleSignInState.Failed>()
            state.isBusy shouldBe false
            coVerify(exactly = 0) { signInWithGoogle.execute(idToken = any()) }
        }

    @Test
    fun `given Firebase reports a failure then the attempt ends in that failure`() = runTest(dispatcher) {
        coEvery { signInWithGoogle.execute(idToken = ID_TOKEN) } returns
            Result.failure(GoogleSignInException.NetworkError)
        viewModel.signInWithGoogle(activityContext = activityContext)

        credentialProvider.respond(Result.success(ID_TOKEN))

        viewModel.state.value shouldBe GoogleSignInState.Failed(GoogleSignInLocalizedException.NetworkError)
    }

    @Test
    fun `given the account link reports a failure then the attempt ends in a failure that is not busy`() =
        runTest(dispatcher) {
            coEvery { signInWithGoogle.execute(idToken = ID_TOKEN) } returns
                Result.failure(IllegalStateException("account link rejected"))
            viewModel.signInWithGoogle(activityContext = activityContext)

            credentialProvider.respond(Result.success(ID_TOKEN))

            val state = viewModel.state.value.shouldBeInstanceOf<GoogleSignInState.Failed>()
            state.exception.shouldBeInstanceOf<UnknownLocalizedException>()
            state.isBusy shouldBe false
        }

    @Test
    fun `given account installation throws then the attempt ends in a failure without delivering an account`() =
        runTest(dispatcher) {
            coEvery { signInWithGoogle.execute(idToken = ID_TOKEN) } throws
                IllegalStateException("account storage unavailable")
            viewModel.signInWithGoogle(activityContext = activityContext)

            credentialProvider.respond(Result.success(ID_TOKEN))

            val state = viewModel.state.value.shouldBeInstanceOf<GoogleSignInState.Failed>()
            state.exception.shouldBeInstanceOf<UnknownLocalizedException>()
            state.isBusy shouldBe false
            coVerify(exactly = 1) { signInWithGoogle.execute(idToken = ID_TOKEN) }
        }

    @Test
    fun `given app authentication throws a foreign cancellation while the attempt is active then it ends in a failure`() =
        runTest(dispatcher) {
            coEvery { signInWithGoogle.execute(idToken = ID_TOKEN) } throws
                CancellationException("Firebase task canceled")
            viewModel.signInWithGoogle(activityContext = activityContext)

            credentialProvider.respond(Result.success(ID_TOKEN))

            viewModel.state.value.shouldBeInstanceOf<GoogleSignInState.Failed>().isBusy shouldBe false
        }

    @Test
    fun `given app authentication failed then a new action authenticates the new token once and succeeds`() =
        runTest(dispatcher) {
            coEvery { signInWithGoogle.execute(idToken = ID_TOKEN) } throws
                IllegalStateException("account storage unavailable")
            viewModel.signInWithGoogle(activityContext = activityContext)
            credentialProvider.respond(Result.success(ID_TOKEN))
            coEvery { signInWithGoogle.execute(idToken = RETRY_ID_TOKEN) } returns Result.success(accountBundle)

            viewModel.signInWithGoogle(activityContext = activityContext)

            viewModel.state.value shouldBe GoogleSignInState.InProgress
            credentialProvider.respond(Result.success(RETRY_ID_TOKEN))
            viewModel.state.value shouldBe GoogleSignInState.Authenticated(accountBundle)
            credentialProvider.requestCount shouldBe 2
            coVerify(exactly = 1) { signInWithGoogle.execute(idToken = ID_TOKEN) }
            coVerify(exactly = 1) { signInWithGoogle.execute(idToken = RETRY_ID_TOKEN) }
        }

    private fun holdAuthentication(): CompletableDeferred<Result<AccountBundle>> {
        val authentication = CompletableDeferred<Result<AccountBundle>>()
        coEvery { signInWithGoogle.execute(idToken = ID_TOKEN) } coAnswers { authentication.await() }
        return authentication
    }

    private companion object {
        const val ID_TOKEN = "google-id-token"
        const val RETRY_ID_TOKEN = "google-id-token-retry"
        val accountBundle = AccountBundle(accountId = "account-id", externalAccountId = "external-id")
    }
}

/**
 * Stands in for Credential Manager: each request waits until the test answers it, and a request
 * whose coroutine is canceled while waiting is counted.
 */
private class ControlledCredentialProvider : GoogleCredentialProvider {
    var requestCount = 0
        private set
    var canceledRequestCount = 0
        private set

    /** Mimics a provider whose generic catch turns coroutine cancellation into a failure. */
    var reportsCancellationAsFailure = false

    private var pendingResponse = CompletableDeferred<Result<String>>()

    fun respond(result: Result<String>) {
        pendingResponse.complete(result)
    }

    /** Ends the waiting request by throwing instead of returning a result. */
    fun fail(exception: Throwable) {
        pendingResponse.completeExceptionally(exception)
    }

    override suspend fun getIdToken(activityContext: Context): Result<String> {
        requestCount += 1
        pendingResponse = CompletableDeferred()
        return try {
            pendingResponse.await()
        } catch (exception: CancellationException) {
            canceledRequestCount += 1
            if (reportsCancellationAsFailure) Result.failure(exception) else throw exception
        }
    }
}
