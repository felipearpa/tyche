import Foundation
import Core
import UI
import LazyPaging
import DataPool
import ViewingState

public class ManageGamblersListViewModel: ObservableObject {
    private let getPoolMembersUseCase: GetPoolMembersUseCase
    private let removeGamblerUseCase: RemoveGamblerUseCase
    let poolId: String
    private var pagingSource: LazyPagingCursorSource<PoolMemberModel>!

    /// Per-gambler removal state. Every row starts in `.idle(member)`; a removal overrides that
    /// original with `.mutating` → `.mutated` (row gone) or `.failure` (revert + show the banner).
    /// Only non-idle entries are stored — `mutationState(for:)` falls back to `.idle(member)`.
    @Published private var removalStates: [String: MutationState<PoolMemberModel>] = [:]

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
            override(member.gamblerId, with: .mutating(member))

            let result = await removeGamblerUseCase.execute(poolId: poolId, gamblerId: member.gamblerId)

            switch result {
            case .success:
                override(member.gamblerId, with: .mutated(member))
            case .failure(let error):
                override(member.gamblerId, with: .failure(value: member, error: error))
            }
        }
    }

    @MainActor
    func dismissFailure() {
        removalStates = removalStates.filter { !$0.value.isFailure() }
    }

    /// The current removal state for a row — `.idle(member)` until a removal overrides it.
    func mutationState(for member: PoolMemberModel) -> MutationState<PoolMemberModel> {
        removalStates[member.gamblerId] ?? .idle(member)
    }

    var failedGambler: PoolMemberModel? {
        removalStates.values.first(where: { $0.isFailure() })?.activeValue()
    }

    @MainActor
    private func override(_ gamblerId: String, with state: MutationState<PoolMemberModel>) {
        removalStates = removalStates.merging([gamblerId: state]) { _, updated in updated }
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
