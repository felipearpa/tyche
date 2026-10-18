import Testing
import Core
import DataPool
import ViewingState
@testable import Pool

@MainActor
struct ManageGamblersListViewModelTests {
    private let jaime = poolMemberModel("jaime")
    private let santiago = poolMemberModel("santiago")

    private let memberRepository: ScriptedPoolMemberRepository
    private let poolRepository: ScriptedPoolRepository

    init() {
        memberRepository = ScriptedPoolMemberRepository(members: [poolMember("owner", isOwner: true), poolMember("jaime"), poolMember("santiago")])
        poolRepository = ScriptedPoolRepository(autoResponse: .success(poolWith(gamblerCount: 3)))
    }

    private func makeViewModel() -> ManageGamblersListViewModel {
        ManageGamblersListViewModel(
            getPoolMembersUseCase: GetPoolMembersUseCase(poolMemberRepository: memberRepository),
            removeGamblerUseCase: RemoveGamblerUseCase(poolMemberRepository: memberRepository),
            getPoolUseCase: GetPoolUseCase(poolRepository: poolRepository),
            poolId: testPoolId
        )
    }

    // MARK: Row holders

    @Test
    func aHolderIsCreatedOnTheFirstRemoveAndReusedAfterwards() async throws {
        let viewModel = makeViewModel()
        #expect(viewModel.rowHolders.isEmpty)

        viewModel.remove(santiago)
        let holder = viewModel.rowHolders["santiago"]
        #expect(holder != nil)
        #expect(viewModel.rowHolders.count == 1)

        try await failRemoval(of: santiago, in: viewModel)
        viewModel.remove(santiago)

        #expect(viewModel.rowHolders["santiago"] === holder)
        #expect(await eventually { memberRepository.removals.calls == 2 })
        #expect(holder?.removal.isMutating() == true)
    }

    @Test
    func aSuccessfulRemovalLowersTheCountAndDropsTheHolderAfterTheExit() async throws {
        let viewModel = makeViewModel()
        #expect(await eventually { viewModel.shownGamblerCount == .loaded(3) })

        viewModel.remove(santiago)
        try #require(await eventually { memberRepository.removals.pendingCount == 1 })
        memberRepository.removals.answer(.success(()))

        #expect(await eventually { viewModel.removedGamblerIds == ["santiago"] })
        #expect(viewModel.shownGamblerCount == .loaded(2))
        #expect(viewModel.rowHolders["santiago"]?.removal.isMutated() == true)
        #expect(await eventually { viewModel.rowHolders["santiago"] == nil })
        #expect(viewModel.removedGamblerIds == ["santiago"])
    }

    @Test
    func dismissingAFailureDropsTheHolderAndKeepsTheCount() async throws {
        let viewModel = makeViewModel()
        #expect(await eventually { viewModel.shownGamblerCount == .loaded(3) })
        try await failRemoval(of: santiago, in: viewModel)
        #expect(viewModel.failedGamblerIds == ["santiago"])

        viewModel.rowHolders["santiago"]?.dismissFailure()

        #expect(viewModel.rowHolders.isEmpty)
        #expect(viewModel.failedGamblerIds.isEmpty)
        #expect(viewModel.shownGamblerCount == .loaded(3))
        #expect(memberRepository.removals.calls == 1)
    }

    @Test
    func retryingAFailureKeepsTheHolder() async throws {
        let viewModel = makeViewModel()
        try await failRemoval(of: santiago, in: viewModel)
        let holder = viewModel.rowHolders["santiago"]

        holder?.retry()

        #expect(viewModel.rowHolders["santiago"] === holder)
        #expect(viewModel.failedGamblerIds.isEmpty)
        #expect(holder?.removal.isMutating() == true)
    }

    @Test
    func refreshDropsFailedHoldersAndKeepsRemovalsInFlight() async throws {
        let viewModel = makeViewModel()
        try await failRemoval(of: santiago, in: viewModel)
        viewModel.remove(jaime)
        try #require(await eventually { memberRepository.removals.pendingCount == 1 })

        await viewModel.refresh()

        #expect(viewModel.rowHolders["santiago"] == nil)
        #expect(viewModel.failedGamblerIds.isEmpty)
        #expect(viewModel.rowHolders["jaime"]?.removal.isMutating() == true)
        memberRepository.removals.answer(.success(()))
        #expect(await eventually { viewModel.removedGamblerIds == ["jaime"] })
    }

    // MARK: Refresh pruning

    @Test
    func removedGamblersStayHiddenUntilTheRefreshedFirstPageLands() async throws {
        let viewModel = makeViewModel()
        await viewModel.lazyPager.refresh()
        try await removeSuccessfully(santiago, in: viewModel)

        memberRepository.members.autoResponse = nil
        let refresh = Task { await viewModel.refresh() }
        try #require(await eventually { memberRepository.members.pendingCount == 1 })
        #expect(viewModel.removedGamblerIds == ["santiago"])

        memberRepository.members.answer(.success(CursorPage(items: [poolMember("owner", isOwner: true), poolMember("jaime")], next: nil)))
        await refresh.value

        #expect(viewModel.removedGamblerIds.isEmpty)
        #expect(viewModel.lazyPager.loadedItems.map(\.gamblerId) == ["owner", "jaime"])
    }

    @Test
    func aFailedRefreshKeepsRemovedGamblersHidden() async throws {
        let viewModel = makeViewModel()
        await viewModel.lazyPager.refresh()
        try await removeSuccessfully(santiago, in: viewModel)

        memberRepository.members.autoResponse = .failure(RemovalFailure())
        await viewModel.refresh()

        #expect(viewModel.removedGamblerIds == ["santiago"])
    }

    @Test
    func aRemovalTheRefreshedCountReflectsIsNotSubtractedTwice() async throws {
        let viewModel = makeViewModel()
        await viewModel.lazyPager.refresh()
        try await removeSuccessfully(santiago, in: viewModel)
        #expect(viewModel.shownGamblerCount == .loaded(2))

        // The count answers while the members reload is still pending.
        poolRepository.pools.autoResponse = .success(poolWith(gamblerCount: 2))
        memberRepository.members.autoResponse = nil
        let refresh = Task { await viewModel.refresh() }
        try #require(await eventually { memberRepository.members.pendingCount == 1 })
        #expect(await eventually { viewModel.gamblerCount == .loaded(2) })
        #expect(viewModel.shownGamblerCount == .loaded(2))

        memberRepository.members.answer(.success(CursorPage(items: [poolMember("owner", isOwner: true), poolMember("jaime")], next: nil)))
        await refresh.value
        #expect(viewModel.shownGamblerCount == .loaded(2))
    }

    // MARK: Gambler count

    @Test
    func theCountLoadsWhenTheScreenOpens() async throws {
        poolRepository.pools.autoResponse = nil
        let viewModel = makeViewModel()
        #expect(viewModel.gamblerCount == .loading)

        try #require(await eventually { poolRepository.pools.pendingCount == 1 })
        poolRepository.pools.answer(.success(poolWith(gamblerCount: 3)))

        #expect(await eventually { viewModel.gamblerCount == .loaded(3) })
        #expect(viewModel.shownGamblerCount == .loaded(3))
    }

    @Test
    func aFailedCountIsAFailure() async {
        poolRepository.pools.autoResponse = .failure(RemovalFailure())
        let viewModel = makeViewModel()

        #expect(await eventually { viewModel.gamblerCount.isFailure() })
        #expect(viewModel.shownGamblerCount.isFailure())
    }

    @Test
    func refreshReloadsTheCount() async throws {
        poolRepository.pools.autoResponse = .failure(RemovalFailure())
        let viewModel = makeViewModel()
        #expect(await eventually { viewModel.gamblerCount.isFailure() })

        poolRepository.pools.autoResponse = nil
        let refresh = Task { await viewModel.refresh() }
        try #require(await eventually { poolRepository.pools.pendingCount == 1 })
        #expect(viewModel.gamblerCount == .loading)

        poolRepository.pools.answer(.success(poolWith(gamblerCount: 4)))
        await refresh.value

        #expect(viewModel.gamblerCount == .loaded(4))
        #expect(poolRepository.pools.calls == 2)
    }

    // MARK: Helpers

    private func failRemoval(of member: PoolMemberModel, in viewModel: ManageGamblersListViewModel) async throws {
        viewModel.remove(member)
        try #require(await eventually { memberRepository.removals.pendingCount == 1 })
        memberRepository.removals.answer(.failure(RemovalFailure()))
        #expect(await eventually { viewModel.rowHolders[member.gamblerId]?.removal.isFailure() == true })
    }

    private func removeSuccessfully(_ member: PoolMemberModel, in viewModel: ManageGamblersListViewModel) async throws {
        #expect(await eventually { viewModel.gamblerCount.isLoaded() })
        viewModel.remove(member)
        try #require(await eventually { memberRepository.removals.pendingCount == 1 })
        memberRepository.removals.answer(.success(()))
        #expect(await eventually { viewModel.removedGamblerIds.contains(member.gamblerId) })
    }
}
