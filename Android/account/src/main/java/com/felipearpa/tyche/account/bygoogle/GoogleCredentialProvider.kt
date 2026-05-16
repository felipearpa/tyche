package com.felipearpa.tyche.account.bygoogle

import android.content.Context

interface GoogleCredentialProvider {
    suspend fun getIdToken(activityContext: Context): Result<String>
}
