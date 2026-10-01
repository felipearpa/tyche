package com.felipearpa.tyche

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.felipearpa.tyche.account.bygoogle.GoogleCredentialProvider
import com.felipearpa.tyche.account.bygoogle.GoogleSignInViewModel
import com.felipearpa.tyche.home.HomeScreen
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.authentication.application.SignInWithGoogle
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInException
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Lifecycle guard for the welcome screen's Google sign-in attempt.
 *
 * Google's account selection, account addition, consent, and verification screens put Fortuna in
 * the background. The attempt must survive that and a configuration recreation without a second
 * Google request, and must reach the existing app authentication with the token Google returns,
 * then navigate once. Destroying the screen is the only lifecycle event that cancels it.
 *
 * Google is replaced by a provider that waits until the test answers; the view model lives in the
 * activity's view model store, as on the real navigation entry, so recreation retains it.
 */
class HomeGoogleSignInAttemptTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val credentialProvider = ControlledCredentialProvider()
    private val signInWithGoogle = mockk<SignInWithGoogle>()
    private var authenticatedAccounts = emptyList<AccountBundle>()

    @Test
    fun backgroundingWhileGoogleIsOpenKeepsTheOriginalRequestThroughAppAuthentication() {
        coEvery { signInWithGoogle.execute(idToken = ID_TOKEN) } returns Result.success(accountBundle)
        showHome()

        googleButton().performClick()
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { credentialProvider.requestCount == 1 }
        sendFortunaToTheBackgroundAndBack()

        assertEquals(0, credentialProvider.canceledRequestCount)
        credentialProvider.respond(Result.success(ID_TOKEN))
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { authenticatedAccounts.isNotEmpty() }
        composeTestRule.waitForIdle()

        assertEquals(1, credentialProvider.requestCount)
        coVerify(exactly = 1) { signInWithGoogle.execute(idToken = ID_TOKEN) }
        assertEquals(listOf(accountBundle), authenticatedAccounts)
    }

    @Test
    fun anAttemptWaitingForGoogleDisablesEverySignInAction() {
        showHome()

        googleButton().performClick()
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { credentialProvider.requestCount == 1 }

        googleButton().assertIsNotEnabled()
        emailButton().assertIsNotEnabled()
        emailAndPasswordButton().assertIsNotEnabled()
        googleButton().performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, credentialProvider.requestCount)
    }

    @Test
    fun cancelingGoogleRestoresEverySignInAction() {
        showHome()
        googleButton().performClick()
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { credentialProvider.requestCount == 1 }

        credentialProvider.respond(Result.failure(GoogleSignInException.Cancelled))
        composeTestRule.waitForIdle()

        googleButton().assertIsEnabled()
        emailButton().assertIsEnabled()
        emailAndPasswordButton().assertIsEnabled()
        coVerify(exactly = 0) { signInWithGoogle.execute(idToken = any()) }
    }

    @Test
    fun recreatingTheActivityWhileGoogleIsOpenStartsNoSecondRequest() {
        coEvery { signInWithGoogle.execute(idToken = ID_TOKEN) } returns Result.success(accountBundle)
        showHome()
        googleButton().performClick()
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { credentialProvider.requestCount == 1 }

        recreateHome()
        googleButton().assertIsNotEnabled()
        credentialProvider.respond(Result.success(ID_TOKEN))
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { authenticatedAccounts.isNotEmpty() }
        composeTestRule.waitForIdle()

        assertEquals(1, credentialProvider.requestCount)
        assertEquals(0, credentialProvider.canceledRequestCount)
        assertEquals(listOf(accountBundle), authenticatedAccounts)
    }

    @Test
    fun recreatingTheActivityAfterSuccessDoesNotNavigateAgain() {
        coEvery { signInWithGoogle.execute(idToken = ID_TOKEN) } returns Result.success(accountBundle)
        showHome()
        googleButton().performClick()
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { credentialProvider.requestCount == 1 }
        credentialProvider.respond(Result.success(ID_TOKEN))
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { authenticatedAccounts.isNotEmpty() }

        recreateHome()

        assertEquals(listOf(accountBundle), authenticatedAccounts)
        googleButton().assertIsNotEnabled()
        coVerify(exactly = 1) { signInWithGoogle.execute(idToken = any()) }
    }

    @Test
    fun destroyingTheScreenCancelsTheRequestAndIgnoresALateToken() {
        showHome()
        googleButton().performClick()
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { credentialProvider.requestCount == 1 }

        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        credentialProvider.respond(Result.success(ID_TOKEN))

        assertEquals(1, credentialProvider.canceledRequestCount)
        coVerify(exactly = 0) { signInWithGoogle.execute(idToken = any()) }
        assertEquals(emptyList<AccountBundle>(), authenticatedAccounts)
    }

    private fun showHome() {
        composeTestRule.setContent { HomeContent(composeTestRule.activity) }
        composeTestRule.waitForIdle()
    }

    /**
     * Recreates the activity as a configuration change does and composes the screen again from
     * the new activity, which finds the retained view model in its store.
     */
    private fun recreateHome() {
        val scenario = composeTestRule.activityRule.scenario
        scenario.recreate()
        scenario.onActivity { activity -> activity.setContent { HomeContent(activity) } }
        composeTestRule.waitForIdle()
    }

    @Composable
    private fun HomeContent(activity: ComponentActivity) {
        TycheTheme {
            HomeScreen(
                googleViewModel = googleViewModel(activity),
                onSignInWithEmail = {},
                onSignInWithEmailAndPassword = {},
                onAuthenticate = { account -> authenticatedAccounts = authenticatedAccounts + account },
            )
        }
    }

    private fun googleViewModel(activity: ComponentActivity) =
        ViewModelProvider.create(
            owner = activity,
            factory = viewModelFactory {
                initializer {
                    GoogleSignInViewModel(
                        credentialProvider = credentialProvider,
                        signInWithGoogle = signInWithGoogle,
                    )
                }
            },
        )[GoogleSignInViewModel::class]

    /**
     * Stops the activity long enough for the process lifecycle to report the background, which it
     * does after a short delay once no activity is started, then resumes it.
     */
    private fun sendFortunaToTheBackgroundAndBack() {
        val scenario = composeTestRule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { processState() == Lifecycle.State.CREATED }
        scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.waitUntil(TIMEOUT_MILLIS) { processState() == Lifecycle.State.RESUMED }
    }

    private fun processState(): Lifecycle.State =
        composeTestRule.runOnUiThread { ProcessLifecycleOwner.get().lifecycle.currentState }

    // The Google button is the only sign-in action without a text label.
    private fun googleButton() =
        composeTestRule.onNode(hasClickAction() and SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))

    private fun emailButton() = composeTestRule.onNodeWithText(string(R.string.sign_in_with_email_action))

    private fun emailAndPasswordButton() =
        composeTestRule.onNodeWithText(string(R.string.sign_in_with_email_and_password))

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private companion object {
        const val ID_TOKEN = "google-id-token"
        const val TIMEOUT_MILLIS = 5_000L
        val accountBundle = AccountBundle(accountId = "account-id", externalAccountId = "external-id")
    }
}

/**
 * Stands in for Credential Manager: each request waits until the test answers it, and a request
 * whose coroutine is canceled while waiting is counted.
 */
private class ControlledCredentialProvider : GoogleCredentialProvider {
    @Volatile
    var requestCount = 0
        private set

    @Volatile
    var canceledRequestCount = 0
        private set

    @Volatile
    private var pendingResponse = CompletableDeferred<Result<String>>()

    fun respond(result: Result<String>) {
        pendingResponse.complete(result)
    }

    override suspend fun getIdToken(activityContext: Context): Result<String> {
        val response = CompletableDeferred<Result<String>>()
        pendingResponse = response
        requestCount += 1
        return try {
            response.await()
        } catch (exception: CancellationException) {
            canceledRequestCount += 1
            throw exception
        }
    }
}
