import Foundation
import Core
import UI
import LazyPaging
import DataPool

public class GamblerScoreListViewModel: ObservableObject {
    private let getPoolGamblerScoresByPoolUseCase : GetPoolGamblerScoresByPoolUseCase
    let gamblerId: String
    private let poolId: String
    private var searchText: String? = nil
    private var pagingSource: LazyPagingCursorSource<PoolGamblerScoreModel>!

    @MainActor
    lazy var lazyPager: LazyPaging.LazyPagingItems<String, PoolGamblerScoreModel> = {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 50, prefetchDistance: 5, enablePlaceholders: false),
                pagingSourceFactory: { [pagingSource = self.pagingSource!] in pagingSource }
            )
        )
    }()

    public init(
        getPoolGamblerScoresByPoolUseCase : GetPoolGamblerScoresByPoolUseCase,
        gamblerId: String,
        poolId: String
    ) {
        self.getPoolGamblerScoresByPoolUseCase = getPoolGamblerScoresByPoolUseCase
        self.gamblerId = gamblerId
        self.poolId = poolId

        self.pagingSource = LazyPagingCursorSource(
            pagingQuery: { [unowned self] next in
                await getPoolGamblerScoresByPoolUseCase.execute(
                    poolId: self.poolId,
                    next: next,
                    searchText: self.searchText
                )
                .map { page in page.map { poolGamblerScore in poolGamblerScore.toPoolGamblerScoreModel() }}
                .mapError { error in error.toLocalizedError() }
            }
        )
    }

    func search(_ newSearchText: String) {
        searchText = newSearchText
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
