package com.felipearpa.tyche.session.managment.infrastructure

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class AccountFirebaseDataSource @Inject constructor(private val firebaseAuth: FirebaseAuth) {
    suspend fun create(firebaseAccountCreationRequest: FirebaseAccountCreationRequest): String {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(
            firebaseAccountCreationRequest.email,
            firebaseAccountCreationRequest.password
        ).await()
        val tokenResult = authResult.user!!.getIdToken(true).await()
        return tokenResult.token!!
    }
}