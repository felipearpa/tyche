import Combine
import Core
import DataBet
import Foundation
import Testing
@testable import Bet

/// The Bets tab's filter counts and visible sections, derived from the loaded rows and kept current
/// by the rows' outcomes.
@MainActor
struct PendingBetListViewModelTests {
    private let clock = ManualClock()

    private let noBet = PendingBetFixtures.match("no-bet")
    private let placed = PendingBetFixtures.match("placed", betScore: TeamScore(homeTeamValue: 2, awayTeamValue: 1))
    private let lockedPlaced = PendingBetFixtures.match(
        "locked-placed",
        betScore: TeamScore(homeTeamValue: 1, awayTeamValue: 0),
        isLocked: true,
        kickoffDay: 1
    )
    private let lockedWithoutBet = PendingBetFixtures.match("locked-without-bet", isLocked: true, kickoffDay: 1)

    private func makeViewModel(_ repository: ScriptedPendingBetRepository) -> PendingBetListViewModel {
        PendingBetListViewModel(
            getPoolGamblerBetsUseCase: GetPendingPoolGamblerBetsUseCase(poolGamblerBetRepository: repository),
            betUseCase: BetUseCase(poolGamblerBetRepository: repository),
            gamblerId: "gambler456",
            poolId: "pool123",
            clock: clock
        )
    }

    @Test
    func countsCoverTheLoadedMatches() async {
        let repository = ScriptedPendingBetRepository(
            pages: [PendingBetFixtures.page([noBet, placed, lockedPlaced, lockedWithoutBet])]
        )
        let viewModel = makeViewModel(repository)

        await viewModel.lazyPager.refresh()

        // A locked match without a prediction can no longer get one, so it is in neither filter.
        #expect(viewModel.counts == PendingBetFilterCounts(all: 4, noBet: 1, placed: 2))
    }

    @Test
    func aSaveMovesTheMatchFromNoBetToPlaced() async throws {
        let repository = ScriptedPendingBetRepository(pages: [PendingBetFixtures.page([noBet, placed])])
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()
        viewModel.filter = .noBet
        #expect(visibleMatchIds(viewModel) == [["no-bet"]])

        try await save(viewModel.rowHolder(for: noBet), as: noBet, in: repository)

        #expect(viewModel.counts == PendingBetFilterCounts(all: 2, noBet: 0, placed: 2))
        #expect(visibleMatchIds(viewModel).isEmpty)
        #expect(viewModel.isFilteredEmpty)
    }

    @Test
    func theListRepublishesWhenARowIsSaved() async throws {
        let repository = ScriptedPendingBetRepository(pages: [PendingBetFixtures.page([noBet])])
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()
        var publishes = 0
        let subscription = viewModel.objectWillChange.sink { publishes += 1 }
        defer { subscription.cancel() }

        try await save(viewModel.rowHolder(for: noBet), as: noBet, in: repository)

        #expect(publishes > 0)
    }

    @Test
    func countsGrowWhenAPageIsAppended() async throws {
        let later = PendingBetFixtures.match("later", kickoffDay: 2)
        let repository = ScriptedPendingBetRepository(pages: [
            PendingBetFixtures.page([noBet, placed], next: "page-2"),
            PendingBetFixtures.page([later]),
        ])
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()
        #expect(viewModel.counts == PendingBetFilterCounts(all: 2, noBet: 1, placed: 1))

        await viewModel.lazyPager.appendIfNeeded(currentIndex: 1)

        #expect(viewModel.lazyPager.loadedItems.map(\.matchId) == ["no-bet", "placed", "later"])
        #expect(viewModel.counts == PendingBetFilterCounts(all: 3, noBet: 2, placed: 1))
    }

    @Test
    func aDraftSurvivesAPageAppend() async {
        let later = PendingBetFixtures.match("later", kickoffDay: 2)
        let repository = ScriptedPendingBetRepository(pages: [
            PendingBetFixtures.page([noBet], next: "page-2"),
            PendingBetFixtures.page([later]),
        ])
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()
        let row = viewModel.rowHolder(for: noBet)
        row.setHome(2)

        await viewModel.lazyPager.appendIfNeeded(currentIndex: 0)

        #expect(viewModel.lazyPager.loadedItems.count == 2)
        #expect(viewModel.rowHolder(for: noBet) === row)
        #expect(row.draftScores == DraftScores(2, nil))
        #expect(row.rowState == .incomplete)
    }

    @Test
    func filtersKeepTheirMatchesUnderTheirKickoffDays() async {
        let repository = ScriptedPendingBetRepository(
            pages: [PendingBetFixtures.page([noBet, placed, lockedPlaced, lockedWithoutBet])]
        )
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()

        #expect(visibleMatchIds(viewModel) == [["no-bet", "placed"], ["locked-placed", "locked-without-bet"]])

        viewModel.filter = .placed
        #expect(visibleMatchIds(viewModel) == [["placed"], ["locked-placed"]])

        viewModel.filter = .noBet
        #expect(visibleMatchIds(viewModel) == [["no-bet"]])
        #expect(!viewModel.isFilteredEmpty)
    }

    @Test
    func noBetIsEmptyWhenEveryLoadedMatchHasAPrediction() async {
        let repository = ScriptedPendingBetRepository(pages: [PendingBetFixtures.page([placed, lockedPlaced])])
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()

        viewModel.filter = .noBet

        #expect(visibleMatchIds(viewModel).isEmpty)
        #expect(viewModel.isFilteredEmpty)
    }

    @Test
    func noBetWaitsForALaterPageThatHoldsAnUnbetMatch() async throws {
        let later = PendingBetFixtures.match("later", kickoffDay: 2)
        let repository = ScriptedPendingBetRepository(pages: [
            PendingBetFixtures.page([placed], next: "page-2"),
            PendingBetFixtures.page([later]),
        ])
        repository.holdsPagesWithCursor = true
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()
        viewModel.filter = .noBet

        // Page 1 holds no unbet match, but page 2 may: no message while more pages exist.
        #expect(visibleMatchIds(viewModel).isEmpty)
        #expect(!viewModel.isFilteredEmpty)

        let append = Task { await viewModel.lazyPager.appendIfNeeded(currentIndex: 0) }
        try #require(await eventually { repository.isHoldingPage })
        #expect(viewModel.lazyPager.loadState.append.isLoading)
        #expect(!viewModel.isFilteredEmpty)

        repository.releaseHeldPage()
        await append.value

        #expect(visibleMatchIds(viewModel) == [["later"]])
        #expect(!viewModel.isFilteredEmpty)
    }

    @Test
    func aSaveSurvivesALaterPageAppend() async throws {
        let later = PendingBetFixtures.match("later", kickoffDay: 2)
        let repository = ScriptedPendingBetRepository(pages: [
            PendingBetFixtures.page([noBet, placed], next: "page-2"),
            PendingBetFixtures.page([later]),
        ])
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()
        let row = viewModel.rowHolder(for: noBet)
        try await save(row, as: noBet, in: repository)

        // The append re-sends page 1's copy of the match, still without the new prediction.
        await viewModel.lazyPager.appendIfNeeded(currentIndex: 1)

        #expect(row.saved == TeamScore(homeTeamValue: 1, awayTeamValue: 1))
        #expect(row.rowState == .saved)
        #expect(viewModel.counts == PendingBetFilterCounts(all: 3, noBet: 1, placed: 2))
    }

    @Test
    func countsArePartialUntilTheLastPage() async {
        let later = PendingBetFixtures.match("later", kickoffDay: 2)
        let repository = ScriptedPendingBetRepository(pages: [
            PendingBetFixtures.page([noBet, placed], next: "page-2"),
            PendingBetFixtures.page([later]),
        ])
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()
        #expect(viewModel.areCountsPartial)

        await viewModel.lazyPager.appendIfNeeded(currentIndex: 1)

        #expect(!viewModel.areCountsPartial)
        #expect(viewModel.counts == PendingBetFilterCounts(all: 3, noBet: 2, placed: 1))
    }

    @Test
    func theEmptyMessageWaitsForTheLastPage() async {
        let laterPlaced = PendingBetFixtures.match(
            "later-placed", betScore: TeamScore(homeTeamValue: 0, awayTeamValue: 0), kickoffDay: 2
        )
        let repository = ScriptedPendingBetRepository(pages: [
            PendingBetFixtures.page([placed], next: "page-2"),
            PendingBetFixtures.page([laterPlaced]),
        ])
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()
        viewModel.filter = .noBet
        #expect(!viewModel.isFilteredEmpty)

        await viewModel.lazyPager.appendIfNeeded(currentIndex: 0)

        #expect(viewModel.lazyPager.loadState.append.endOfPaginationReached)
        #expect(visibleMatchIds(viewModel).isEmpty)
        #expect(viewModel.isFilteredEmpty)
    }

    @Test
    func anEmptyFeedIsNotAFilteredEmptyList() async {
        let viewModel = makeViewModel(ScriptedPendingBetRepository(pages: [PendingBetFixtures.page([])]))
        await viewModel.lazyPager.refresh()

        viewModel.filter = .noBet

        #expect(!viewModel.isFilteredEmpty)
    }

    @Test
    func aDraftOfAFilteredOutMatchSurvives() async throws {
        let repository = ScriptedPendingBetRepository(pages: [PendingBetFixtures.page([noBet, placed])])
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()
        let row = viewModel.rowHolder(for: noBet)
        row.setHome(2)

        viewModel.filter = .placed
        #expect(visibleMatchIds(viewModel) == [["placed"]])
        viewModel.filter = .all

        #expect(viewModel.rowHolder(for: noBet) === row)
        #expect(row.draftScores == DraftScores(2, nil))
    }

    @Test
    func aSaveOfAFilteredOutMatchCompletesAndUpdatesTheCounts() async throws {
        let repository = ScriptedPendingBetRepository(pages: [PendingBetFixtures.page([noBet, placed])])
        let viewModel = makeViewModel(repository)
        await viewModel.lazyPager.refresh()
        viewModel.filter = .placed

        try await save(viewModel.rowHolder(for: noBet), as: noBet, in: repository)

        #expect(visibleMatchIds(viewModel) == [["no-bet", "placed"]])
        #expect(viewModel.counts.placed == 2)
    }

    private func save(
        _ row: PendingBetRowHolder,
        as match: PoolGamblerBetModel,
        in repository: ScriptedPendingBetRepository
    ) async throws {
        row.setBoth(home: 1, away: 1)
        try await clock.waitForSleepers(1)
        clock.advance(by: PendingBetRowHolder.saveDebounce)
        let submitted = try await repository.nextSubmission()
        repository.completeSaving(submitted, as: match)
        try #require(await eventually { row.rowState == .saved })
    }

    private func visibleMatchIds(_ viewModel: PendingBetListViewModel) -> [[String]] {
        viewModel.visibleSections.map { section in section.matches.map(\.matchId) }
    }
}
