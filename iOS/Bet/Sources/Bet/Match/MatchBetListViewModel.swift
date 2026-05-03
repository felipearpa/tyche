import Foundation
import Core
import UI
import DataBet

public class MatchBetListViewModel: ObservableObject {
    private let getPoolGamblerBetUseCase: GetPoolGamblerBetUseCase
    private let getPoolMatchGamblerBetsUseCase: GetPoolMatchGamblerBetsUseCase

    private let poolId: String
    private let gamblerId: String
    private let matchId: String

    private var pagingSource: PoolGamblerBetPagingSource!

    @Published var poolGamblerBetState: LoadableViewState<PoolGamblerBetModel> = .initial

    lazy var lazyPager: LazyPagingItems<String, PoolGamblerBetModel> = {
        LazyPagingItems(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: { self.pagingSource }
            )
        )
    }()

    public init(
        getPoolGamblerBetUseCase: GetPoolGamblerBetUseCase,
        getPoolMatchGamblerBetsUseCase: GetPoolMatchGamblerBetsUseCase,
        poolId: String,
        gamblerId: String,
        matchId: String
    ) {
        self.getPoolGamblerBetUseCase = getPoolGamblerBetUseCase
        self.getPoolMatchGamblerBetsUseCase = getPoolMatchGamblerBetsUseCase
        self.poolId = poolId
        self.gamblerId = gamblerId
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

    @MainActor
    func loadPoolGamblerBet() async {
        poolGamblerBetState = .loading
        let result = await getPoolGamblerBetUseCase.execute(
            poolId: poolId,
            gamblerId: gamblerId,
            matchId: matchId
        )
        switch result {
        case .success(let bet):
            poolGamblerBetState = .success(bet.toPoolGamblerBetModel())
        case .failure(let error):
            poolGamblerBetState = .failure(error)
        }
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
