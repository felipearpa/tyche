import Foundation
import Core
import UI
import LazyPaging
import DataBet
import DataPool

/// Another gambler's Timeline in one pool: the paged feed of their bets and their authoritative
/// pool total. The two load independently, so either can succeed while the other is loading or
/// has failed. A view model serves one pool and gambler; another context gets a new view model,
/// so a previous context's rows or total never reach it.
public class BetTimelineListViewModel: ObservableObject {
    private let getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase
    private let getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase
    private let poolId: String
    private let gamblerId: String
    private var pagingSource: LazyPagingCursorSource<PoolGamblerBetModel>!

    /// The selected gambler's total in this pool, as the server reports it. It is requested on
    /// entry, on pull to refresh, and on summary retry; paging never requests or derives it.
    @MainActor @Published private(set) var pointsSummary = HistoryPointsSummaryState.initial

    /// Identifies the latest summary request; a response from an older request is discarded.
    @MainActor private var pointsSummaryGeneration = 0

    @MainActor
    lazy var lazyPager: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel> = {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 50, prefetchDistance: 5, enablePlaceholders: false),
                pagingSourceFactory: { [pagingSource = self.pagingSource!] in pagingSource }
            )
        )
    }()

    public init(
        getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase,
        getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase,
        poolId: String,
        gamblerId: String
    ) {
        self.getGamblerBetsTimelineUseCase = getGamblerBetsTimelineUseCase
        self.getPoolGamblerScoreUseCase = getPoolGamblerScoreUseCase
        self.poolId = poolId
        self.gamblerId = gamblerId

        let pagingSource = LazyPagingCursorSource<PoolGamblerBetModel>(
            pagingQuery: { next in
                await getGamblerBetsTimelineUseCase.execute(
                    poolId: poolId,
                    gamblerId: gamblerId,
                    next: next
                )
                .map { page in page.map { poolGamblerBet in poolGamblerBet.toPoolGamblerBetModel() }}
                .mapError { error in error.toLocalizedError() }
            }
        )
        self.pagingSource = pagingSource
    }

    /// Reloads the rows from the first page; the total is left as it is.
    func refresh() {
        pagingSource.invalidate()
    }

    /// Requests the total on its own. Entry loading and summary retry use it, so it never
    /// reloads the rows. Confirmed points stay visible while it runs.
    @MainActor
    func loadPointsSummary() async {
        pointsSummaryGeneration += 1
        let generation = pointsSummaryGeneration
        pointsSummary = pointsSummary.loading()

        let result = await getPoolGamblerScoreUseCase.execute(poolId: poolId, gamblerId: gamblerId)

        guard generation == pointsSummaryGeneration else { return }
        switch result {
        case .success(let score):
            pointsSummary = pointsSummary.loaded(HistoryPoints(score: score.score))
        case .failure:
            pointsSummary = pointsSummary.failed()
        }
    }

    /// Pull to refresh: reloads the rows and the total together and returns when both finish,
    /// so the refresh indicator covers whichever takes longer. Each result is published as soon
    /// as it arrives, and one failing does not hold back the other.
    @MainActor
    func refreshListAndPointsSummary() async {
        async let list: Void = lazyPager.refresh()
        async let summary: Void = loadPointsSummary()
        _ = await (list, summary)
    }
}

private extension Error {
    func toLocalizedError() -> Error {
        if let networkError = self as? NetworkError {
            return networkError.toNetworkLocalizedError()
        }

        return UnknownLocalizedError()
    }
}
