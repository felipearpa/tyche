package com.felipearpa.tyche.poolhome.drawer

import com.felipearpa.tyche.data.pool.application.DeletePool
import com.felipearpa.tyche.data.pool.application.GetPool
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScore
import com.felipearpa.tyche.data.pool.domain.Pool
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScore
import com.felipearpa.tyche.session.CurrentAccountCoordinator
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The pool home drawer shows Gamblers and Delete pool only to the owner, and disables Delete pool
 * while a deletion is pending; both read the state checked here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DrawerViewModelTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given the pool's creator when the drawer state loads then it reports an owner with the gambler count`() =
        runTest {
            val viewModel = drawerViewModel(creatorGamblerId = GAMBLER_ID)
            observe(viewModel)

            viewModel.uiState.value.isOwner.shouldBeTrue()
            viewModel.uiState.value.gamblerCount shouldBe 13
        }

    @Test
    fun `given another gambler's pool when the drawer state loads then it reports a member`() = runTest {
        val viewModel = drawerViewModel(creatorGamblerId = "another-gambler")
        observe(viewModel)

        viewModel.uiState.value.isOwner.shouldBeFalse()
    }

    @Test
    fun `given a deletion in flight when observing the drawer state then it stays pending until the request answers`() =
        runTest {
            val deletePool = mockk<DeletePool>()
            val answer = CompletableDeferred<Result<Unit>>()
            coEvery { deletePool.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) } coAnswers { answer.await() }
            val viewModel = drawerViewModel(creatorGamblerId = GAMBLER_ID, deletePool = deletePool)
            observe(viewModel)
            var deletedCount = 0

            viewModel.deletePool(onSuccess = { deletedCount++ })
            viewModel.uiState.value.isDeleting.shouldBeTrue()

            answer.complete(Result.failure(RuntimeException("offline")))
            viewModel.uiState.value.isDeleting.shouldBeFalse()
            deletedCount shouldBe 0
            coVerify(exactly = 1) { deletePool.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) }
        }

    @Test
    fun `given a deletion in flight when deletion is requested again then no second request starts`() = runTest {
        val deletePool = mockk<DeletePool>()
        val answer = CompletableDeferred<Result<Unit>>()
        coEvery { deletePool.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) } coAnswers { answer.await() }
        val viewModel = drawerViewModel(creatorGamblerId = GAMBLER_ID, deletePool = deletePool)
        observe(viewModel)
        var deletedCount = 0

        // As when the confirmation is pressed twice before its dialog leaves the screen.
        viewModel.deletePool(onSuccess = { deletedCount++ })
        viewModel.deletePool(onSuccess = { deletedCount++ })
        answer.complete(Result.success(Unit))

        coVerify(exactly = 1) { deletePool.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) }
        deletedCount shouldBe 1
    }

    @Test
    fun `given a failed deletion when deletion is requested again then it starts a new request`() = runTest {
        val deletePool = mockk<DeletePool>()
        coEvery { deletePool.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) } returnsMany listOf(
            Result.failure(RuntimeException("offline")),
            Result.success(Unit),
        )
        val viewModel = drawerViewModel(creatorGamblerId = GAMBLER_ID, deletePool = deletePool)
        observe(viewModel)
        var deletedCount = 0

        viewModel.deletePool(onSuccess = { deletedCount++ })
        viewModel.resetDeleteState()
        viewModel.deletePool(onSuccess = { deletedCount++ })

        coVerify(exactly = 2) { deletePool.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) }
        deletedCount shouldBe 1
    }

    @Test
    fun `given a deletion when it succeeds then the host is told once`() = runTest {
        val deletePool = mockk<DeletePool>()
        coEvery { deletePool.execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) } returns Result.success(Unit)
        val viewModel = drawerViewModel(creatorGamblerId = GAMBLER_ID, deletePool = deletePool)
        observe(viewModel)
        var deletedCount = 0

        viewModel.deletePool(onSuccess = { deletedCount++ })

        deletedCount shouldBe 1
    }
}

/** `uiState` shares while subscribed, so a test keeps one subscriber for its duration. */
@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.observe(viewModel: DrawerViewModel) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
}

private fun drawerViewModel(
    creatorGamblerId: String,
    deletePool: DeletePool = mockk(),
): DrawerViewModel {
    val getPoolGamblerScore = mockk<GetPoolGamblerScore> {
        coEvery { execute(poolId = POOL_ID, gamblerId = GAMBLER_ID) } returns Result.success(poolGamblerScore)
    }
    val getPool = mockk<GetPool> {
        coEvery { execute(poolId = POOL_ID) } returns Result.success(
            Pool(id = POOL_ID, name = POOL_NAME, creatorGamblerId = creatorGamblerId, gamblerCount = 13),
        )
    }
    val currentAccountCoordinator = mockk<CurrentAccountCoordinator> {
        every { state } returns MutableStateFlow(null)
    }

    return DrawerViewModel(
        poolId = POOL_ID,
        gamblerId = GAMBLER_ID,
        logOut = mockk(),
        getPoolGamblerScore = getPoolGamblerScore,
        getPool = getPool,
        deletePool = deletePool,
        currentAccountCoordinator = currentAccountCoordinator,
    )
}

private const val POOL_ID = "pool-1"
private const val GAMBLER_ID = "gambler-1"
private const val POOL_NAME = "Copa Mundial"

private val poolGamblerScore = PoolGamblerScore(
    poolId = POOL_ID,
    poolName = POOL_NAME,
    gamblerId = GAMBLER_ID,
    gamblerUsername = "felipearpa",
    position = 1,
    beforePosition = 1,
    score = 41,
    gamblerCount = 13,
)
