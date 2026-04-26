import Foundation
import Core
import UI
import DataPool

public class PoolScoreListViewModel: ObservableObject {
    private let getPoolGamblerScoresByGamblerUseCase : GetPoolGamblerScoresByGamblerUseCase
    private let getOpenPoolLayoutsUseCase: GetOpenPoolLayoutsUseCase

    private let joinPoolUrlTemplate: JoinPoolUrlTemplateProvider

    private let gamblerId: String

    private var searchText: String? = nil

    private var pagingSource: PoolGamblerScorePagingSource!
    private var poolLayoutsPagingSource: OpenPoolLayoutPagingSource!

    lazy var lazyPagingItems: LazyPagingItems<String, PoolGamblerScoreModel> = {
        LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: { self.pagingSource }
            )
        )
    }()

    lazy var lazyPoolLayouts: LazyPagingItems<String, PoolLayoutModel> = {
        LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: { self.poolLayoutsPagingSource }
            )
        )
    }()

    public init(
        getPoolGamblerScoresByGamblerUseCase : GetPoolGamblerScoresByGamblerUseCase,
        getOpenPoolLayoutsUseCase: GetOpenPoolLayoutsUseCase,
        joinPoolUrlTemplate: JoinPoolUrlTemplateProvider,
        gamblerId: String
    ) {
        self.getPoolGamblerScoresByGamblerUseCase = getPoolGamblerScoresByGamblerUseCase
        self.getOpenPoolLayoutsUseCase = getOpenPoolLayoutsUseCase
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

        self.poolLayoutsPagingSource = OpenPoolLayoutPagingSource(
            pagingQuery: { [unowned self] next in
                await self.getOpenPoolLayoutsUseCase.execute(next: next, searchText: nil)
                    .map { page in page.map { poolLayout in poolLayout.toPoolLayoutModel() }}
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
