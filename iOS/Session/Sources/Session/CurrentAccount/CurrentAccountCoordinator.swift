import Foundation

/// Application triggers that may start a current-account refresh.
public enum CurrentAccountRefreshTrigger: Sendable {
    /// First authenticated launch of the process; refreshes once regardless of staleness.
    case coldStart
    /// The app returned to the foreground; refreshes only when the snapshot is stale.
    case foreground
    /// Profile opened; refreshes only when the snapshot is stale.
    case profileOpened
}

/// How long a successful validation keeps the current account fresh.
public let currentAccountFreshnessInterval: TimeInterval = 5 * 60

/// Application-scoped owner of the signed-in account. Serializes sign-in installation,
/// server refresh, username mutation, and logout clearing so an older remote result can
/// never overwrite newer local state, and publishes one replaying observable value that
/// every account consumer observes instead of keeping manual copies.
///
/// State changes are applied synchronously inside the actor and fenced by an epoch:
/// any operation that began before a mutation or logout finds the epoch advanced and
/// discards its result. Disk writes are enqueued in state-change order so the persisted
/// snapshot can never run ahead of, or reorder against, the published value.
public actor CurrentAccountCoordinator {
    private let accountStorage: AccountStorage
    private let authenticationRepository: AuthenticationRepository
    private let now: @Sendable () -> Date
    private let freshnessInterval: TimeInterval

    private var snapshot: CurrentAccountSnapshot?
    private var epoch = 0
    private var hydrationTask: Task<Void, Never>?
    private var refreshTask: Task<Void, Never>?
    private var hasRefreshedAfterColdStart = false
    private var persistenceChain: Task<Void, Never>?
    private var continuations: [UUID: AsyncStream<AccountBundle?>.Continuation] = [:]

    init(
        accountStorage: AccountStorage,
        authenticationRepository: AuthenticationRepository,
        now: @escaping @Sendable () -> Date = { Date() },
        freshnessInterval: TimeInterval = currentAccountFreshnessInterval
    ) {
        self.accountStorage = accountStorage
        self.authenticationRepository = authenticationRepository
        self.now = now
        self.freshnessInterval = freshnessInterval
    }

    /// The current signed-in account, or nil when signed out or not yet hydrated.
    public var account: AccountBundle? { snapshot?.account }

    /// Replays the current account immediately, then yields every subsequent change.
    public func accountUpdates() -> AsyncStream<AccountBundle?> {
        AsyncStream { continuation in
            let id = UUID()
            continuations[id] = continuation
            continuation.yield(snapshot?.account)
            continuation.onTermination = { @Sendable _ in
                Task { [weak self] in await self?.removeContinuation(id) }
            }
        }
    }

    /// Loads the persisted snapshot once and publishes it before any remote work,
    /// so authenticated routing never waits on a network request.
    @discardableResult
    public func hydrate() async -> AccountBundle? {
        if hydrationTask == nil {
            hydrationTask = Task { await self.performHydration() }
        }
        await hydrationTask?.value
        return snapshot?.account
    }

    /// Installs the canonical account produced by sign-in. Throws when the snapshot could
    /// not be persisted, so sign-in can report the failure instead of silently producing a
    /// session that will not survive the next launch.
    public func install(account: AccountBundle) async throws {
        epoch += 1
        let newSnapshot = CurrentAccountSnapshot(account: account, validatedAt: now())
        snapshot = newSnapshot
        hasRefreshedAfterColdStart = true
        publish()
        try await persistOrdered { [accountStorage] in
            try await accountStorage.store(snapshot: newSnapshot)
        }
    }

    /// Refreshes from the server when the trigger is eligible. Concurrent triggers
    /// coalesce into one request; a failure keeps the snapshot published and stale so a
    /// later trigger retries.
    public func refresh(trigger: CurrentAccountRefreshTrigger) async {
        guard snapshot != nil else { return }
        switch trigger {
        case .coldStart:
            guard !hasRefreshedAfterColdStart else { return }
        case .foreground, .profileOpened:
            guard isStale else { return }
        }
        hasRefreshedAfterColdStart = true
        if refreshTask == nil {
            refreshTask = Task { await self.performRefresh() }
        }
        let task = refreshTask
        await task?.value
    }

    /// PATCHes the username and, on success, persists and publishes the submitted value.
    /// A refresh that began earlier can no longer overwrite it; a failure leaves the
    /// published account unchanged.
    public func updateUsername(_ username: String) async -> Result<String, Error> {
        guard let current = snapshot else {
            return .failure(UpdateUsernameError.noStoredAccount)
        }
        let startEpoch = epoch
        let result = await authenticationRepository.updateUsername(
            accountId: current.account.accountId,
            username: username
        )
        switch result {
        case .success:
            if epoch == startEpoch, let live = snapshot {
                epoch += 1
                let updated = CurrentAccountSnapshot(
                    account: live.account.withUsername(username),
                    validatedAt: live.validatedAt
                )
                snapshot = updated
                publish()
                enqueuePersistence { [accountStorage] in
                    try? await accountStorage.store(snapshot: updated)
                }
            }
            return .success(username)
        case .failure(let error):
            return .failure(error)
        }
    }

    /// Clears the account on logout; any in-flight refresh or mutation result can no
    /// longer restore it. Throws when the persisted snapshot could not be removed, so
    /// callers can surface an incomplete logout instead of leaving the account
    /// recoverable on disk.
    public func clear() async throws {
        epoch += 1
        snapshot = nil
        hasRefreshedAfterColdStart = false
        publish()
        try await persistOrdered { [accountStorage] in
            try await accountStorage.delete()
        }
    }

    /// Awaits every persistence operation enqueued so far.
    func awaitPersistence() async {
        await persistenceChain?.value
    }

    private var isStale: Bool {
        guard let validatedAt = snapshot?.validatedAt else { return true }
        return now().timeIntervalSince(validatedAt) >= freshnessInterval
    }

    private func performHydration() async {
        let startEpoch = epoch
        let persisted = (try? await accountStorage.retrieve()) ?? nil
        guard let persisted, epoch == startEpoch, snapshot == nil else { return }
        snapshot = persisted
        publish()
    }

    private func performRefresh() async {
        defer { refreshTask = nil }
        let startEpoch = epoch
        let result = await authenticationRepository.getCurrentAccount()
        guard epoch == startEpoch, snapshot != nil else { return }
        guard case .success(let account) = result else { return }
        let newSnapshot = CurrentAccountSnapshot(account: account, validatedAt: now())
        snapshot = newSnapshot
        publish()
        enqueuePersistence { [accountStorage] in
            try? await accountStorage.store(snapshot: newSnapshot)
        }
    }

    private func publish() {
        let account = snapshot?.account
        for continuation in continuations.values {
            continuation.yield(account)
        }
    }

    private func removeContinuation(_ id: UUID) {
        continuations[id] = nil
    }

    private func enqueuePersistence(_ operation: @escaping @Sendable () async -> Void) {
        let previous = persistenceChain
        persistenceChain = Task {
            await previous?.value
            await operation()
        }
    }

    /// Enqueues a persistence operation in state-change order and awaits it, rethrowing
    /// its failure. Refresh and username persists stay fire-and-forget (the server is
    /// authoritative and the next refresh self-heals); install and clear must be durable.
    private func persistOrdered(_ operation: @escaping @Sendable () async throws -> Void) async throws {
        let previous = persistenceChain
        let task = Task {
            await previous?.value
            try await operation()
        }
        persistenceChain = Task { _ = try? await task.value }
        try await task.value
    }
}
