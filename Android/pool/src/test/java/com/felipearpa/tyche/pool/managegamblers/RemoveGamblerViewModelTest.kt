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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Each gambler row shows the removal state checked here and reports its outcome to the screen. */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoveGamblerViewModelTest {

    private val registry = RecordingRegistry()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given a new row then it is idle`() {
        val viewModel = removeGamblerViewModel(removeGambler = mockk())

        viewModel.state.value shouldBe MutationState.Idle(MEMBER)
    }

    @Test
    fun `given a removal in flight then the row is mutating and nothing is reported`() {
        val answer = CompletableDeferred<Result<Unit>>()
        val viewModel = removeGamblerViewModel(removeGambler = removeGambler { answer.await() })

        viewModel.remove()

        viewModel.state.value.shouldBeInstanceOf<MutationState.Mutating<PoolMemberModel>>()
        registry.removedGamblerIds.shouldBeEmpty()
        registry.failedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given a removal when the server confirms it then the row is mutated and reported removed`() {
        val viewModel = removeGamblerViewModel(removeGambler = removeGambler { Result.success(Unit) })

        viewModel.remove()

        viewModel.state.value shouldBe MutationState.Mutated(original = MEMBER, updated = MEMBER)
        registry.removedGamblerIds shouldContainExactly setOf(GAMBLER_ID)
        registry.failedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given a removal when the request fails then the row fails and is reported failed, not removed`() {
        val viewModel = removeGamblerViewModel(
            removeGambler = removeGambler { Result.failure(RuntimeException("offline")) },
        )

        viewModel.remove()

        viewModel.state.value.shouldBeInstanceOf<MutationState.Failure<PoolMemberModel>>()
        registry.failedGamblerIds.value shouldContainExactly setOf(GAMBLER_ID)
        registry.removedGamblerIds.shouldBeEmpty()
    }

    @Test
    fun `given a removal in flight when removal is requested again then no second request starts`() {
        val answer = CompletableDeferred<Result<Unit>>()
        val removeGambler = removeGambler { answer.await() }
        val viewModel = removeGamblerViewModel(removeGambler = removeGambler)

        viewModel.remove()
        viewModel.remove()
        answer.complete(Result.success(Unit))

        coVerify(exactly = 1) { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) }
    }

    @Test
    fun `given a failed removal when retried then the failure is cleared while the new request runs`() {
        val retryAnswer = CompletableDeferred<Result<Unit>>()
        val removeGambler = mockk<RemoveGambler>()
        coEvery { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) } coAnswers {
            Result.failure(RuntimeException("offline"))
        } coAndThen { retryAnswer.await() }
        val viewModel = removeGamblerViewModel(removeGambler = removeGambler)
        viewModel.remove()

        viewModel.retry()

        viewModel.state.value.shouldBeInstanceOf<MutationState.Mutating<PoolMemberModel>>()
        registry.failedGamblerIds.value.shouldBeEmpty()

        retryAnswer.complete(Result.success(Unit))

        viewModel.state.value shouldBe MutationState.Mutated(original = MEMBER, updated = MEMBER)
        registry.removedGamblerIds shouldContainExactly setOf(GAMBLER_ID)
        coVerify(exactly = 2) { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) }
    }

    @Test
    fun `given an idle row when retried then no request starts`() {
        val removeGambler = removeGambler { Result.success(Unit) }
        val viewModel = removeGamblerViewModel(removeGambler = removeGambler)

        viewModel.retry()

        viewModel.state.value shouldBe MutationState.Idle(MEMBER)
        coVerify(exactly = 0) { removeGambler.execute(poolId = any(), gamblerId = any()) }
    }

    @Test
    fun `given a failed removal when dismissed then the row is idle, the failure is cleared, and no request starts`() {
        val removeGambler = removeGambler { Result.failure(RuntimeException("offline")) }
        val viewModel = removeGamblerViewModel(removeGambler = removeGambler)
        viewModel.remove()

        viewModel.dismissFailure()

        viewModel.state.value shouldBe MutationState.Idle(MEMBER)
        registry.failedGamblerIds.value.shouldBeEmpty()
        registry.removedGamblerIds.shouldBeEmpty()
        coVerify(exactly = 1) { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) }
    }

    @Test
    fun `given a failed removal when the screen clears the failure then the row returns to idle`() {
        val viewModel = removeGamblerViewModel(
            removeGambler = removeGambler { Result.failure(RuntimeException("offline")) },
        )
        viewModel.remove()

        // As the pull-to-refresh does for every failed row.
        registry.clearFailed(GAMBLER_ID)

        viewModel.state.value shouldBe MutationState.Idle(MEMBER)
    }

    @Test
    fun `given another gambler's failure is cleared then this row's failure stays`() {
        val viewModel = removeGamblerViewModel(
            removeGambler = removeGambler { Result.failure(RuntimeException("offline")) },
        )
        viewModel.remove()
        registry.markFailed("another-gambler")

        registry.clearFailed("another-gambler")

        viewModel.state.value.shouldBeInstanceOf<MutationState.Failure<PoolMemberModel>>()
        registry.failedGamblerIds.value shouldContainExactly setOf(GAMBLER_ID)
    }

    private fun removeGambler(answer: suspend () -> Result<Unit>): RemoveGambler {
        val removeGambler = mockk<RemoveGambler>()
        coEvery { removeGambler.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) } coAnswers { answer() }
        return removeGambler
    }

    private fun removeGamblerViewModel(removeGambler: RemoveGambler) =
        RemoveGamblerViewModel(
            poolId = POOL_ID,
            member = MEMBER,
            removeGambler = removeGambler,
            registry = registry,
        )

    private class RecordingRegistry : RemovalRegistry {
        var removedGamblerIds: Set<String> = emptySet()
            private set

        private val _failedGamblerIds = MutableStateFlow<Set<String>>(emptySet())
        override val failedGamblerIds: StateFlow<Set<String>> = _failedGamblerIds.asStateFlow()

        override fun markRemoved(gamblerId: String) {
            removedGamblerIds = removedGamblerIds + gamblerId
        }

        override fun markFailed(gamblerId: String) {
            _failedGamblerIds.update { ids -> ids + gamblerId }
        }

        override fun clearFailed(gamblerId: String) {
            _failedGamblerIds.update { ids -> ids - gamblerId }
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
