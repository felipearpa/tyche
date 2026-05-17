import Foundation
import DataPool
import UI
import LazyPaging
import Core

public class PoolFromLayoutCreatorStepOneViewModel : ObservableObject {
    private let getOpenPoolLayoutsUseCase: GetOpenPoolLayoutsUseCase

    private var pagingSource: LazyPagingCursorSource<PoolLayoutModel>!

    private var searchText: String? = nil

    @MainActor
    lazy var lazyPager: LazyPaging.LazyPagingItems<String, PoolLayoutModel> = {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 50, prefetchDistance: 5, enablePlaceholders: false),
                pagingSourceFactory: { [pagingSource = self.pagingSource!] in pagingSource }
            )
        )
    }()

    public init(getOpenPoolLayoutsUseCase: GetOpenPoolLayoutsUseCase) {
        self.getOpenPoolLayoutsUseCase = getOpenPoolLayoutsUseCase

        self.pagingSource = LazyPagingCursorSource<PoolLayoutModel>(
            pagingQuery: { [unowned self] next in
                return await self.getOpenPoolLayoutsUseCase.execute(
                    next: next,
                    searchText: self.searchText
                )
                .map { page in page.map { poolLayout in poolLayout.toPoolLayoutModel() }}
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
