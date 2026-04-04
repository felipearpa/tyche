package com.felipearpa.tyche.session

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthTokenFirebaseRetriever(private val firebaseAuth: FirebaseAuth) :
    AuthTokenRetriever {
    override suspend fun authToken(): String? {
        return firebaseAuth.currentUser?.getIdToken(true)?.await()?.token
    }
}
