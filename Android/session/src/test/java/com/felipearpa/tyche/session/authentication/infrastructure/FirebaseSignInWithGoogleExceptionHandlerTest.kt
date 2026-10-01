package com.felipearpa.tyche.session.authentication.infrastructure

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FirebaseSignInWithGoogleExceptionHandlerTest {
    @Test
    fun `given Firebase succeeds then its value is returned`() = runTest {
        handleFirebaseSignInWithGoogle { "external-id" } shouldBeSuccess "external-id"
    }

    @Test
    fun `given another Firebase failure then that failure is returned`() = runTest {
        val failure = IllegalStateException("unexpected")

        handleFirebaseSignInWithGoogle<String> { throw failure }.exceptionOrNull() shouldBeSameInstanceAs failure
    }

    @Test
    fun `given the sign-in is canceled then the cancellation is rethrown instead of returned`() = runTest {
        val cancellation = CancellationException("sign-in abandoned")

        shouldThrow<CancellationException> {
            handleFirebaseSignInWithGoogle<String> { throw cancellation }
        } shouldBeSameInstanceAs cancellation
    }
}
