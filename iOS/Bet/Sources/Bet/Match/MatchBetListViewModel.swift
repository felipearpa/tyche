import Foundation
import Core
import UI
import LazyPaging
import DataBet
import ViewingState

public class MatchBetListViewModel: ObservableObject {
    private let getPoolGamblerBetUseCase: GetPoolGamblerBetUseCase
    private let getPoolMatchGamblerBetsUseCase: GetPoolMatchGamblerBetsUseCase
    private let poolId: String
    private let gamblerId: String
    private let matchId: String
    private var pagingSource: LazyPagingCursorSource<PoolGamblerBetModel>!
    @Published var poolGamblerBetState: ViewingState.LoadState<PoolGamblerBetModel> = .idle

    @MainActor
    lazy var lazyPager: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel> = {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 50, prefetchDistance: 5, enablePlaceholders: false),
                pagingSourceFactory: { [pagingSource = self.pagingSource!] in pagingSource }
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

        let pagingSource = LazyPagingCursorSource<PoolGamblerBetModel>(
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
            poolGamblerBetState = .loaded(bet.toPoolGamblerBetModel())
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
