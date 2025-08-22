import Foundation
import Core
import UI
import DataPool

public class GamblerScoreListViewModel: ObservableObject {
    private let getPoolGamblerScoresByPoolUseCase : GetPoolGamblerScoresByPoolUseCase
    let gamblerId: String
    private let poolId: String
    private var searchText: String? = nil
    private var pagingSource: PoolGamblerScorePagingSource!
    
    lazy var lazyPager: LazyPagingItems<String, PoolGamblerScoreModel> = {
        LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: { self.pagingSource }
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
        
        self.pagingSource = PoolGamblerScorePagingSource(
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
