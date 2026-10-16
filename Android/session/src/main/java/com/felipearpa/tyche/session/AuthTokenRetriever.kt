package com.felipearpa.tyche.session

interface AuthTokenRetriever {
    /**
     * Returns the signed-in user's ID token, or null when no user is signed in.
     *
     * With [forceRefresh] false, the provider's current token is returned (the provider renews it
     * before it expires). With [forceRefresh] true, a new token is fetched even if the current one
     * has not expired.
     */
    suspend fun authToken(forceRefresh: Boolean): String?
}
