import Foundation
import Core
import UI
import DataBet

public class MatchBetListViewModel: ObservableObject {
    private let getPoolMatchGamblerBetsUseCase: GetPoolMatchGamblerBetsUseCase

    private let poolId: String

    private let matchId: String

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
        getPoolMatchGamblerBetsUseCase: GetPoolMatchGamblerBetsUseCase,
        poolId: String,
        matchId: String
    ) {
        self.getPoolMatchGamblerBetsUseCase = getPoolMatchGamblerBetsUseCase
        self.poolId = poolId
        self.matchId = matchId

        let pagingSource = PoolGamblerBetPagingSource(
            pagingQuery: { next in
                await getPoolMatchGamblerBetsUseCase.execute(
                    poolId: poolId,
                    matchId: matchId,
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
