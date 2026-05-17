import SwiftUI
import UI
import LazyPaging
import DataBet
import Core

public class FinishedBetListViewModel: ObservableObject {
    private let getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase

    private let gamblerId: String
    private let poolId: String

    private var searchText: String? = nil

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
        getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase,
        gamblerId: String,
        poolId: String
    ) {
        self.getFinishedPoolGamblerBetsUseCase = getFinishedPoolGamblerBetsUseCase
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
}

private extension Error {
    func toLocalizedError() -> Error {
        if let networkError = self as? NetworkError {
            return networkError.toNetworkLocalizedError()
        }
        return UnknownLocalizedError()
    }
}
