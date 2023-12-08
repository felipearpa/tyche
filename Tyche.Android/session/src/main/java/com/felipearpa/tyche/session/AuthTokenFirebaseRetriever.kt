package com.felipearpa.tyche.session

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthTokenFirebaseRetriever @Inject constructor(private val firebaseAuth: FirebaseAuth) :
    AuthTokenRetriever {
    override suspend fun authToken(): String? {
        return firebaseAuth.currentUser?.getIdToken(true)?.await()?.token
    }
}