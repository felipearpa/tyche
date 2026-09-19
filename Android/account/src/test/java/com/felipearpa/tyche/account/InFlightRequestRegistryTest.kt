package com.felipearpa.tyche.account

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InFlightRequestRegistryTest {

    @Test
    fun `given concurrent callers for the same key then one operation runs and every caller receives its result`() =
        runTest {
            val registry = InFlightRequestRegistry<String>(
                scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            )
            val gate = CompletableDeferred<Unit>()
            var operationCount = 0
            val operation: suspend () -> String = {
                operationCount += 1
                gate.await()
                "result"
            }

            val first = async { registry.runCoalesced(key = "avatar#a#0#64", operation = operation) }
            val second = async { registry.runCoalesced(key = "avatar#a#0#64", operation = operation) }
            runCurrent()

            gate.complete(Unit)

            first.await() shouldBe "result"
            second.await() shouldBe "result"
            operationCount shouldBe 1
        }

    @Test
    fun `given different keys then their operations run independently`() = runTest {
        val registry = InFlightRequestRegistry<String>(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        var operationCount = 0

        val first = async {
            registry.runCoalesced(key = "avatar#a#0#64") {
                operationCount += 1
                "a"
            }
        }
        val second = async {
            registry.runCoalesced(key = "avatar#b#0#64") {
                operationCount += 1
                "b"
            }
        }

        first.await() shouldBe "a"
        second.await() shouldBe "b"
        operationCount shouldBe 2
    }

    @Test
    fun `given a completed operation then a later call for the same key runs a new operation`() =
        runTest {
            val registry = InFlightRequestRegistry<Int>(
                scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            )
            var operationCount = 0
            val operation: suspend () -> Int = {
                operationCount += 1
                operationCount
            }

            registry.runCoalesced(key = "avatar#a#0#64", operation = operation) shouldBe 1
            advanceUntilIdle()
            registry.runCoalesced(key = "avatar#a#0#64", operation = operation) shouldBe 2

            operationCount shouldBe 2
        }
}
