import Foundation
import Core
import UI

public class PoolScoreListViewModel: ObservableObject {
    private let getPoolGamblerScoresByGamblerUseCase : GetPoolGamblerScoresByGamblerUseCase
    
    private let gamblerId: String
    
    private var searchText: String? = nil
    
    private var pagingSource: PoolGamblerScorePagingSource!
    
    @MainActor @Published var pagingData: PagingData<String, PoolGamblerScoreModel>!
    
    @MainActor
    public init(
        getPoolGamblerScoresByGamblerUseCase : GetPoolGamblerScoresByGamblerUseCase,
        gamblerId: String
    ) {
        self.getPoolGamblerScoresByGamblerUseCase = getPoolGamblerScoresByGamblerUseCase
        self.gamblerId = gamblerId
        
        self.pagingSource = PoolGamblerScorePagingSource(
            pagingQuery: { [unowned self] next in
                await getPoolGamblerScoresByGamblerUseCase.execute(
                    gamblerId: self.gamblerId,
                    next: next,
                    searchText: self.searchText
                )
                .map { page in page.map { poolGamblerScore in poolGamblerScore.toPoolGamblerScoreModel() }}
                .mapError { error in error.toLocalizedError() }
            }
        )
        
        self.pagingData = PagingData(
            pagingConfig: PagingConfig(prefetchDistance: 5),
            pagingSourceFactory: self.pagingSource
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
