package com.felipearpa.tyche.session

interface AuthTokenRetriever {
    suspend fun authToken(): String?
}