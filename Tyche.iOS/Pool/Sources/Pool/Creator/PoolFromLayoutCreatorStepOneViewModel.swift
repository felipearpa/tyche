import Foundation
import DataPool
import UI
import Core

public class PoolFromLayoutCreatorStepOneViewModel : ObservableObject {
    private let getOpenPoolLayoutsUseCase: GetOpenPoolLayoutsUseCase

    private var pagingSource: OpenPoolLayoutPagingSource!

    private var searchText: String? = nil

    lazy var lazyPager: LazyPager<String, PoolLayoutModel> = {
        LazyPager(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: self.pagingSource
            )
        )
    }()

    public init(getOpenPoolLayoutsUseCase: GetOpenPoolLayoutsUseCase) {
        self.getOpenPoolLayoutsUseCase = getOpenPoolLayoutsUseCase

        self.pagingSource = OpenPoolLayoutPagingSource(
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
