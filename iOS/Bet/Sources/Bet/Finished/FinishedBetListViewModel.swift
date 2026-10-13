import SwiftUI
import UI
import LazyPaging
import DataBet
import DataPool
import Core

/// The signed-in gambler's History in one pool: the grouped feed of finished kickoff groups and
/// the authoritative pool total. The two load independently, so either can succeed while the
/// other is loading or has failed. A view model serves one pool and gambler; another context gets
/// a new view model, so a previous context's groups or total never reach it.
@MainActor
public class FinishedBetListViewModel: ObservableObject {
    private let getPoolGamblerScoreUseCase: GetPoolGamblerScoreUseCase

    private let gamblerId: String
    private let poolId: String

    private var feed: KickoffGroupFeed!
    private var pagingSource: LazyPagingCursorSource<KickoffGroupModel>!

    /// The authoritative total for this view model's pool and gambler. It is requested on entry,
    /// on pull to refresh, and on summary retry; paging never requests or derives it.
    @Published private(set) var pointsSummary = HistoryPointsSummaryState.initial

    /// Identifies the latest summary request; a response from an older request is discarded.
    private var pointsSummaryGeneration = 0

    lazy var lazyPager: LazyPaging.LazyPagingItems<String, KickoffGroupModel> = {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 20, prefetchDistance: 3, enablePlaceholders: false),
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
        self.getPoolGamblerScoreUseCase = getPoolGamblerScoreUseCase
        self.gamblerId = gamblerId
        self.poolId = poolId

        let feed = KickoffGroupFeed(
            query: { next in
                await getFinishedPoolGamblerBetsUseCase.execute(poolId: poolId, gamblerId: gamblerId, next: next)
            },
            onRestartRequested: { [weak self] in
                Task { await self?.restartAfterRejectedCursor() }
            }
        )
        self.feed = feed
        self.pagingSource = LazyPagingCursorSource<KickoffGroupModel>(
            pagingQuery: { next in await feed.load(next: next) }
        )
    }

    /// Reloads the groups from the first page; the total is left as it is.
    func refresh() {
        feed.resetAutomaticRestart()
        pagingSource.invalidate()
    }

    /// The list's retry after a failed page. A rejected continuation restarts from the first
    /// page; any other failure repeats the failed request.
    func retryPage(after error: Error) async {
        if error.restartsKickoffGroupFeed {
            await restartFromFirstPage()
        } else {
            await lazyPager.retry()
        }
    }

    /// The list's retry after its first page failed. It requests only the groups.
    func retryFirstPage() async {
        await restartFromFirstPage()
    }

    /// Requests the earned-points total. Entry loading and summary retry use it on its own, so
    /// it never reloads the rows. Confirmed points stay visible while it runs.
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

    /// Pull to refresh: reloads the groups and the total together and returns when both finish,
    /// so the refresh indicator covers whichever takes longer. Each result is published as soon
    /// as it arrives, and one failing does not hold back the other.
    func refreshListAndPointsSummary() async {
        async let list: Void = restartFromFirstPage()
        async let summary: Void = loadPointsSummary()
        _ = await (list, summary)
    }

    private func restartFromFirstPage() async {
        feed.resetAutomaticRestart()
        await feed.waitUntilIdle()
        await lazyPager.refresh()
    }

    /// The automatic restart after a rejected continuation: the loaded groups stay visible until
    /// the first page replaces them.
    private func restartAfterRejectedCursor() async {
        await feed.waitUntilIdle()
        await lazyPager.refresh()
    }
}
