package com.felipearpa.tyche.pool.managegamblers

import com.felipearpa.tyche.data.pool.application.GetPool
import com.felipearpa.tyche.data.pool.application.GetPoolMembers
import com.felipearpa.tyche.data.pool.application.RemoveGambler
import com.felipearpa.tyche.data.pool.domain.Pool
import com.felipearpa.ui.state.LoadState
import com.felipearpa.ui.state.MutationState
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The screen hides gamblers in `removedGamblerIds`, releases rows in `failedGamblerIds` on
 * refresh, shows `gamblerCount` in its heading, and gives each row the holder in `rowHolders`;
 * removals are started through `remove(member)` and finished through the holders checked here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManageGamblersViewModelTest {

    private val getPool = mockk<GetPool>()
    private val removeGambler = mockk<RemoveGambler>()

    @BeforeEach
    fun setUp() {
        // viewModelScope runs on Main; runTest shares this dispatcher's scheduler.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        answerCount(3)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given no removals when the screen opens then no gambler is removed or failed`() = runTest {
        val viewModel = manageGamblersViewModel()

        viewModel.removedGamblerIds.value.shouldBeEmpty()
        viewModel.failedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given no action on a row when the screen opens then no row has a holder`() = runTest {
        val viewModel = manageGamblersViewModel()

        viewModel.rowHolders.value.shouldBeEmpty()
        viewModel.rowHolder(SANTIAGO.gamblerId).shouldBeNull()
    }

    @Test
    fun `given removed gamblers when the server confirms them then each stays in the removed set`() = runTest {
        answerRemoval(SANTIAGO, Result.success(Unit))
        answerRemoval(JAIME, Result.success(Unit))
        val viewModel = manageGamblersViewModel()

        viewModel.remove(SANTIAGO)
        viewModel.remove(JAIME)

        viewModel.removedGamblerIds.value shouldContainExactly setOf(SANTIAGO.gamblerId, JAIME.gamblerId)
        viewModel.failedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given failed gamblers when one is dismissed then only the other stays failed`() = runTest {
        answerRemoval(SANTIAGO, offline())
        answerRemoval(JAIME, offline())
        val viewModel = manageGamblersViewModel()

        viewModel.remove(SANTIAGO)
        viewModel.remove(JAIME)
        viewModel.rowHolder(SANTIAGO.gamblerId).shouldNotBeNull().dismissFailure()

        viewModel.failedGamblerIds.value shouldContainExactly setOf(JAIME.gamblerId)
        viewModel.removedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given a failed gambler when a later attempt removes them then they are removed and no longer failed`() =
        runTest {
            coEvery { removeGambler.execute(poolId = POOL_ID, gamblerId = SANTIAGO.gamblerId) } returns
                offline() andThen Result.success(Unit)
            val viewModel = manageGamblersViewModel()

            viewModel.remove(SANTIAGO)
            viewModel.rowHolder(SANTIAGO.gamblerId).shouldNotBeNull().retry()

            viewModel.removedGamblerIds.value shouldContainExactly setOf(SANTIAGO.gamblerId)
            viewModel.failedGamblerIds.value.shouldBeEmpty()
        }

    @Test
    fun `given a row that never failed when its failure is dismissed then the sets are unchanged`() = runTest {
        answerRemoval(JAIME, offline())
        val answer = CompletableDeferred<Result<Unit>>()
        answerRemoval(SANTIAGO) { answer.await() }
        val viewModel = manageGamblersViewModel()
        viewModel.remove(JAIME)
        viewModel.remove(SANTIAGO)

        viewModel.rowHolder(SANTIAGO.gamblerId).shouldNotBeNull().dismissFailure()

        viewModel.failedGamblerIds.value shouldContainExactly setOf(JAIME.gamblerId)
        viewModel.removedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given a removal in flight when the gambler is removed again then the same holder sends one request`() =
        runTest {
            val answer = CompletableDeferred<Result<Unit>>()
            answerRemoval(SANTIAGO) { answer.await() }
            val viewModel = manageGamblersViewModel()

            viewModel.remove(SANTIAGO)
            val holder = viewModel.rowHolder(SANTIAGO.gamblerId).shouldNotBeNull()
            viewModel.remove(SANTIAGO)

            viewModel.rowHolder(SANTIAGO.gamblerId) shouldBeSameInstanceAs holder
            viewModel.rowHolders.value.keys shouldContainExactly setOf(SANTIAGO.gamblerId)
            holder.removal.value.shouldBeInstanceOf<MutationState.Mutating<PoolMemberModel>>()
            coVerify(exactly = 1) { removeGambler.execute(poolId = POOL_ID, gamblerId = SANTIAGO.gamblerId) }
        }

    @Test
    fun `given a removal when the server confirms it then the holder is dropped`() = runTest {
        val answer = CompletableDeferred<Result<Unit>>()
        answerRemoval(SANTIAGO) { answer.await() }
        val viewModel = manageGamblersViewModel()
        viewModel.remove(SANTIAGO)
        viewModel.rowHolder(SANTIAGO.gamblerId).shouldNotBeNull()

        answer.complete(Result.success(Unit))

        viewModel.rowHolder(SANTIAGO.gamblerId).shouldBeNull()
        viewModel.removedGamblerIds.value shouldContainExactly setOf(SANTIAGO.gamblerId)
    }

    @Test
    fun `given a failed removal when dismissed then the holder is dropped`() = runTest {
        answerRemoval(SANTIAGO, offline())
        val viewModel = manageGamblersViewModel()
        viewModel.remove(SANTIAGO)

        viewModel.rowHolder(SANTIAGO.gamblerId).shouldNotBeNull().dismissFailure()

        viewModel.rowHolder(SANTIAGO.gamblerId).shouldBeNull()
        viewModel.failedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given a failed removal when retried then the holder is kept while the new request runs`() = runTest {
        val retryAnswer = CompletableDeferred<Result<Unit>>()
        coEvery { removeGambler.execute(poolId = POOL_ID, gamblerId = SANTIAGO.gamblerId) } coAnswers {
            offline()
        } coAndThen { retryAnswer.await() }
        val viewModel = manageGamblersViewModel()
        viewModel.remove(SANTIAGO)
        val holder = viewModel.rowHolder(SANTIAGO.gamblerId).shouldNotBeNull()

        holder.retry()

        viewModel.rowHolder(SANTIAGO.gamblerId) shouldBeSameInstanceAs holder
        holder.removal.value.shouldBeInstanceOf<MutationState.Mutating<PoolMemberModel>>()
        viewModel.failedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given an unanswered count request when the screen opens then the count is loading`() = runTest {
        val answer = CompletableDeferred<Result<Pool>>()
        coEvery { getPool.execute(poolId = POOL_ID) } coAnswers { answer.await() }

        val viewModel = manageGamblersViewModel()

        viewModel.gamblerCount.value shouldBe LoadState.Loading
    }

    @Test
    fun `given the pool's count when the screen opens then the count is loaded`() = runTest {
        val viewModel = manageGamblersViewModel()

        viewModel.gamblerCount.value shouldBe LoadState.Loaded(3)
        coVerify(exactly = 1) { getPool.execute(poolId = POOL_ID) }
    }

    @Test
    fun `given the pool request fails when the screen opens then the count fails`() = runTest {
        coEvery { getPool.execute(poolId = POOL_ID) } returns Result.failure(RuntimeException("offline"))

        val viewModel = manageGamblersViewModel()

        viewModel.gamblerCount.value.shouldBeInstanceOf<LoadState.Failure>()
    }

    @Test
    fun `given a pool without a count when the screen opens then the count fails rather than guessing`() = runTest {
        coEvery { getPool.execute(poolId = POOL_ID) } returns Result.success(pool(gamblerCount = null))

        val viewModel = manageGamblersViewModel()

        viewModel.gamblerCount.value.shouldBeInstanceOf<LoadState.Failure>()
            .exception.shouldBeInstanceOf<MissingGamblerCountException>()
    }

    @Test
    fun `given a loaded count when removals succeed or fail then only the successes lower it`() = runTest {
        answerRemoval(SANTIAGO, Result.success(Unit))
        answerRemoval(JAIME, offline())
        val viewModel = manageGamblersViewModel()

        viewModel.remove(SANTIAGO)
        viewModel.remove(JAIME)

        viewModel.gamblerCount.value shouldBe LoadState.Loaded(2)
    }

    @Test
    fun `given someone joined when the owner refreshes then the new count is shown`() = runTest {
        val viewModel = manageGamblersViewModel()
        answerCount(4)

        viewModel.refresh()

        viewModel.gamblerCount.value shouldBe LoadState.Loaded(4)
        coVerify(exactly = 2) { getPool.execute(poolId = POOL_ID) }
    }

    @Test
    fun `given a refresh whose count is unanswered when observing the count then the previous count stays`() =
        runTest {
            val viewModel = manageGamblersViewModel()
            val answer = CompletableDeferred<Result<Pool>>()
            coEvery { getPool.execute(poolId = POOL_ID) } coAnswers { answer.await() }

            viewModel.refresh()
            viewModel.gamblerCount.value shouldBe LoadState.Loaded(3)

            answer.complete(Result.success(pool(gamblerCount = 5)))
            viewModel.gamblerCount.value shouldBe LoadState.Loaded(5)
        }

    @Test
    fun `given a refresh whose count request fails when observing the count then it fails`() = runTest {
        val viewModel = manageGamblersViewModel()
        coEvery { getPool.execute(poolId = POOL_ID) } returns Result.failure(RuntimeException("offline"))

        viewModel.refresh()

        viewModel.gamblerCount.value.shouldBeInstanceOf<LoadState.Failure>()
    }

    @Test
    fun `given a removal before a refresh when the refreshed members land then the gambler is released and counted once`() =
        runTest {
            answerRemoval(SANTIAGO, Result.success(Unit))
            val viewModel = manageGamblersViewModel()
            viewModel.remove(SANTIAGO)
            // The server already applied the removal when the refreshed count was requested.
            answerCount(2)

            viewModel.refresh()
            viewModel.removedGamblerIds.value shouldContainExactly setOf(SANTIAGO.gamblerId)
            viewModel.gamblerCount.value shouldBe LoadState.Loaded(2)

            viewModel.onMembersRefreshed()
            viewModel.removedGamblerIds.value.shouldBeEmpty()
            viewModel.gamblerCount.value shouldBe LoadState.Loaded(2)
        }

    @Test
    fun `given a removal during a refresh when the refreshed members land then that gambler stays hidden`() =
        runTest {
            answerRemoval(JAIME, Result.success(Unit))
            val viewModel = manageGamblersViewModel()
            viewModel.refresh()

            viewModel.remove(JAIME)
            viewModel.onMembersRefreshed()

            viewModel.removedGamblerIds.value shouldContainExactly setOf(JAIME.gamblerId)
            viewModel.gamblerCount.value shouldBe LoadState.Loaded(2)
        }

    @Test
    fun `given a removal that the server confirms after a refresh started when the refreshed members land then that gambler stays hidden`() =
        runTest {
            val answer = CompletableDeferred<Result<Unit>>()
            answerRemoval(JAIME) { answer.await() }
            val viewModel = manageGamblersViewModel()
            viewModel.remove(JAIME)

            viewModel.refresh()
            answer.complete(Result.success(Unit))
            viewModel.onMembersRefreshed()

            viewModel.removedGamblerIds.value shouldContainExactly setOf(JAIME.gamblerId)
            viewModel.gamblerCount.value shouldBe LoadState.Loaded(2)
        }

    @Test
    fun `given no refresh when members finish loading then removed gamblers stay hidden`() = runTest {
        answerRemoval(SANTIAGO, Result.success(Unit))
        val viewModel = manageGamblersViewModel()
        viewModel.remove(SANTIAGO)

        viewModel.onMembersRefreshed()

        viewModel.removedGamblerIds.value shouldContainExactly setOf(SANTIAGO.gamblerId)
    }

    @Test
    fun `given failed gamblers when the owner refreshes then every failure is cleared`() = runTest {
        answerRemoval(SANTIAGO, offline())
        answerRemoval(JAIME, offline())
        val viewModel = manageGamblersViewModel()
        viewModel.remove(SANTIAGO)
        viewModel.remove(JAIME)

        viewModel.refresh()

        viewModel.failedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given failed and in-flight removals when the owner refreshes then failed holders are dropped and in-flight ones kept`() =
        runTest {
            answerRemoval(SANTIAGO, offline())
            val answer = CompletableDeferred<Result<Unit>>()
            answerRemoval(JAIME) { answer.await() }
            val viewModel = manageGamblersViewModel()
            viewModel.remove(SANTIAGO)
            viewModel.remove(JAIME)
            val inFlight = viewModel.rowHolder(JAIME.gamblerId).shouldNotBeNull()

            viewModel.refresh()

            viewModel.rowHolder(SANTIAGO.gamblerId).shouldBeNull()
            viewModel.rowHolder(JAIME.gamblerId) shouldBeSameInstanceAs inFlight
            inFlight.removal.value.shouldBeInstanceOf<MutationState.Mutating<PoolMemberModel>>()
        }

    @Test
    fun `given an earlier count answers after a later one when observing the count then the later one stays`() =
        runTest {
            val earlier = CompletableDeferred<Result<Pool>>()
            coEvery { getPool.execute(poolId = POOL_ID) } coAnswers { earlier.await() }
            val viewModel = manageGamblersViewModel()
            answerCount(4)

            viewModel.refresh()
            earlier.complete(Result.success(pool(gamblerCount = 3)))

            viewModel.gamblerCount.value shouldBe LoadState.Loaded(4)
        }

    private fun answerCount(count: Int) {
        coEvery { getPool.execute(poolId = POOL_ID) } returns Result.success(pool(gamblerCount = count))
    }

    private fun answerRemoval(member: PoolMemberModel, result: Result<Unit>) {
        coEvery { removeGambler.execute(poolId = POOL_ID, gamblerId = member.gamblerId) } returns result
    }

    private fun answerRemoval(member: PoolMemberModel, answer: suspend () -> Result<Unit>) {
        coEvery { removeGambler.execute(poolId = POOL_ID, gamblerId = member.gamblerId) } coAnswers { answer() }
    }

    private fun offline() = Result.failure<Unit>(RuntimeException("offline"))

    private fun pool(gamblerCount: Int?) =
        Pool(id = POOL_ID, name = "prueba", creatorGamblerId = "owner", gamblerCount = gamblerCount)

    private fun manageGamblersViewModel() =
        ManageGamblersViewModel(
            poolId = POOL_ID,
            getPoolMembers = mockk<GetPoolMembers>(),
            getPool = getPool,
            removeGambler = removeGambler,
        )

    private companion object {
        const val POOL_ID = "pool"
        val SANTIAGO = PoolMemberModel(
            gamblerId = "santiago-id",
            gamblerUsername = "santiago",
            gamblerEmail = "santiago@example.com",
        )
        val JAIME = PoolMemberModel(
            gamblerId = "jaime-id",
            gamblerUsername = "jaime",
            gamblerEmail = "jaime@example.com",
        )
    }
}
