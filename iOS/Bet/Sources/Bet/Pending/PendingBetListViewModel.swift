import Foundation
import Core
import UI
import LazyPaging
import DataBet

public class PendingBetListViewModel: ObservableObject {
    private let getPoolGamblerBetsUseCase : GetPendingPoolGamblerBetsUseCase

    private let gamblerId: String

    private let poolId: String

    private var searchText: String? = nil

    private var pagingSource: LazyPagingCursorSource<PoolGamblerBetModel>!

    @MainActor
    lazy var lazyPager: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel> = {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: { [pagingSource = self.pagingSource!] in pagingSource }
            )
        )
    }()

    public init(
        getPoolGamblerBetsUseCase : GetPendingPoolGamblerBetsUseCase,
        gamblerId: String,
        poolId: String
    ) {
        self.getPoolGamblerBetsUseCase = getPoolGamblerBetsUseCase
        self.gamblerId = gamblerId
        self.poolId = poolId

        let pagingSource = LazyPagingCursorSource<PoolGamblerBetModel>(
            pagingQuery: { next in
                await getPoolGamblerBetsUseCase.execute(
                    poolId: poolId,
                    gamblerId: gamblerId,
                    next: next,
                    searchText: nil
                )
                .map { page in page.map { poolGamblerBet in poolGamblerBet.toPoolGamblerBetModel() }}
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
}

private extension Error {
    func toLocalizedError() -> Error {
        if let networkError = self as? NetworkError {
            return networkError.toNetworkLocalizedError()
        }

        return UnknownLocalizedError()
    }
}
