package com.felipearpa.tyche.account.bygoogle

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * Classification of Credential Manager outcomes for the Google button: the token on success, a
 * silent outcome for the gambler canceling, distinct missing and malformed credentials, failures
 * returned rather than thrown, and coroutine cancellation passed through. Each call makes exactly
 * one request; nothing is retried.
 */
class CredentialManagerGoogleCredentialProviderTest {
    private val activityContext = mockk<Context>()
    private val credentialManager = mockk<CredentialManager>()
    private val provider = CredentialManagerGoogleCredentialProvider(
        webClientIdProvider = { WEB_CLIENT_ID },
        createCredentialManager = { credentialManager },
    )

    @Test
    fun `given Google returns an ID token credential then its token is returned from one Google-button request`() =
        runTest {
            val request = respondWith(googleIdTokenCredential())

            provider.getIdToken(activityContext = activityContext) shouldBeSuccess ID_TOKEN

            verifyOneRequest()
            val option = request.captured.credentialOptions.single()
                .shouldBeInstanceOf<GetSignInWithGoogleOption>()
            option.serverClientId shouldBe WEB_CLIENT_ID
        }

    @Test
    fun `given the gambler cancels Google then the outcome is the silent cancellation`() = runTest {
        failWith(GetCredentialCancellationException())

        provider.getIdToken(activityContext = activityContext) shouldBeFailure GoogleSignInException.Cancelled
        verifyOneRequest()
    }

    @Test
    fun `given Google returns no credential then the outcome is a missing credential, not a malformed one`() =
        runTest {
            failWith(NoCredentialException())

            provider.getIdToken(activityContext = activityContext) shouldBeFailure GoogleSignInException.NoCredential
            verifyOneRequest()
        }

    @Test
    fun `given the request is interrupted then the outcome is a network failure without a retry`() = runTest {
        failWith(GetCredentialInterruptedException())

        provider.getIdToken(activityContext = activityContext) shouldBeFailure GoogleSignInException.NetworkError
        verifyOneRequest()
    }

    @Test
    fun `given Google services are unsupported then the provider failure is returned without a retry`() = runTest {
        val failure = GetCredentialUnsupportedException()
        failWith(failure)

        provider.getIdToken(activityContext = activityContext).exceptionOrNull() shouldBeSameInstanceAs failure
        verifyOneRequest()
    }

    @Test
    fun `given the provider is misconfigured then the provider failure is returned without a retry`() = runTest {
        val failure = GetCredentialProviderConfigurationException()
        failWith(failure)

        provider.getIdToken(activityContext = activityContext).exceptionOrNull() shouldBeSameInstanceAs failure
        verifyOneRequest()
    }

    @Test
    fun `given the provider fails for an unknown reason then that failure is returned without a retry`() =
        runTest {
            val failure = GetCredentialUnknownException()
            failWith(failure)

            provider.getIdToken(activityContext = activityContext).exceptionOrNull() shouldBeSameInstanceAs failure
            verifyOneRequest()
        }

    @Test
    fun `given Google returns another credential type then the credential is invalid`() = runTest {
        respondWith(PasswordCredential(id = "gambler", password = "password"))

        provider.getIdToken(activityContext = activityContext) shouldBeFailure GoogleSignInException.InvalidCredential
    }

    @Test
    fun `given Google returns an ID token credential that cannot be parsed then the credential is invalid`() =
        runTest {
            respondWith(
                CustomCredential(
                    type = GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
                    data = Bundle(),
                ),
            )

            provider.getIdToken(activityContext = activityContext) shouldBeFailure GoogleSignInException.InvalidCredential
        }

    @Test
    fun `given the web client ID cannot be read then the failure is returned and Google is not asked`() = runTest {
        val failure = IllegalStateException("missing web client ID")
        val provider = CredentialManagerGoogleCredentialProvider(
            webClientIdProvider = { throw failure },
            createCredentialManager = { credentialManager },
        )

        provider.getIdToken(activityContext = activityContext).exceptionOrNull() shouldBeSameInstanceAs failure
        coVerify(exactly = 0) { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) }
    }

    @Test
    fun `given an empty web client ID then the request cannot be built and Google is not asked`() = runTest {
        val provider = CredentialManagerGoogleCredentialProvider(
            webClientIdProvider = { "" },
            createCredentialManager = { credentialManager },
        )

        provider.getIdToken(activityContext = activityContext)
            .shouldBeFailure<IllegalArgumentException>()
        coVerify(exactly = 0) { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) }
    }

    @Test
    fun `given the calling coroutine is canceled while Google is open then the cancellation is rethrown`() =
        runTest {
            val cancellation = CancellationException("sign-in screen destroyed")
            failWith(cancellation)

            shouldThrow<CancellationException> {
                provider.getIdToken(activityContext = activityContext)
            } shouldBeSameInstanceAs cancellation
        }

    private fun respondWith(credential: Credential): CapturingSlot<GetCredentialRequest> {
        val request = slot<GetCredentialRequest>()
        coEvery {
            credentialManager.getCredential(activityContext, capture(request))
        } returns GetCredentialResponse(credential)
        return request
    }

    private fun failWith(failure: Throwable) {
        coEvery {
            credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
        } throws failure
    }

    private fun verifyOneRequest() {
        coVerify(exactly = 1) { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) }
    }

    /** A Google ID token credential as Credential Manager returns it: a typed custom credential. */
    private fun googleIdTokenCredential(): CustomCredential {
        val data = mockk<Bundle> {
            every { getString(any()) } returns null
            every { getString("$GOOGLE_ID_KEY_PREFIX.BUNDLE_KEY_ID") } returns "gambler@fortuna.test"
            every { getString("$GOOGLE_ID_KEY_PREFIX.BUNDLE_KEY_ID_TOKEN") } returns ID_TOKEN
            every { getParcelable<Uri>(any()) } returns null
            every { getParcelable(any(), Uri::class.java) } returns null
        }
        return CustomCredential(type = GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, data = data)
    }

    private companion object {
        const val WEB_CLIENT_ID = "web-client-id.apps.googleusercontent.com"
        const val ID_TOKEN = "google-id-token"
        const val GOOGLE_ID_KEY_PREFIX = "com.google.android.libraries.identity.googleid"
    }
}
