package com.felipearpa.tyche.session.authentication.infrastructure

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class AuthenticationFirebaseDataSource @Inject constructor(private val firebaseAuth: FirebaseAuth) {
    suspend fun login(firebaseLoginRequest: FirebaseLoginRequest): String {
        val loginResult = firebaseAuth.signInWithEmailAndPassword(
            firebaseLoginRequest.email,
            firebaseLoginRequest.password
        ).await()
        val tokenResult = loginResult.user!!.getIdToken(true).await()
        return tokenResult.token!!
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}