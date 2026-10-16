package com.felipearpa.tyche.session

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthTokenFirebaseRetriever(private val firebaseAuth: FirebaseAuth) :
    AuthTokenRetriever {
    override suspend fun authToken(forceRefresh: Boolean): String? {
        return firebaseAuth.currentUser?.getIdToken(forceRefresh)?.await()?.token
    }
}
