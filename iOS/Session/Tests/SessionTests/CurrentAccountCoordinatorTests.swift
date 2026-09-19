import Foundation
import Testing
@testable import Session

@Suite("CurrentAccountCoordinator")
struct CurrentAccountCoordinatorTests {

    @Test("given a persisted snapshot when hydrated then it publishes before any remote request")
    func hydrationPublishesPersistedSnapshotWithoutRemoteWork() async {
        let fixture = Fixture(persisted: snapshot(username: "stored", validatedAt: nil))

        let account = await fixture.coordinator.hydrate()

        #expect(account?.username == "stored")
        #expect(fixture.repository.currentAccountCallCount == 0)
    }

    @Test("given no persisted snapshot when hydrated then no account is published")
    func hydrationWithoutSnapshotPublishesNil()  async {
        let fixture = Fixture(persisted: nil)

        let account = await fixture.coordinator.hydrate()

        #expect(account == nil)
    }

    @Test("given a stale snapshot when a cold-start refresh succeeds then the canonical account is persisted and published")
    func coldStartRefreshAdoptsCanonicalAccount() async {
        let fixture = Fixture(persisted: snapshot(username: "stale", validatedAt: nil))
        fixture.repository.currentAccountResults = [.success(bundle(username: "canonical"))]
        await fixture.coordinator.hydrate()

        await fixture.coordinator.refresh(trigger: .coldStart)
        await fixture.coordinator.awaitPersistence()

        let account = await fixture.coordinator.account
        #expect(account?.username == "canonical")
        #expect(fixture.storage.storedSnapshots.last?.account.username == "canonical")
        #expect(fixture.storage.storedSnapshots.last?.validatedAt == fixture.clock.now)
    }

    @Test("given a fresh snapshot when a non-forced trigger fires then no request starts")
    func freshSnapshotSuppressesRefresh() async {
        let fixture = Fixture(persisted: snapshot(username: "fresh", validatedAt: Date(timeIntervalSince1970: 990)))
        fixture.clock.now = Date(timeIntervalSince1970: 1000)
        await fixture.coordinator.hydrate()

        await fixture.coordinator.refresh(trigger: .foreground)
        await fixture.coordinator.refresh(trigger: .profileOpened)

        #expect(fixture.repository.currentAccountCallCount == 0)
    }

    @Test("given a stale snapshot when a foreground trigger fires then it refreshes")
    func staleSnapshotRefreshesOnForeground() async {
        let fixture = Fixture(persisted: snapshot(username: "old", validatedAt: Date(timeIntervalSince1970: 0)))
        fixture.clock.now = Date(timeIntervalSince1970: 10_000)
        fixture.repository.currentAccountResults = [.success(bundle(username: "new"))]
        await fixture.coordinator.hydrate()

        await fixture.coordinator.refresh(trigger: .foreground)

        #expect(fixture.repository.currentAccountCallCount == 1)
        let account = await fixture.coordinator.account
        #expect(account?.username == "new")
    }

    @Test("given concurrent refresh triggers then one request serves every waiter")
    func concurrentTriggersCoalesce() async {
        let fixture = Fixture(persisted: snapshot(username: "stale", validatedAt: nil))
        fixture.repository.gateCurrentAccount = true
        fixture.repository.currentAccountResults = [.success(bundle(username: "coalesced"))]
        await fixture.coordinator.hydrate()

        let first = Task { await fixture.coordinator.refresh(trigger: .coldStart) }
        let second = Task { await fixture.coordinator.refresh(trigger: .profileOpened) }
        await waitUntil { fixture.repository.currentAccountCallCount == 1 }
        fixture.repository.releaseCurrentAccountGate()
        await first.value
        await second.value

        #expect(fixture.repository.currentAccountCallCount == 1)
        let account = await fixture.coordinator.account
        #expect(account?.username == "coalesced")
    }

    @Test("given a refresh fails then the snapshot is retained and a later trigger retries")
    func failedRefreshRetainsSnapshotAndStaysRetryable() async {
        let fixture = Fixture(persisted: snapshot(username: "offline", validatedAt: nil))
        fixture.repository.currentAccountResults = [
            .failure(TestError()),
            .success(bundle(username: "recovered")),
        ]
        await fixture.coordinator.hydrate()

        await fixture.coordinator.refresh(trigger: .coldStart)
        let retained = await fixture.coordinator.account
        #expect(retained?.username == "offline")

        await fixture.coordinator.refresh(trigger: .profileOpened)
        let recovered = await fixture.coordinator.account
        #expect(recovered?.username == "recovered")
        #expect(fixture.repository.currentAccountCallCount == 2)
    }

    @Test("given a refresh began before a username save then its result cannot overwrite the saved username")
    func staleRefreshResultCannotOverwriteNewerSave() async {
        let fixture = Fixture(persisted: snapshot(username: "before", validatedAt: nil))
        fixture.repository.gateCurrentAccount = true
        fixture.repository.currentAccountResults = [.success(bundle(username: "before"))]
        await fixture.coordinator.hydrate()

        let refresh = Task { await fixture.coordinator.refresh(trigger: .coldStart) }
        await waitUntil { fixture.repository.currentAccountCallCount == 1 }

        let saveResult = await fixture.coordinator.updateUsername("after")
        #expect((try? saveResult.get()) == "after")

        fixture.repository.releaseCurrentAccountGate()
        await refresh.value
        await fixture.coordinator.awaitPersistence()

        let account = await fixture.coordinator.account
        #expect(account?.username == "after")
        #expect(fixture.storage.storedSnapshots.last?.account.username == "after")
    }

    @Test("given a successful username save then the submitted username is persisted and published")
    func successfulSavePersistsSubmittedUsername() async {
        let fixture = Fixture(persisted: snapshot(username: "old", validatedAt: nil))
        await fixture.coordinator.hydrate()

        let result = await fixture.coordinator.updateUsername("newname")
        await fixture.coordinator.awaitPersistence()

        #expect((try? result.get()) == "newname")
        #expect(fixture.repository.updatedUsernames == [Update(accountId: "account-1", username: "newname")])
        let account = await fixture.coordinator.account
        #expect(account?.username == "newname")
        #expect(fixture.storage.storedSnapshots.last?.account.username == "newname")
    }

    @Test("given a failed username save then the published account is unchanged")
    func failedSaveLeavesAccountUnchanged() async {
        let fixture = Fixture(persisted: snapshot(username: "kept", validatedAt: nil))
        fixture.repository.updateUsernameResult = .failure(TestError())
        await fixture.coordinator.hydrate()

        let result = await fixture.coordinator.updateUsername("rejected")

        #expect(result.isFailure)
        let account = await fixture.coordinator.account
        #expect(account?.username == "kept")
        #expect(fixture.storage.storedSnapshots.isEmpty)
    }

    @Test("given logout during an in-flight refresh then the late result cannot restore the account")
    func logoutWinsOverInFlightRefresh() async {
        let fixture = Fixture(persisted: snapshot(username: "leaving", validatedAt: nil))
        fixture.repository.gateCurrentAccount = true
        fixture.repository.currentAccountResults = [.success(bundle(username: "leaving"))]
        await fixture.coordinator.hydrate()

        let refresh = Task { await fixture.coordinator.refresh(trigger: .coldStart) }
        await waitUntil { fixture.repository.currentAccountCallCount == 1 }

        try? await fixture.coordinator.clear()
        fixture.repository.releaseCurrentAccountGate()
        await refresh.value
        await fixture.coordinator.awaitPersistence()

        let account = await fixture.coordinator.account
        #expect(account == nil)
        #expect(fixture.storage.operations.last == .delete)
    }

    @Test("given logout then the observable value and the persisted snapshot are cleared")
    func logoutClearsPublishedAndPersistedState() async {
        let fixture = Fixture(persisted: snapshot(username: "gone", validatedAt: nil))
        await fixture.coordinator.hydrate()

        try? await fixture.coordinator.clear()
        await fixture.coordinator.awaitPersistence()

        let account = await fixture.coordinator.account
        #expect(account == nil)
        #expect(fixture.storage.deleteCount == 1)
        let persisted = try? await fixture.storage.retrieve()
        #expect(persisted == nil)
    }

    @Test("given sign-in installation then the canonical account is published with a fresh validation")
    func installPublishesCanonicalAccount() async {
        let fixture = Fixture(persisted: nil)
        await fixture.coordinator.hydrate()

        try? await fixture.coordinator.install(account: bundle(username: "signedin"))
        await fixture.coordinator.awaitPersistence()

        let account = await fixture.coordinator.account
        #expect(account?.username == "signedin")
        #expect(fixture.storage.storedSnapshots.last?.validatedAt == fixture.clock.now)

        await fixture.coordinator.refresh(trigger: .coldStart)
        #expect(fixture.repository.currentAccountCallCount == 0)
    }

    @Test("given a subscriber then the stream replays the current account and follows changes")
    func streamReplaysAndFollowsChanges() async {
        let fixture = Fixture(persisted: snapshot(username: "replayed", validatedAt: nil))
        await fixture.coordinator.hydrate()

        let stream = await fixture.coordinator.accountUpdates()
        var iterator = stream.makeAsyncIterator()

        let replayed = await iterator.next()
        #expect(replayed??.username == "replayed")

        _ = await fixture.coordinator.updateUsername("followed")
        let followed = await iterator.next()
        #expect(followed??.username == "followed")
    }
}

private func bundle(username: String) -> AccountBundle {
    AccountBundle(
        accountId: "account-1",
        externalAccountId: "external-1",
        email: "gambler@tyche.com",
        username: username
    )
}

private func snapshot(username: String, validatedAt: Date?) -> CurrentAccountSnapshot {
    CurrentAccountSnapshot(account: bundle(username: username), validatedAt: validatedAt)
}

private func waitUntil(
    timeoutTicks: Int = 2000,
    _ condition: @escaping () -> Bool
) async {
    var ticks = 0
    while !condition() && ticks < timeoutTicks {
        await Task.yield()
        ticks += 1
    }
}

private struct TestError: Error {}

private struct Update: Equatable {
    let accountId: String
    let username: String
}

private final class Fixture {
    let storage: FakeAccountStorage
    let repository: ControllableAuthenticationRepository
    let clock: MutableClock
    let coordinator: CurrentAccountCoordinator

    init(persisted: CurrentAccountSnapshot?) {
        let storage = FakeAccountStorage(snapshot: persisted)
        let repository = ControllableAuthenticationRepository()
        let clock = MutableClock(Date(timeIntervalSince1970: 1000))
        self.storage = storage
        self.repository = repository
        self.clock = clock
        self.coordinator = CurrentAccountCoordinator(
            accountStorage: storage,
            authenticationRepository: repository,
            now: { clock.now },
            freshnessInterval: 300
        )
    }
}

private final class MutableClock: @unchecked Sendable {
    var now: Date

    init(_ now: Date) {
        self.now = now
    }
}

private final class FakeAccountStorage: AccountStorage, @unchecked Sendable {
    enum Operation: Equatable {
        case store
        case delete
    }

    private var snapshot: CurrentAccountSnapshot?
    private(set) var storedSnapshots: [CurrentAccountSnapshot] = []
    private(set) var deleteCount = 0
    private(set) var operations: [Operation] = []

    init(snapshot: CurrentAccountSnapshot?) {
        self.snapshot = snapshot
    }

    func store(snapshot: CurrentAccountSnapshot) async throws {
        storedSnapshots.append(snapshot)
        operations.append(.store)
        self.snapshot = snapshot
    }

    func delete() async throws {
        deleteCount += 1
        operations.append(.delete)
        snapshot = nil
    }

    func retrieve() async throws -> CurrentAccountSnapshot? { snapshot }
}

private final class ControllableAuthenticationRepository: AuthenticationRepository, @unchecked Sendable {
    var currentAccountResults: [Result<AccountBundle, Error>] = []
    var updateUsernameResult: Result<Void, Error> = .success(())
    var gateCurrentAccount = false

    private(set) var currentAccountCallCount = 0
    private(set) var updatedUsernames: [Update] = []
    private var gateContinuations: [CheckedContinuation<Void, Never>] = []

    func releaseCurrentAccountGate() {
        let continuations = gateContinuations
        gateContinuations = []
        for continuation in continuations {
            continuation.resume()
        }
    }

    func getCurrentAccount() async -> Result<AccountBundle, Error> {
        currentAccountCallCount += 1
        if gateCurrentAccount {
            await withCheckedContinuation { gateContinuations.append($0) }
        }
        if currentAccountResults.isEmpty {
            return .success(bundle(username: "default"))
        }
        return currentAccountResults.removeFirst()
    }

    func updateUsername(accountId: String, username: String) async -> Result<Void, Error> {
        updatedUsernames.append(Update(accountId: accountId, username: username))
        return updateUsernameResult
    }

    func sendSignInLinkToEmail(email: String) async -> Result<Void, Error> { .success(()) }
    func signInWithEmailLink(email: String, emailLink: String) async -> Result<ExternalAccountId, Error> { .success("") }
    func signInWithEmailAndPassword(email: String, password: String) async -> Result<ExternalAccountId, Error> { .success("") }
    func signInWithGoogle(idToken: String, accessToken: String) async -> Result<GoogleSignInResult, Error> {
        .success(GoogleSignInResult(externalAccountId: "", email: ""))
    }
    func logOut() async -> Result<Void, Error> { .success(()) }
    func linkAccount(accountLink: AccountLink) async -> Result<AccountBundle, Error> {
        .success(bundle(username: "linked"))
    }
}

private extension Result {
    var isFailure: Bool {
        if case .failure = self { return true }
        return false
    }
}
