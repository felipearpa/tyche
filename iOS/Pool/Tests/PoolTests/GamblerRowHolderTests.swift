import Testing
import DataPool
import ViewingState
@testable import Pool

@MainActor
struct GamblerRowHolderTests {
    private let repository = ScriptedPoolMemberRepository()
    private let outcomes = OutcomeLog()
    private let member = poolMemberModel("santiago")

    private func makeHolder() -> GamblerRowHolder {
        GamblerRowHolder(
            member: member,
            poolId: testPoolId,
            removeGamblerUseCase: RemoveGamblerUseCase(poolMemberRepository: repository),
            outcomes: outcomes
        )
    }

    @Test
    func removeShowsRemovingThenRemovedAndReportsIt() async throws {
        let holder = makeHolder()
        #expect(holder.removal.isIdle())

        holder.remove()
        #expect(holder.removal.isMutating())
        try #require(await eventually { repository.removals.pendingCount == 1 })
        #expect(outcomes.events.isEmpty)

        repository.removals.answer(.success(()))

        #expect(await eventually { holder.removal.isMutated() })
        #expect(outcomes.events == [.removed("santiago")])
    }

    @Test
    func removingAgainWhileInFlightSendsNoSecondRequest() async throws {
        let holder = makeHolder()

        holder.remove()
        try #require(await eventually { repository.removals.pendingCount == 1 })
        holder.remove()
        holder.retry()
        try? await Task.sleep(nanoseconds: 50_000_000)

        #expect(repository.removals.calls == 1)
        repository.removals.answer(.success(()))
        #expect(await eventually { holder.removal.isMutated() })
    }

    @Test
    func failureShowsTheFailureAndReportsIt() async throws {
        let holder = makeHolder()

        try await fail(holder)

        #expect(holder.removal.isFailure())
        #expect(holder.removal.activeValue() == member)
        #expect(outcomes.events == [.failed("santiago")])
    }

    @Test
    func retryRestartsTheRemovalWithoutConfirmation() async throws {
        let holder = makeHolder()
        try await fail(holder)

        holder.retry()

        #expect(holder.removal.isMutating())
        #expect(await eventually { repository.removals.calls == 2 })
        #expect(outcomes.events == [.failed("santiago"), .failureCleared("santiago")])

        repository.removals.answer(.success(()))
        #expect(await eventually { holder.removal.isMutated() })
        #expect(outcomes.events.last == .removed("santiago"))
    }

    @Test
    func dismissReturnsToIdleWithoutARequest() async throws {
        let holder = makeHolder()
        try await fail(holder)

        holder.dismissFailure()
        try? await Task.sleep(nanoseconds: 50_000_000)

        #expect(holder.removal.isIdle())
        #expect(repository.removals.calls == 1)
        #expect(outcomes.events == [.failed("santiago"), .failureCleared("santiago")])
    }

    @Test
    func retryAndDismissDoNothingOutsideAFailure() async {
        let holder = makeHolder()

        holder.retry()
        holder.dismissFailure()
        try? await Task.sleep(nanoseconds: 50_000_000)

        #expect(holder.removal.isIdle())
        #expect(repository.removals.calls == 0)
        #expect(outcomes.events.isEmpty)
    }

    private func fail(_ holder: GamblerRowHolder) async throws {
        holder.remove()
        try #require(await eventually { repository.removals.pendingCount == 1 })
        repository.removals.answer(.failure(RemovalFailure()))
        #expect(await eventually { holder.removal.isFailure() })
    }
}

@MainActor
private final class OutcomeLog: GamblerRowOutcomes {
    enum Event: Equatable {
        case removed(String)
        case failed(String)
        case failureCleared(String)
    }

    private(set) var events: [Event] = []

    func removed(_ gamblerId: String) { events.append(.removed(gamblerId)) }
    func failed(_ gamblerId: String) { events.append(.failed(gamblerId)) }
    func failureCleared(_ gamblerId: String) { events.append(.failureCleared(gamblerId)) }
}
