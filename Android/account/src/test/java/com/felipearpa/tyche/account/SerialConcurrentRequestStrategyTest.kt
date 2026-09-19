package com.felipearpa.tyche.account

import coil3.annotation.ExperimentalCoilApi
import coil3.fetch.FetchResult
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoilApi::class, ExperimentalCoroutinesApi::class)
class SerialConcurrentRequestStrategyTest {
    private val fetchResult = mockk<FetchResult>()

    @Test
    fun `given three concurrent fetches of one key then they never overlap`() = runTest {
        val strategy = SerialConcurrentRequestStrategy()
        (1..3).map {
            async {
                strategy.apply(key = "avatar-url") {
                    running += 1
                    val overlapped = running > 1
                    yield()
                    running -= 1
                    peakOverlap = peakOverlap || overlapped
                    fetchResult
                }
            }
        }.awaitAll()

        peakOverlap shouldBe false
    }

    @Test
    fun `given fetches of different keys then one does not wait for the other`() = runTest {
        val strategy = SerialConcurrentRequestStrategy()
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()

        val first = async {
            strategy.apply(key = "url-1") {
                firstStarted.complete(Unit)
                releaseFirst.await()
                fetchResult
            }
        }
        firstStarted.await()

        strategy.apply(key = "url-2") { fetchResult } shouldBe fetchResult

        releaseFirst.complete(Unit)
        first.await() shouldBe fetchResult
    }

    @Test
    fun `given a fetch cancelled while it holds the key then the key is released`() = runTest {
        val strategy = SerialConcurrentRequestStrategy()
        val started = CompletableDeferred<Unit>()
        val holder = launch {
            strategy.apply(key = "avatar-url") {
                started.complete(Unit)
                CompletableDeferred<Unit>().await()
                fetchResult
            }
        }
        started.await()

        holder.cancelAndJoin()
        advanceUntilIdle()

        strategy.apply(key = "avatar-url") { fetchResult } shouldBe fetchResult
    }

    private var running = 0
    private var peakOverlap = false
}
