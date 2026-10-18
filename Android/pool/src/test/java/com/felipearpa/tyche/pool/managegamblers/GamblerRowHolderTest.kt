package com.felipearpa.tyche.pool.managegamblers

import com.felipearpa.tyche.data.pool.application.RemoveGambler
import com.felipearpa.ui.state.MutationState
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** Each gambler row shows the removal state checked here and reports its outcome to the screen. */
@OptIn(ExperimentalCoroutinesApi::class)
class GamblerRowHolderTest {

    private val outcomes = RecordingOutcomes()

    @Test
    fun `given a new row then it is idle`() = runTest(UnconfinedTestDispatcher()) {
        val holder = gamblerRowHolder(removeGambler = mockk(), scope = backgroundScope)

        holder.removal.value shouldBe MutationState.Idle(MEMBER)
        outcomes.reported.shouldBeEmpty()
    }

    @Test
    fun `given a removal in flight then the row is mutating and nothing is reported`() =
        runTest(UnconfinedTestDispatcher()) {
            val answer = CompletableDeferred<Result<Unit>>()
            val holder = gamblerRowHolder(removeGambler = removeGambler { answer.await() }, scope = backgroundScope)

            holder.remove()

            holder.removal.value.shouldBeInstanceOf<MutationState.Mutating<PoolMemberModel>>()
            outcomes.reported.shouldBeEmpty()
        }

    @Test
    fun `given a removal when the server confirms it then the row is mutated and reported removed`() =
        runTest(UnconfinedTestDispatcher()) {
            val holder = gamblerRowHolder(
                removeGambler = removeGambler { Result.success(Unit) },
                scope = backgroundScope,
            )

            holder.remove()

            holder.removal.value shouldBe MutationState.Mutated(original = MEMBER, updated = MEMBER)
            outcomes.reported shouldContainExactly listOf(Outcome.Removed)
        }

    @Test
    fun `given a removal when the request fails then the row fails and is reported failed, not removed`() =
        runTest(UnconfinedTestDispatcher()) {
            val holder = gamblerRowHolder(
                removeGambler = removeGambler { Result.failure(RuntimeException("offline")) },
                scope = backgroundScope,
            )

            holder.remove()

            holder.removal.value.shouldBeInstanceOf<MutationState.Failure<PoolMemberModel>>()
            outcomes.reported shouldContainExactly listOf(Outcome.Failed)
        }

    @Test
    fun `given a removal when the request fails then the failure is reported before the row shows it`() =
        runTest(UnconfinedTestDispatcher()) {
            val holder = gamblerRowHolder(
                removeGambler = removeGambler { Result.failure(RuntimeException("offline")) },
                scope = backgroundScope,
            )
            outcomes.observe = { holder.removal.value }

            holder.remove()

            outcomes.rowStatesWhenReported.single()
                .shouldBeInstanceOf<MutationState.Mutating<PoolMemberModel>>()
        }

    @Test
    fun `given a removal in flight when removal is requested again then no second request starts`() =
        runTest(UnconfinedTestDispatcher()) {
            val answer = CompletableDeferred<Result<Unit>>()
            val removeGambler = removeGambler { answer.await() }
            val holder = gamblerRowHolder(removeGambler = removeGambler, scope = backgroundScope)

            holder.remove()
            holder.remove()
            answer.complete(Result.success(Unit))

            coVerify(exactly = 1) { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) }
        }

    @Test
    fun `given a failed removal when retried then the failure is cleared while the new request runs`() =
        runTest(UnconfinedTestDispatcher()) {
            val retryAnswer = CompletableDeferred<Result<Unit>>()
            val removeGambler = mockk<RemoveGambler>()
            coEvery { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) } coAnswers {
                Result.failure(RuntimeException("offline"))
            } coAndThen { retryAnswer.await() }
            val holder = gamblerRowHolder(removeGambler = removeGambler, scope = backgroundScope)
            holder.remove()

            holder.retry()

            holder.removal.value.shouldBeInstanceOf<MutationState.Mutating<PoolMemberModel>>()
            outcomes.reported shouldContainExactly listOf(Outcome.Failed, Outcome.FailureCleared)

            retryAnswer.complete(Result.success(Unit))

            holder.removal.value shouldBe MutationState.Mutated(original = MEMBER, updated = MEMBER)
            outcomes.reported shouldContainExactly listOf(Outcome.Failed, Outcome.FailureCleared, Outcome.Removed)
            coVerify(exactly = 2) { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) }
        }

    @Test
    fun `given a failed removal when removal is requested again then the failure is cleared and a new request starts`() =
        runTest(UnconfinedTestDispatcher()) {
            val retryAnswer = CompletableDeferred<Result<Unit>>()
            val removeGambler = mockk<RemoveGambler>()
            coEvery { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) } coAnswers {
                Result.failure(RuntimeException("offline"))
            } coAndThen { retryAnswer.await() }
            val holder = gamblerRowHolder(removeGambler = removeGambler, scope = backgroundScope)
            holder.remove()

            holder.remove()

            holder.removal.value.shouldBeInstanceOf<MutationState.Mutating<PoolMemberModel>>()
            outcomes.reported shouldContainExactly listOf(Outcome.Failed, Outcome.FailureCleared)
            coVerify(exactly = 2) { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) }
        }

    @Test
    fun `given an idle row when retried then no request starts`() = runTest(UnconfinedTestDispatcher()) {
        val removeGambler = removeGambler { Result.success(Unit) }
        val holder = gamblerRowHolder(removeGambler = removeGambler, scope = backgroundScope)

        holder.retry()

        holder.removal.value shouldBe MutationState.Idle(MEMBER)
        outcomes.reported.shouldBeEmpty()
        coVerify(exactly = 0) { removeGambler.execute(poolId = any(), gamblerId = any()) }
    }

    @Test
    fun `given a failed removal when dismissed then the row is idle, the failure is cleared, and no request starts`() =
        runTest(UnconfinedTestDispatcher()) {
            val removeGambler = removeGambler { Result.failure(RuntimeException("offline")) }
            val holder = gamblerRowHolder(removeGambler = removeGambler, scope = backgroundScope)
            holder.remove()

            holder.dismissFailure()

            holder.removal.value shouldBe MutationState.Idle(MEMBER)
            outcomes.reported shouldContainExactly listOf(Outcome.Failed, Outcome.FailureCleared)
            coVerify(exactly = 1) { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) }
        }

    private fun removeGambler(answer: suspend () -> Result<Unit>): RemoveGambler {
        val removeGambler = mockk<RemoveGambler>()
        coEvery { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) } coAnswers { answer() }
        return removeGambler
    }

    private fun gamblerRowHolder(removeGambler: RemoveGambler, scope: CoroutineScope) =
        GamblerRowHolder(
            member = MEMBER,
            poolId = POOL_ID,
            removeGambler = removeGambler,
            scope = scope,
            outcomes = outcomes,
        )

    private enum class Outcome { Removed, Failed, FailureCleared }

    /** Records each reported outcome for [GAMBLER_ID] and, when [observe] is set, the row's state then. */
    private class RecordingOutcomes : GamblerRowOutcomes {
        var reported: List<Outcome> = emptyList()
            private set

        var rowStatesWhenReported: List<MutationState<PoolMemberModel>> = emptyList()
            private set

        var observe: (() -> MutationState<PoolMemberModel>)? = null

        override fun removed(gamblerId: String) = record(gamblerId, Outcome.Removed)

        override fun failed(gamblerId: String) = record(gamblerId, Outcome.Failed)

        override fun failureCleared(gamblerId: String) = record(gamblerId, Outcome.FailureCleared)

        private fun record(gamblerId: String, outcome: Outcome) {
            gamblerId shouldBe GAMBLER_ID
            reported = reported + outcome
            observe?.let { rowState -> rowStatesWhenReported = rowStatesWhenReported + rowState() }
        }
    }

    private companion object {
        const val POOL_ID = "pool"
        const val GAMBLER_ID = "santiago-id"
        val MEMBER = PoolMemberModel(
            gamblerId = GAMBLER_ID,
            gamblerUsername = "santiago",
            gamblerEmail = "santiago@example.com",
        )
    }
}
