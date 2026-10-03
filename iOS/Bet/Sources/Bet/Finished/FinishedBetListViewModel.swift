import SwiftUI
import UI
import LazyPaging
import DataBet
import DataPool
import Core

public class FinishedBetListViewModel: ObservableObject {
    private let getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase
    private let getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase

    private let gamblerId: String
    private let poolId: String

    private var searchText: String? = nil

    private var pagingSource: LazyPagingCursorSource<PoolGamblerBetModel>!

    /// The authoritative total for this view model's pool and gambler. It is requested on entry,
    /// on pull to refresh, and on summary retry; paging never requests or derives it. A view model
    /// serves one pool and gambler, so another context gets a new view model and never sees this
    /// total.
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
        getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase,
        getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase,
        gamblerId: String,
        poolId: String
    ) {
        self.getFinishedPoolGamblerBetsUseCase = getFinishedPoolGamblerBetsUseCase
        self.getPoolGamblerScoreUseCase = getPoolGamblerScoreUseCase
        self.gamblerId = gamblerId
        self.poolId = poolId

        let pagingSource = LazyPagingCursorSource<PoolGamblerBetModel>(
            pagingQuery: { next in
                await getFinishedPoolGamblerBetsUseCase.execute(
                    poolId: poolId,
                    gamblerId: gamblerId,
                    next: next,
                    searchText: nil
                )
                .map { page in page.map { bet in bet.toPoolGamblerBetModel() } }
                .mapError { error in error.toLocalizedError() }
            }
        )
        self.pagingSource = pagingSource
    }

    func search(_ text: String) {
        searchText = text
        pagingSource.invalidate()
    }

    func refresh() {
        pagingSource.invalidate()
    }

    /// Requests the earned-points total. Entry loading and summary retry use it on its own, so
    /// it never reloads the rows. Confirmed points stay visible while it runs.
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
