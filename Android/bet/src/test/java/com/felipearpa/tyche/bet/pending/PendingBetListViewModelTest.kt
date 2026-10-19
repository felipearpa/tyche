package com.felipearpa.tyche.bet.pending

import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.Draft
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.RowState
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.data.bet.application.GetPendingPoolGamblerBets
import com.felipearpa.tyche.data.bet.application.PlaceBet
import com.felipearpa.tyche.data.bet.domain.BetException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The Bets tab's filter counts and membership, derived from the loaded rows' holders and kept
 * current by their outcomes, with pages presented the way the list presents them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PendingBetListViewModelTest {
    private val noBet = PendingBetFixtures.match("no-bet")
    private val placed = PendingBetFixtures.match("placed", betScore = TeamScore(2, 1))
    private val lockedPlaced = PendingBetFixtures.match(
        "locked-placed",
        betScore = TeamScore(1, 0),
        isLocked = true,
        kickoffDay = 1,
    )
    private val lockedWithoutBet = PendingBetFixtures.match("locked-without-bet", isLocked = true, kickoffDay = 1)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given loaded matches then the counts cover them`() = runTest {
        val viewModel = viewModel(ScriptedPendingBetRepository(listOf(page(noBet, placed, lockedPlaced, lockedWithoutBet))))

        present(viewModel)

        // A locked match without a prediction can no longer get one, so it is in neither filter.
        viewModel.listState.value.counts shouldBe PendingBetFilterCounts(all = 4, noBet = 1, placed = 2)
    }

    @Test
    fun `given the No bet filter when a match is saved then it moves to Placed`() = runTest {
        val repository = ScriptedPendingBetRepository(listOf(page(noBet, placed)))
        val viewModel = viewModel(repository)
        val presenter = present(viewModel)
        viewModel.selectFilter(PendingBetFilter.NO_BET)
        visibleMatchIds(viewModel, presenter) shouldBe listOf(listOf("no-bet"))

        save(viewModel.rowHolder(noBet), repository)

        viewModel.listState.value.counts shouldBe PendingBetFilterCounts(all = 2, noBet = 0, placed = 2)
        visibleMatchIds(viewModel, presenter) shouldBe emptyList()
        viewModel.listState.value.isFilteredEmpty shouldBe true
    }

    @Test
    fun `given a page append then the counts grow`() = runTest {
        val later = PendingBetFixtures.match("later", kickoffDay = 2)
        val viewModel = viewModel(
            ScriptedPendingBetRepository(listOf(page(noBet, placed, next = "page-2"), page(later))),
        )
        val presenter = present(viewModel)
        viewModel.listState.value.counts shouldBe PendingBetFilterCounts(all = 2, noBet = 1, placed = 1)

        presenter.readLastRow()

        presenter.matchIds() shouldBe listOf("no-bet", "placed", "later")
        viewModel.listState.value.counts shouldBe PendingBetFilterCounts(all = 3, noBet = 2, placed = 1)
    }

    @Test
    fun `given a draft when a page is appended then the row keeps its holder and draft`() = runTest {
        val later = PendingBetFixtures.match("later", kickoffDay = 2)
        val viewModel = viewModel(
            ScriptedPendingBetRepository(listOf(page(noBet, next = "page-2"), page(later))),
        )
        val presenter = present(viewModel)
        val row = viewModel.rowHolder(noBet)
        row.setHome(2)

        presenter.readLastRow()

        presenter.size shouldBe 2
        viewModel.rowHolder(noBet) shouldBeSameInstanceAs row
        row.draft.value shouldBe Draft(2, null)
        row.rowState shouldBe RowState.INCOMPLETE
    }

    @Test
    fun `given a save on the first page when the next page arrives then the save and the counts stand`() = runTest {
        val later = PendingBetFixtures.match("later", kickoffDay = 2)
        val repository = ScriptedPendingBetRepository(listOf(page(noBet, placed, next = "page-2"), page(later)))
        val viewModel = viewModel(repository)
        val presenter = present(viewModel)
        val row = viewModel.rowHolder(noBet)
        save(row, repository)

        presenter.readLastRow()

        presenter.matchIds() shouldBe listOf("no-bet", "placed", "later")
        // The presented page still holds the feed's copy without the prediction; the row keeps the save.
        presenter.matches().first().betScore shouldBe null
        viewModel.rowHolder(presenter.matches().first()) shouldBeSameInstanceAs row
        row.saved shouldBe SavedBet.Placed(TeamScore(1, 1))
        row.rowState shouldBe RowState.SAVED
        viewModel.listState.value.counts shouldBe PendingBetFilterCounts(all = 3, noBet = 1, placed = 2)
    }

    @Test
    fun `given each filter then it keeps its matches under their kickoff days`() = runTest {
        val viewModel = viewModel(ScriptedPendingBetRepository(listOf(page(noBet, placed, lockedPlaced, lockedWithoutBet))))
        val presenter = present(viewModel)

        visibleMatchIds(viewModel, presenter) shouldBe
            listOf(listOf("no-bet", "placed"), listOf("locked-placed", "locked-without-bet"))

        viewModel.selectFilter(PendingBetFilter.PLACED)
        visibleMatchIds(viewModel, presenter) shouldBe listOf(listOf("placed"), listOf("locked-placed"))

        viewModel.selectFilter(PendingBetFilter.NO_BET)
        visibleMatchIds(viewModel, presenter) shouldBe listOf(listOf("no-bet"))
        viewModel.listState.value.isFilteredEmpty shouldBe false
    }

    @Test
    fun `given a filter then every paging item keeps its slot`() = runTest {
        val viewModel = viewModel(ScriptedPendingBetRepository(listOf(page(noBet, placed, lockedPlaced))))
        val presenter = present(viewModel)

        viewModel.selectFilter(PendingBetFilter.NO_BET)
        val rows = viewModel.listState.value.rowsFor(presenter.matches())

        rows.map { row -> row.match.matchId to row.isVisible } shouldBe
            listOf("no-bet" to true, "placed" to false, "locked-placed" to false)
    }

    @Test
    fun `given every loaded match has a prediction when No bet is selected then the filter is empty`() = runTest {
        val viewModel = viewModel(ScriptedPendingBetRepository(listOf(page(placed, lockedPlaced))))
        val presenter = present(viewModel)

        viewModel.selectFilter(PendingBetFilter.NO_BET)

        visibleMatchIds(viewModel, presenter) shouldBe emptyList()
        viewModel.listState.value.isFilteredEmpty shouldBe true
    }

    @Test
    fun `given no loaded match has a prediction when Placed is selected then the filter is empty`() = runTest {
        val viewModel = viewModel(ScriptedPendingBetRepository(listOf(page(noBet, lockedWithoutBet))))
        present(viewModel)

        viewModel.selectFilter(PendingBetFilter.PLACED)

        viewModel.listState.value.isFilteredEmpty shouldBe true
    }

    @Test
    fun `given an empty feed when a filter is selected then the list is not filtered empty`() = runTest {
        val viewModel = viewModel(ScriptedPendingBetRepository(listOf(page())))
        present(viewModel)

        viewModel.selectFilter(PendingBetFilter.NO_BET)

        viewModel.listState.value.isFilteredEmpty shouldBe false
    }

    @Test
    fun `given a draft of a filtered-out match when the filter returns to All then the draft survives`() = runTest {
        val viewModel = viewModel(ScriptedPendingBetRepository(listOf(page(noBet, placed))))
        val presenter = present(viewModel)
        val row = viewModel.rowHolder(noBet)
        row.setHome(2)

        viewModel.selectFilter(PendingBetFilter.PLACED)
        visibleMatchIds(viewModel, presenter) shouldBe listOf(listOf("placed"))
        viewModel.selectFilter(PendingBetFilter.ALL)

        viewModel.rowHolder(noBet) shouldBeSameInstanceAs row
        row.draft.value shouldBe Draft(2, null)
    }

    @Test
    fun `given a filtered-out match when its save completes then the counts update`() = runTest {
        val repository = ScriptedPendingBetRepository(listOf(page(noBet, placed)))
        val viewModel = viewModel(repository)
        val presenter = present(viewModel)
        viewModel.selectFilter(PendingBetFilter.PLACED)

        save(viewModel.rowHolder(noBet), repository)

        visibleMatchIds(viewModel, presenter) shouldBe listOf(listOf("no-bet", "placed"))
        viewModel.listState.value.counts.placed shouldBe 2
    }

    @Test
    fun `given a forbidden save then the locked match leaves No bet`() = runTest {
        val repository = ScriptedPendingBetRepository(listOf(page(noBet, placed)))
        val viewModel = viewModel(repository)
        present(viewModel)
        val row = viewModel.rowHolder(noBet)
        row.setBoth(home = 1, away = 1)
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()

        repository.complete(Result.failure(BetException.Forbidden()))
        runCurrent()

        row.rowState shouldBe RowState.LOCKED
        viewModel.listState.value.counts shouldBe PendingBetFilterCounts(all = 2, noBet = 0, placed = 1)
    }

    @Test
    fun `given a refresh then the counts cover only what it returns`() = runTest {
        val later = PendingBetFixtures.match("later", kickoffDay = 2)
        val viewModel = viewModel(
            ScriptedPendingBetRepository(listOf(page(noBet, placed, next = "page-2"), page(later))),
        )
        val presenter = present(viewModel)
        presenter.readLastRow()
        viewModel.listState.value.counts.all shouldBe 3

        presenter.refresh()
        runCurrent()

        presenter.matchIds() shouldBe listOf("no-bet", "placed")
        viewModel.listState.value.counts shouldBe PendingBetFilterCounts(all = 2, noBet = 1, placed = 1)
    }

    private fun viewModel(repository: ScriptedPendingBetRepository) = PendingBetListViewModel(
        poolId = PendingBetFixtures.POOL_ID,
        gamblerId = PendingBetFixtures.GAMBLER_ID,
        getPendingPoolGamblerBets = GetPendingPoolGamblerBets(repository),
        placeBet = PlaceBet(repository),
    )

    /** Presents the feed like the list does; the presenter keeps collecting for the whole test. */
    private fun TestScope.present(viewModel: PendingBetListViewModel): RecordingPresenter {
        val presenter = RecordingPresenter(this)
        backgroundScope.launch { viewModel.poolGamblerBets.collectLatest(presenter::collectFrom) }
        runCurrent()
        return presenter
    }

    private fun TestScope.save(row: PendingBetRowHolder, repository: ScriptedPendingBetRepository) {
        row.setBoth(home = 1, away = 1)
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()
        repository.completeSaving(repository.submissions.last())
        runCurrent()
        row.rowState shouldBe RowState.SAVED
    }

    /** The visible matches grouped under the day headers the list would show. */
    private fun visibleMatchIds(viewModel: PendingBetListViewModel, presenter: RecordingPresenter): List<List<String>> =
        viewModel.listState.value.rowsFor(presenter.matches())
            .filter { row -> row.isVisible }
            .fold(emptyList()) { sections, row ->
                if (row.dayHeader != null) {
                    sections + listOf(listOf(row.match.matchId))
                } else {
                    sections.dropLast(1) + listOf(sections.last() + row.match.matchId)
                }
            }

    private fun page(vararg matches: PoolGamblerBetModel, next: String? = null) =
        PendingBetFixtures.page(matches.toList(), next)

    /** Presents paging data the way a list does, so reading the last row requests the next page. */
    private class RecordingPresenter(private val testScope: TestScope) : PagingDataPresenter<PoolGamblerBetModel>(
        mainContext = Dispatchers.Main,
    ) {
        fun matches(): List<PoolGamblerBetModel> = snapshot().items

        fun matchIds(): List<String> = matches().map { match -> match.matchId }

        fun readLastRow() {
            get(size - 1)
            testScope.runCurrent()
        }

        override suspend fun presentPagingDataEvent(event: PagingDataEvent<PoolGamblerBetModel>) = Unit
    }
}
