import Foundation
import Core
import UI
import DataBet

public class BetsTimelineViewModel: ObservableObject {
    private let getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase

    private let poolId: String

    private let gamblerId: String

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
        getGamblerBetsTimelineUseCase: GetGamblerBetsTimelineUseCase,
        poolId: String,
        gamblerId: String
    ) {
        self.getGamblerBetsTimelineUseCase = getGamblerBetsTimelineUseCase
        self.poolId = poolId
        self.gamblerId = gamblerId

        let pagingSource = PoolGamblerBetPagingSource(
            pagingQuery: { next in
                await getGamblerBetsTimelineUseCase.execute(
                    poolId: poolId,
                    gamblerId: gamblerId,
                    next: next
                )
                .map { page in page.map { poolGamblerBet in poolGamblerBet.toPoolGamblerBetModel() }}
                .mapError { error in error.toLocalizedError() }
            }
        )
        self.pagingSource = pagingSource
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
