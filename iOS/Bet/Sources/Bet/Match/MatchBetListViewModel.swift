import Core
import DataBet
import Foundation
import LazyPaging
import UI

/// The match screen for one signed-in account, pool, and match. Another context gets a new view
/// model, so an earlier context's responses never reach it.
///
/// It keeps three independent parts:
/// - the heading and privacy gate, from the routed gambler's bet for the match;
/// - the viewer's own prediction, shown while bets are hidden. It always belongs to
///   `viewerAccountId`, never to the routed gambler, who can be another participant;
/// - the ranked participant feed, loaded only once the gate confirms bets are revealed.
///
/// Each request carries a generation; a response from an older request is discarded.
@MainActor
public final class MatchBetListViewModel: ObservableObject {
    private let getPoolGamblerBetUseCase: GetPoolGamblerBetUseCase
    private let getRankedPoolMatchGamblerBetsUseCase: GetRankedPoolMatchGamblerBetsUseCase
    private let poolId: String
    private let routedGamblerId: String
    private let matchId: String
    let viewerAccountId: String

    @Published private(set) var heading = MatchHeadingState.initial
    @Published private(set) var ownBet = OwnBetState.loading
    /// Replaced whenever the gate stops revealing bets, so rows from a revealed period are never
    /// shown again before a fresh first page.
    @Published private(set) var lazyPager: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>

    private var feed: RankedMatchBetsFeed
    private var feedToken = 0
    private var headingGeneration = 0
    private var ownBetGeneration = 0

    public init(
        getPoolGamblerBetUseCase: GetPoolGamblerBetUseCase,
        getRankedPoolMatchGamblerBetsUseCase: GetRankedPoolMatchGamblerBetsUseCase,
        poolId: String,
        gamblerId: String,
        matchId: String,
        viewerAccountId: String
    ) {
        self.getPoolGamblerBetUseCase = getPoolGamblerBetUseCase
        self.getRankedPoolMatchGamblerBetsUseCase = getRankedPoolMatchGamblerBetsUseCase
        self.poolId = poolId
        self.routedGamblerId = gamblerId
        self.matchId = matchId
        self.viewerAccountId = viewerAccountId

        // Placeholders until `makeFeed` can capture `self`.
        let placeholderFeed = RankedMatchBetsFeed(
            query: { _ in .success(CursorPage(items: [], next: nil)) },
            onPredictionsHidden: {},
            onRestartRequested: {}
        )
        self.feed = placeholderFeed
        self.lazyPager = Self.makePager(feed: placeholderFeed)
        replaceFeed()
    }

    /// Entry and reappearance: confirms the gate, then loads the own prediction when bets are
    /// hidden. The participant list loads itself once it is shown.
    func load() async {
        await loadHeading()
        if heading.confirmedPresentation == .hidden {
            await loadOwnBet()
        }
    }

    /// Pull to refresh: rechecks the gate, then refreshes what the confirmed gate shows. The
    /// ranked feed restarts from its first page and replaces the rows when it arrives.
    func refresh() async {
        await loadHeading()
        switch heading.confirmedPresentation {
        case .hidden:
            await loadOwnBet()
        case .betsClosed, .computed:
            await restartFeed()
        case nil:
            break
        }
    }

    /// Requests the heading on its own: entry, retry, and refresh use it. A confirmed heading
    /// stays visible while it runs and after it fails.
    func loadHeading() async {
        headingGeneration += 1
        let generation = headingGeneration
        heading = heading.loading()

        let result = await getPoolGamblerBetUseCase.execute(
            poolId: poolId,
            gamblerId: routedGamblerId,
            matchId: matchId
        )
        guard generation == headingGeneration else { return }

        switch result {
        case .success(let bet):
            let match = bet.toPoolGamblerBetModel()
            heading = heading.loaded(match)
            if routedGamblerId == viewerAccountId {
                ownBetGeneration += 1
                ownBet = OwnBetState(betScore: match.betScore)
            }
        case .failure:
            heading = heading.failed()
        }
        dropRevealedRowsIfHidden()
    }

    /// Requests the viewer's own prediction. When the route already belongs to the viewer, the
    /// heading's bet is the viewer's and no other request is made.
    func loadOwnBet() async {
        guard routedGamblerId != viewerAccountId else {
            if heading.match == nil { await loadHeading() }
            return
        }
        ownBetGeneration += 1
        let generation = ownBetGeneration
        let confirmed = ownBet.isConfirmed
        if !confirmed {
            ownBet = .loading
        }

        let result = await getPoolGamblerBetUseCase.execute(
            poolId: poolId,
            gamblerId: viewerAccountId,
            matchId: matchId
        )
        guard generation == ownBetGeneration else { return }

        switch result {
        case .success(let bet):
            ownBet = OwnBetState(betScore: bet.betScore)
        case .failure(let error) where error.isNotFound:
            // The server has no prediction record for the viewer in this match.
            ownBet = .noBet
        case .failure:
            if !confirmed {
                ownBet = .failed
            }
        }
    }

    /// The list's retry after a failed page. A rejected continuation restarts from the first
    /// page; any other failure repeats the failed request.
    func retryPage(after error: Error) async {
        if error.restartsMatchBetsFromFirstPage {
            await restartFeed()
        } else {
            await lazyPager.retry()
        }
    }

    /// The list's retry after its first page failed.
    func retryFirstPage() async {
        await restartFeed()
    }

    private func restartFeed() async {
        feed.resetAutomaticRestart()
        await feed.waitUntilIdle()
        await lazyPager.refresh()
    }

    // MARK: Gate changes

    private func dropRevealedRowsIfHidden() {
        guard heading.confirmedPresentation == .hidden, feed.generation > 0 else { return }
        replaceFeed()
    }

    private func predictionsHiddenByServer() {
        heading = heading.hiddenByServer()
        replaceFeed()
        Task { await loadOwnBet() }
    }

    /// Starts a new feed and an empty pager. Callbacks of earlier feeds are ignored.
    private func replaceFeed() {
        feedToken += 1
        let token = feedToken
        weak var feed: RankedMatchBetsFeed?
        let newFeed = RankedMatchBetsFeed(
            query: { [getRankedPoolMatchGamblerBetsUseCase, poolId, matchId] next in
                await getRankedPoolMatchGamblerBetsUseCase.execute(poolId: poolId, matchId: matchId, next: next)
                    .map { page in page.map { bet in bet.toPoolGamblerBetModel() } }
            },
            onPredictionsHidden: { [weak self] in
                guard let self, self.feedToken == token else { return }
                self.predictionsHiddenByServer()
            },
            onRestartRequested: { [weak self] in
                guard let self, self.feedToken == token else { return }
                Task {
                    await feed?.waitUntilIdle()
                    await self.lazyPager.refresh()
                }
            }
        )
        feed = newFeed
        self.feed = newFeed
        lazyPager = Self.makePager(feed: newFeed)
    }

    private static func makePager(
        feed: RankedMatchBetsFeed
    ) -> LazyPaging.LazyPagingItems<String, PoolGamblerBetModel> {
        let pagingSource = LazyPagingCursorSource<PoolGamblerBetModel>(
            pagingQuery: { next in await feed.load(next: next) }
        )
        return LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 50, prefetchDistance: 5, enablePlaceholders: false),
                pagingSourceFactory: { pagingSource }
            )
        )
    }
}

private extension OwnBetState {
    var isConfirmed: Bool {
        switch self {
        case .bet, .noBet: true
        case .loading, .failed: false
        }
    }
}

private extension Error {
    var isNotFound: Bool {
        guard let networkError = self as? NetworkError, case .http(code: .notFound) = networkError else {
            return false
        }
        return true
    }
}
