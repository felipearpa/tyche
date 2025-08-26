import SwiftUI
import UI
import DataBet
import Core

public class FinishedBetListViewModel: ObservableObject {
    private let getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase
    
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
        getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase,
        gamblerId: String,
        poolId: String
    ) {
        self.getFinishedPoolGamblerBetsUseCase = getFinishedPoolGamblerBetsUseCase
        self.gamblerId = gamblerId
        self.poolId = poolId
        
        let pagingSource = PoolGamblerBetPagingSource(
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
//        lazyPager.refresh()
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
