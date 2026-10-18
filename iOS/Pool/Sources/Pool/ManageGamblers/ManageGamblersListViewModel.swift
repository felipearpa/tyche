import Foundation
import Core
import UI
import LazyPaging
import DataPool
import ViewingState

@MainActor
public class ManageGamblersListViewModel: ObservableObject {
    private let getPoolMembersUseCase: GetPoolMembersUseCase
    private let removeGamblerUseCase: RemoveGamblerUseCase
    private let getPoolUseCase: GetPoolUseCase
    let poolId: String
    private var pagingSource: LazyPagingCursorSource<PoolMemberModel>!

    /// One holder per gambler the owner acted on, keyed by gambler ID. Rows without a holder are
    /// idle. A holder is dropped once its work is done: after a removal's exit animation, when a
    /// failure is dismissed, and, for failures, on refresh.
    @Published private(set) var rowHolders: [String: GamblerRowHolder] = [:]

    /// Gamblers removed on this screen. They stay hidden, and out of the shown count, until a
    /// refresh's first page, which no longer contains them, has landed.
    @Published private(set) var removedGamblerIds: Set<String> = []

    /// The pool's gambler count as the server last reported it. `.failure` hides the count line
    /// until the next refresh asks again.
    @Published private(set) var gamblerCount: ViewingState.LoadState<Int?> = .loading

    /// The removals the server had already applied when the current `gamblerCount` was requested.
    private var countReflectedRemovals: Set<String> = []
    /// Gamblers whose row shows a failed removal; a refresh returns them to idle.
    @Published private(set) var failedGamblerIds: Set<String> = []
    private var countRequest = UUID()

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
        getPoolUseCase: GetPoolUseCase,
        poolId: String
    ) {
        self.getPoolMembersUseCase = getPoolMembersUseCase
        self.removeGamblerUseCase = removeGamblerUseCase
        self.getPoolUseCase = getPoolUseCase
        self.poolId = poolId

        self.pagingSource = LazyPagingCursorSource(
            pagingQuery: { [unowned self] next in
                await getPoolMembersUseCase.execute(poolId: self.poolId, next: next)
                    .map { page in page.map { member in member.toPoolMemberModel() } }
                    .mapError { error in error.toLocalizedError() }
            }
        )

        Task {
            await self.loadGamblerCount()
        }
    }

    /// The count line's value: the server's count less the removals made here that it did not
    /// reflect yet.
    var shownGamblerCount: ViewingState.LoadState<Int?> {
        let pendingRemovals = removedGamblerIds.subtracting(countReflectedRemovals).count
        return gamblerCount.map { count in count.map { $0 - pendingRemovals } }
    }

    /// Starts (or, from a failure, restarts) the confirmed removal of `member`.
    func remove(_ member: PoolMemberModel) {
        holder(for: member).remove()
    }

    /// A pull-to-refresh: returns failed rows to idle, asks for the count again, and reloads the
    /// members. Gamblers removed before the refresh started are released once its first page has
    /// landed; if the reload fails they stay hidden.
    func refresh() async {
        rowHolders = rowHolders.filter { !failedGamblerIds.contains($0.key) }
        failedGamblerIds = []
        let removalsBeforeRefresh = removedGamblerIds

        async let count: Void = loadGamblerCount()
        await lazyPager.refresh()
        if lazyPager.loadState.refresh.isNotLoading {
            removedGamblerIds.subtract(removalsBeforeRefresh)
        }
        await count
    }

    private func holder(for member: PoolMemberModel) -> GamblerRowHolder {
        if let holder = rowHolders[member.gamblerId] {
            return holder
        }

        let holder = GamblerRowHolder(
            member: member,
            poolId: poolId,
            removeGamblerUseCase: removeGamblerUseCase,
            outcomes: self
        )
        rowHolders[member.gamblerId] = holder
        return holder
    }

    private func loadGamblerCount() async {
        // A newer request supersedes an unanswered one, so a slow answer cannot overwrite it.
        let request = UUID()
        countRequest = request
        let reflectedRemovals = removedGamblerIds
        gamblerCount = .loading

        let result = await getPoolUseCase.execute(poolId: poolId)
        guard countRequest == request else { return }

        countReflectedRemovals = reflectedRemovals
        switch result {
        case .success(let pool):
            gamblerCount = .loaded(pool.gamblerCount)
        case .failure(let error):
            gamblerCount = .failure(error.toLocalizedError())
        }
    }
}

extension ManageGamblersListViewModel: GamblerRowOutcomes {
    func removed(_ gamblerId: String) {
        failedGamblerIds.remove(gamblerId)
        removedGamblerIds.insert(gamblerId)

        // The row leaves with an exit animation that still draws the holder's state, so the
        // holder goes once that animation has had time to finish.
        Task { [weak self] in
            try? await Task.sleep(nanoseconds: gamblerRowExitNanoseconds)
            self?.rowHolders[gamblerId] = nil
        }
    }

    func failed(_ gamblerId: String) {
        failedGamblerIds.insert(gamblerId)
    }

    func failureCleared(_ gamblerId: String) {
        failedGamblerIds.remove(gamblerId)
        // A retry leaves the holder removing; only a dismissed failure has no work left.
        guard let holder = rowHolders[gamblerId], holder.removal.isIdle() else { return }
        rowHolders[gamblerId] = nil
    }
}

/// How long a removed row's exit animation runs (SwiftUI's default animation).
let gamblerRowExitDuration: TimeInterval = 0.35
private let gamblerRowExitNanoseconds = UInt64(gamblerRowExitDuration * 1_000_000_000)

private extension Error {
    func toLocalizedError() -> Error {
        if let networkError = self as? NetworkError {
            return networkError.toNetworkLocalizedError()
        }

        return UnknownLocalizedError()
    }
}
