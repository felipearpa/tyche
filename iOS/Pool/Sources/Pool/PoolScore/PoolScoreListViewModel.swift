import Foundation
import Core
import UI
import DataPool

public class PoolScoreListViewModel: ObservableObject {
    private let getPoolGamblerScoresByGamblerUseCase : GetPoolGamblerScoresByGamblerUseCase

    private let joinPoolUrlTemplate: JoinPoolUrlTemplateProvider

    private let gamblerId: String
    
    private var searchText: String? = nil
    
    private var pagingSource: PoolGamblerScorePagingSource!
    
    lazy var lazyPagingItems: LazyPagingItems<String, PoolGamblerScoreModel> = {
        LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: { self.pagingSource }
            )
        )
    }()

    public init(
        getPoolGamblerScoresByGamblerUseCase : GetPoolGamblerScoresByGamblerUseCase,
        joinPoolUrlTemplate: JoinPoolUrlTemplateProvider,
        gamblerId: String
    ) {
        self.getPoolGamblerScoresByGamblerUseCase = getPoolGamblerScoresByGamblerUseCase
        self.joinPoolUrlTemplate = joinPoolUrlTemplate
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
    }

    public func refresh() {
        pagingSource.invalidate()
    }

    func search(_ newSearchText: String) {
        searchText = newSearchText
        pagingSource.invalidate()
    }

    func createUrlForJoining(poolId: String) -> String {
        String(format: joinPoolUrlTemplate(), poolId)
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
