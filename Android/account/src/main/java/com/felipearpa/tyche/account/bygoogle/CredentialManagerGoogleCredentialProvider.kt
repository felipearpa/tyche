package com.felipearpa.tyche.account.bygoogle

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.NoCredentialException
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Requests a Google ID token through the Google button's Credential Manager flow.
 *
 * Every outcome other than coroutine cancellation comes back as a [Result], including failures
 * to build the request. Coroutine cancellation (the owning screen was destroyed) is rethrown so
 * the caller's attempt ends silently instead of reporting a sign-in error. The gambler canceling
 * Google's screens is a different thing: it is a normal outcome, [GoogleSignInException.Cancelled].
 *
 * Failures that keep their original exception (unsupported or misconfigured provider, unknown
 * provider errors, request construction) are shown with the generic error.
 */
internal class CredentialManagerGoogleCredentialProvider(
    private val webClientIdProvider: WebClientIdProvider,
    private val createCredentialManager: (Context) -> CredentialManager = CredentialManager::create,
) : GoogleCredentialProvider {
    override suspend fun getIdToken(activityContext: Context): Result<String> =
        try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientIdProvider())
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val response = createCredentialManager(activityContext)
                .getCredential(activityContext, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.failure(GoogleSignInException.InvalidCredential)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: GetCredentialCancellationException) {
            Result.failure(GoogleSignInException.Cancelled)
        } catch (_: NoCredentialException) {
            Result.failure(GoogleSignInException.NoCredential)
        } catch (_: GetCredentialInterruptedException) {
            // Play services reports network errors and dropped connections as interruptions.
            Result.failure(GoogleSignInException.NetworkError)
        } catch (_: GoogleIdTokenParsingException) {
            Result.failure(GoogleSignInException.InvalidCredential)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
}
