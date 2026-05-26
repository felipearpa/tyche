import Foundation
import Core
import UI
import LazyPaging
import DataPool

public class ManageGamblersListViewModel: ObservableObject {
    private let getPoolMembersUseCase: GetPoolMembersUseCase
    private let removeGamblerUseCase: RemoveGamblerUseCase
    let poolId: String
    private var pagingSource: LazyPagingCursorSource<PoolMemberModel>!

    @Published var deletingGamblerIds: Set<String> = []
    @Published var removedGamblerIds: Set<String> = []
    @Published var failedGambler: PoolMemberModel? = nil

    @MainActor
    lazy var lazyPager: LazyPaging.LazyPagingItems<String, PoolMemberModel> = {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 50, prefetchDistance: 5, enablePlaceholders: false),
                pagingSourceFactory: { [pagingSource = self.pagingSource!] in pagingSource }
            )
        )
    }()

    public init(
        getPoolMembersUseCase: GetPoolMembersUseCase,
        removeGamblerUseCase: RemoveGamblerUseCase,
        poolId: String
    ) {
        self.getPoolMembersUseCase = getPoolMembersUseCase
        self.removeGamblerUseCase = removeGamblerUseCase
        self.poolId = poolId

        self.pagingSource = LazyPagingCursorSource(
            pagingQuery: { [unowned self] next in
                await getPoolMembersUseCase.execute(poolId: self.poolId, next: next)
                    .map { page in page.map { member in member.toPoolMemberModel() } }
                    .mapError { error in error.toLocalizedError() }
            }
        )
    }

    @MainActor
    func remove(_ member: PoolMemberModel) {
        Task {
            failedGambler = nil
            deletingGamblerIds.insert(member.gamblerId)

            let result = await removeGamblerUseCase.execute(poolId: poolId, gamblerId: member.gamblerId)

            deletingGamblerIds.remove(member.gamblerId)

            switch result {
            case .success:
                removedGamblerIds.insert(member.gamblerId)
            case .failure:
                failedGambler = member
            }
        }
    }

    @MainActor
    func dismissFailure() {
        failedGambler = nil
    }

    func isDeleting(_ member: PoolMemberModel) -> Bool {
        deletingGamblerIds.contains(member.gamblerId)
    }

    func isRemoved(_ member: PoolMemberModel) -> Bool {
        removedGamblerIds.contains(member.gamblerId)
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
