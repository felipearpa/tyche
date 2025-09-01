import Foundation
import Core
import UI
import DataBet

public class PendingBetListViewModel: ObservableObject {
    private let getPoolGamblerBetsUseCase : GetPendingPoolGamblerBetsUseCase
    
    private let gamblerId: String
    
    private let poolId: String
    
    private var searchText: String? = nil
    
    private var pagingSource: PoolGamblerBetPagingSource!

    lazy var lazyPager: LazyPagingItems<String, PoolGamblerBetModel> = {
        LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: { self.pagingSource }
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
        
        let pagingSource = PoolGamblerBetPagingSource(
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
