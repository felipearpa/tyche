package com.felipearpa.tyche.account.bygoogle

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

internal class CredentialManagerGoogleCredentialProvider(
    private val webClientIdProvider: WebClientIdProvider,
) : GoogleCredentialProvider {
    override suspend fun getIdToken(activityContext: Context): Result<String> {
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientIdProvider())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(googleCredential.idToken)
            } else {
                Result.failure(GoogleSignInException.InvalidCredential)
            }
        } catch (exception: GetCredentialCancellationException) {
            Log.w(LOG_TAG, "Credential Manager returned cancellation (may mask a backend rejection)", exception)
            Result.failure(GoogleSignInException.Cancelled)
        } catch (exception: NoCredentialException) {
            Log.w(LOG_TAG, "No Google credential available", exception)
            Result.failure(GoogleSignInException.InvalidCredential)
        } catch (exception: Exception) {
            Log.e(LOG_TAG, "Unexpected Credential Manager failure", exception)
            Result.failure(exception)
        }
    }
}

private const val LOG_TAG = "GoogleCredential"
