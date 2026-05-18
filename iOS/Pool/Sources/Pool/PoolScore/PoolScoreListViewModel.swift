import Foundation
import Core
import UI
import LazyPaging
import DataPool

public class PoolScoreListViewModel: ObservableObject {
    private let getPoolGamblerScoresByGamblerUseCase : GetPoolGamblerScoresByGamblerUseCase
    private let getOpenPoolLayoutsUseCase: GetOpenPoolLayoutsUseCase
    private let joinPoolUrlTemplate: JoinPoolUrlTemplateProvider
    private let gamblerId: String
    private var searchText: String? = nil
    private var pagingSource: LazyPagingCursorSource<PoolGamblerScoreModel>!
    private var poolLayoutsPagingSource: LazyPagingCursorSource<PoolLayoutModel>!

    @MainActor
    lazy var lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerScoreModel> = {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 50, prefetchDistance: 5, enablePlaceholders: false),
                pagingSourceFactory: { [pagingSource = self.pagingSource!] in pagingSource }
            )
        )
    }()

    @MainActor
    lazy var lazyPoolLayouts: LazyPaging.LazyPagingItems<String, PoolLayoutModel> = {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 50, prefetchDistance: 5, enablePlaceholders: false),
                pagingSourceFactory: { [poolLayoutsPagingSource = self.poolLayoutsPagingSource!] in poolLayoutsPagingSource }
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

        self.pagingSource = LazyPagingCursorSource(
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

        self.poolLayoutsPagingSource = LazyPagingCursorSource(
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
