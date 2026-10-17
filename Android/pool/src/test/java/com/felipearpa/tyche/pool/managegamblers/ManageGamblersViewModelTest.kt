package com.felipearpa.tyche.pool.managegamblers

import com.felipearpa.tyche.data.pool.application.GetPool
import com.felipearpa.tyche.data.pool.application.GetPoolMembers
import com.felipearpa.tyche.data.pool.domain.Pool
import com.felipearpa.ui.state.LoadState
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The screen hides gamblers in `removedGamblerIds`, releases rows in `failedGamblerIds` on
 * refresh, and shows `gamblerCount` in its heading; the row ViewModels report removals through
 * the registry checked here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManageGamblersViewModelTest {

    private val getPool = mockk<GetPool>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        answerCount(3)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given no removals when the screen opens then no gambler is removed or failed`() {
        val viewModel = manageGamblersViewModel()

        viewModel.removedGamblerIds.value.shouldBeEmpty()
        viewModel.failedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given removed gamblers when marked then each stays in the removed set`() {
        val viewModel = manageGamblersViewModel()

        viewModel.markRemoved("santiago")
        viewModel.markRemoved("jaime")

        viewModel.removedGamblerIds.value shouldContainExactly setOf("santiago", "jaime")
        viewModel.failedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given failed gamblers when one is cleared then only the other stays failed`() {
        val viewModel = manageGamblersViewModel()

        viewModel.markFailed("santiago")
        viewModel.markFailed("jaime")
        viewModel.clearFailed("santiago")

        viewModel.failedGamblerIds.value shouldContainExactly setOf("jaime")
        viewModel.removedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given a failed gambler when a later attempt removes them then they are removed and no longer failed`() {
        val viewModel = manageGamblersViewModel()

        viewModel.markFailed("santiago")
        viewModel.markRemoved("santiago")

        viewModel.removedGamblerIds.value shouldContainExactly setOf("santiago")
        viewModel.failedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given a gambler that never failed when cleared then the sets are unchanged`() {
        val viewModel = manageGamblersViewModel()
        viewModel.markFailed("jaime")

        viewModel.clearFailed("santiago")

        viewModel.failedGamblerIds.value shouldContainExactly setOf("jaime")
    }

    @Test
    fun `given an unanswered count request when the screen opens then the count is loading`() {
        val answer = CompletableDeferred<Result<Pool>>()
        coEvery { getPool.execute(poolId = POOL_ID) } coAnswers { answer.await() }

        val viewModel = manageGamblersViewModel()

        viewModel.gamblerCount.value shouldBe LoadState.Loading
    }

    @Test
    fun `given the pool's count when the screen opens then the count is loaded`() {
        val viewModel = manageGamblersViewModel()

        viewModel.gamblerCount.value shouldBe LoadState.Loaded(3)
        coVerify(exactly = 1) { getPool.execute(poolId = POOL_ID) }
    }

    @Test
    fun `given the pool request fails when the screen opens then the count fails`() {
        coEvery { getPool.execute(poolId = POOL_ID) } returns Result.failure(RuntimeException("offline"))

        val viewModel = manageGamblersViewModel()

        viewModel.gamblerCount.value.shouldBeInstanceOf<LoadState.Failure>()
    }

    @Test
    fun `given a pool without a count when the screen opens then the count fails rather than guessing`() {
        coEvery { getPool.execute(poolId = POOL_ID) } returns Result.success(pool(gamblerCount = null))

        val viewModel = manageGamblersViewModel()

        viewModel.gamblerCount.value.shouldBeInstanceOf<LoadState.Failure>()
            .exception.shouldBeInstanceOf<MissingGamblerCountException>()
    }

    @Test
    fun `given a loaded count when removals succeed or fail then only the successes lower it`() {
        val viewModel = manageGamblersViewModel()

        viewModel.markRemoved("santiago")
        viewModel.markFailed("jaime")

        viewModel.gamblerCount.value shouldBe LoadState.Loaded(2)
    }

    @Test
    fun `given someone joined when the owner refreshes then the new count is shown`() {
        val viewModel = manageGamblersViewModel()
        answerCount(4)

        viewModel.refresh()

        viewModel.gamblerCount.value shouldBe LoadState.Loaded(4)
        coVerify(exactly = 2) { getPool.execute(poolId = POOL_ID) }
    }

    @Test
    fun `given a refresh whose count is unanswered when observing the count then the previous count stays`() {
        val viewModel = manageGamblersViewModel()
        val answer = CompletableDeferred<Result<Pool>>()
        coEvery { getPool.execute(poolId = POOL_ID) } coAnswers { answer.await() }

        viewModel.refresh()
        viewModel.gamblerCount.value shouldBe LoadState.Loaded(3)

        answer.complete(Result.success(pool(gamblerCount = 5)))
        viewModel.gamblerCount.value shouldBe LoadState.Loaded(5)
    }

    @Test
    fun `given a refresh whose count request fails when observing the count then it fails`() {
        val viewModel = manageGamblersViewModel()
        coEvery { getPool.execute(poolId = POOL_ID) } returns Result.failure(RuntimeException("offline"))

        viewModel.refresh()

        viewModel.gamblerCount.value.shouldBeInstanceOf<LoadState.Failure>()
    }

    @Test
    fun `given a removal before a refresh when the refreshed members land then the gambler is released and counted once`() {
        val viewModel = manageGamblersViewModel()
        viewModel.markRemoved("santiago")
        // The server already applied the removal when the refreshed count was requested.
        answerCount(2)

        viewModel.refresh()
        viewModel.removedGamblerIds.value shouldContainExactly setOf("santiago")
        viewModel.gamblerCount.value shouldBe LoadState.Loaded(2)

        viewModel.onMembersRefreshed()
        viewModel.removedGamblerIds.value.shouldBeEmpty()
        viewModel.gamblerCount.value shouldBe LoadState.Loaded(2)
    }

    @Test
    fun `given a removal during a refresh when the refreshed members land then that gambler stays hidden`() {
        val viewModel = manageGamblersViewModel()
        viewModel.refresh()

        viewModel.markRemoved("jaime")
        viewModel.onMembersRefreshed()

        viewModel.removedGamblerIds.value shouldContainExactly setOf("jaime")
        viewModel.gamblerCount.value shouldBe LoadState.Loaded(2)
    }

    @Test
    fun `given no refresh when members finish loading then removed gamblers stay hidden`() {
        val viewModel = manageGamblersViewModel()
        viewModel.markRemoved("santiago")

        viewModel.onMembersRefreshed()

        viewModel.removedGamblerIds.value shouldContainExactly setOf("santiago")
    }

    @Test
    fun `given failed gamblers when the owner refreshes then every failure is cleared`() {
        val viewModel = manageGamblersViewModel()
        viewModel.markFailed("santiago")
        viewModel.markFailed("jaime")

        viewModel.refresh()

        viewModel.failedGamblerIds.value.shouldBeEmpty()
    }

    @Test
    fun `given an earlier count answers after a later one when observing the count then the later one stays`() {
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

    private fun pool(gamblerCount: Int?) =
        Pool(id = POOL_ID, name = "prueba", creatorGamblerId = "owner", gamblerCount = gamblerCount)

    private fun manageGamblersViewModel() =
        ManageGamblersViewModel(poolId = POOL_ID, getPoolMembers = mockk<GetPoolMembers>(), getPool = getPool)

    private companion object {
        const val POOL_ID = "pool"
    }
}
