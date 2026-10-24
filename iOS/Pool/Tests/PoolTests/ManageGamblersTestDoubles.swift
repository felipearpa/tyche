import Foundation
import Core
import Testing
import DataPool
@testable import Pool

/// Answers a repository call either at once (`autoResponse`) or when the test calls `answer`, so
/// tests can look at the state while a request is in flight.
@MainActor
final class Responder<Response> {
    private(set) var calls = 0
    private var waiting: [CheckedContinuation<Response, Never>] = []
    var autoResponse: Response?

    init(autoResponse: Response? = nil) {
        self.autoResponse = autoResponse
    }

    var pendingCount: Int { waiting.count }

    func respond() async -> Response {
        calls += 1
        if let autoResponse {
            return autoResponse
        }
        return await withCheckedContinuation { waiting.append($0) }
    }

    /// Answers the oldest pending call, or records an issue when no call is waiting.
    func answer(_ response: Response, sourceLocation: SourceLocation = #_sourceLocation) {
        guard !waiting.isEmpty else {
            Issue.record("No pending call to answer", sourceLocation: sourceLocation)
            return
        }
        waiting.removeFirst().resume(returning: response)
    }
}

final class ScriptedPoolMemberRepository: PoolMemberRepository, @unchecked Sendable {
    let members: Responder<Result<CursorPage<PoolMember>, Error>>
    let removals: Responder<Result<Void, Error>>

    @MainActor
    init(members: [PoolMember] = []) {
        self.members = Responder(autoResponse: .success(CursorPage(items: members, next: nil)))
        self.removals = Responder()
    }

    func getPoolMembers(poolId: String, next: String?) async -> Result<CursorPage<PoolMember>, Error> {
        await members.respond()
    }

    func removeGambler(poolId: String, gamblerId: String) async -> Result<Void, Error> {
        await removals.respond()
    }
}

final class ScriptedPoolRepository: PoolRepository, @unchecked Sendable {
    let pools: Responder<Result<Pool, Error>>

    @MainActor
    init(autoResponse: Result<Pool, Error>? = nil) {
        self.pools = Responder(autoResponse: autoResponse)
    }

    func getPool(id: String) async -> Result<Pool, Error> {
        await pools.respond()
    }

    func createPool(createPoolInput: CreatePoolInput) async -> Result<CreatePoolOutput, Error> {
        fatalError("Not used by Manage gamblers")
    }

    func joinPool(joinPoolInput: JoinPoolInput) async -> Result<Void, Error> {
        fatalError("Not used by Manage gamblers")
    }

    func deletePool(poolId: String, gamblerId: String) async -> Result<Void, Error> {
        fatalError("Not used by Manage gamblers")
    }
}

struct RemovalFailure: Error {}

func poolWith(gamblerCount: Int?) -> Pool {
    Pool(id: testPoolId, name: "Pool", creatorGamblerId: "owner", gamblerCount: gamblerCount)
}

let testPoolId = "pool"

func poolMemberModel(_ gamblerId: String) -> PoolMemberModel {
    PoolMemberModel(
        gamblerId: gamblerId,
        gamblerUsername: gamblerId,
        gamblerEmail: "\(gamblerId)@example.com"
    )
}

/// `PoolMember` has no public initializer; it is decoded the way the network layer builds it.
func poolMember(_ gamblerId: String, isOwner: Bool = false) -> PoolMember {
    let json = """
    {"gamblerId":"\(gamblerId)","gamblerUsername":"\(gamblerId)","gamblerEmail":"\(gamblerId)@example.com","isOwner":\(isOwner)}
    """
    return try! JSONDecoder().decode(PoolMember.self, from: Data(json.utf8))
}

/// Polls `condition` on the main actor.
/// Budgets polls, not wall time: parallel ViewInspector suites can stall the main actor on CI far
/// past any deadline, while the work being waited on queues behind the same stall.
@MainActor
func eventually(polls: Int = 1000, _ condition: () -> Bool) async -> Bool {
    var remaining = polls
    while !condition() {
        guard remaining > 0 else { return false }
        remaining -= 1
        try? await Task.sleep(for: .milliseconds(10))
    }
    return true
}
