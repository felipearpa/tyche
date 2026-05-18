import Foundation
import Core
import UI
import LazyPaging
import DataBet

public class BetTimelineListViewModel: ObservableObject {
    private let getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase
    private let poolId: String
    private let gamblerId: String
    private var pagingSource: LazyPagingCursorSource<PoolGamblerBetModel>!

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
        poolId: String,
        gamblerId: String
    ) {
        self.getGamblerBetsTimelineUseCase = getGamblerBetsTimelineUseCase
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

    func refresh() {
        pagingSource.invalidate()
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
