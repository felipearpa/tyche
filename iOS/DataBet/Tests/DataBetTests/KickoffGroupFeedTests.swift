import Core
import Foundation
import Testing
@testable import DataBet

/// The grouped History and Timeline wire contract (`KickoffGroupResponse.fs` on the server):
/// pages of whole kickoff groups, `movement` as an object or JSON null, and the rejected
/// continuation.
@Suite("Kickoff group feed")
struct KickoffGroupFeedTests {
    @Test("a page decodes its groups in server order with counts, movement, and match rows")
    func pageDecodesGroups() throws {
        let page = try decodePage(
            #"""
            {
              "items": [
                {
                  "kickoffDateTime": "2026-06-12T17:00:00Z",
                  "totalMatches": 2,
                  "computedMatches": 2,
                  "movement": { "beforePosition": 8, "position": 5 },
                  "matches": [\#(match("m-1", computed: true)), \#(match("m-2", computed: true))]
                },
                {
                  "kickoffDateTime": "2026-06-11T20:00:00Z",
                  "totalMatches": 1,
                  "computedMatches": 1,
                  "movement": { "beforePosition": 4, "position": 6 },
                  "matches": [\#(match("m-0", computed: true))]
                }
              ],
              "next": "token-2"
            }
            """#
        )

        #expect(page.next == "token-2")
        #expect(page.items.map(\.kickoffDateTime) == [instant("2026-06-12T17:00:00Z"), instant("2026-06-11T20:00:00Z")])
        let first = try #require(page.items.first)
        #expect(first.totalMatches == 2)
        #expect(first.computedMatches == 2)
        #expect(first.movement == GroupMovement(beforePosition: 8, position: 5))
        #expect(first.matches.map(\.matchId) == ["m-1", "m-2"])
        #expect(first.matches.map(\.isComputed) == [true, true])
    }

    @Test("a partial group keeps the server's counts, not the number of rows it carries")
    func partialGroupKeepsServerCounts() throws {
        let page = try decodePage(
            #"""
            {
              "items": [{
                "kickoffDateTime": "2026-06-12T17:00:00Z",
                "totalMatches": 4,
                "computedMatches": 2,
                "movement": { "beforePosition": 8, "position": 6 },
                "matches": [\#(match("m-1", computed: true))]
              }],
              "next": null
            }
            """#
        )

        let group = try #require(page.items.first)
        #expect(group.totalMatches == 4)
        #expect(group.computedMatches == 2)
        #expect(group.matches.count == 1)
        #expect(page.next == nil)
    }

    @Test("a null movement decodes as no movement while the rows remain")
    func nullMovementDecodesAsNil() throws {
        let page = try decodePage(
            #"""
            {
              "items": [{
                "kickoffDateTime": "2026-06-12T17:00:00Z",
                "totalMatches": 2,
                "computedMatches": 2,
                "movement": null,
                "matches": [\#(match("m-1", computed: true)), \#(match("m-2", computed: true))]
              }]
            }
            """#
        )

        let group = try #require(page.items.first)
        #expect(group.movement == nil)
        #expect(group.matches.count == 2)
    }

    @Test("a group with no computed match decodes with zero computed and pending rows")
    func pendingGroupDecodes() throws {
        let page = try decodePage(
            #"""
            {
              "items": [{
                "kickoffDateTime": "2026-06-12T17:00:00Z",
                "totalMatches": 2,
                "computedMatches": 0,
                "movement": null,
                "matches": [\#(match("m-1", computed: false))]
              }]
            }
            """#
        )

        let group = try #require(page.items.first)
        #expect(group.computedMatches == 0)
        #expect(group.movement == nil)
        #expect(group.matches.map(\.isComputed) == [false])
    }

    @Test("equal instants decode equal and different instants on one day stay distinct")
    func kickoffIdentityIsTheInstant() throws {
        let page = try decodePage(
            #"""
            {
              "items": [
                { "kickoffDateTime": "2026-06-12T17:00:00Z", "totalMatches": 1, "computedMatches": 1, "movement": null, "matches": [] },
                { "kickoffDateTime": "2026-06-12T14:00:00Z", "totalMatches": 1, "computedMatches": 1, "movement": null, "matches": [] }
              ]
            }
            """#
        )

        #expect(page.items[0].kickoffDateTime == instant("2026-06-12T17:00:00Z"))
        #expect(page.items[0].kickoffDateTime != page.items[1].kickoffDateTime)
    }

    @Test("a first page sends no cursor and a continuation sends only the cursor")
    func feedParameters() {
        #expect(kickoffGroupFeedParameters(next: nil) == [:])
        #expect(kickoffGroupFeedParameters(next: "abc") == ["next": "abc"])
    }

    @Test(
        "a rejected continuation maps to cursorRejected",
        arguments: ["INVALID_CURSOR", "CURSOR_CONTEXT_MISMATCH"]
    )
    func rejectedContinuationMaps(code: String) {
        #expect(kickoffGroupFeedError(statusCode: 400, body: body(code)) == .cursorRejected)
    }

    @Test(
        "other responses keep the transport error",
        arguments: [(400, "SOMETHING_ELSE" as String?), (401, nil), (403, "NOT_POOL_MEMBER"), (500, nil)]
    )
    func otherResponsesAreNotFeedErrors(statusCode: Int, code: String?) {
        #expect(kickoffGroupFeedError(statusCode: statusCode, body: code.map(body)) == nil)
    }

    @Test("the repository passes a rejected continuation through unchanged")
    func repositoryPassesCursorRejectionThrough() async {
        let repository = PoolGamblerBetRemoteRepository(
            poolGamblerBetRemoteDataSource: RejectingFeedDataSource(),
            networkErrorHandler: PassThroughHandler()
        )

        let history = await repository.getFinishedPoolGamblerBets(poolId: "pool", gamblerId: "gambler", next: "old")
        let timeline = await repository.getGamblerBetsTimeline(poolId: "pool", gamblerId: "gambler", next: "old")

        for result in [history, timeline] {
            guard case .failure(let error) = result else {
                Issue.record("Expected a failure")
                continue
            }
            #expect(error as? KickoffGroupFeedError == .cursorRejected)
        }
    }

    // MARK: Helpers

    private func decodePage(_ json: String) throws -> CursorPage<KickoffGroup> {
        try JSONDecoder().withISODate()
            .decode(CursorPage<KickoffGroupResponse>.self, from: Data(json.utf8))
            .map { $0.toKickoffGroup() }
    }

    private func match(_ matchId: String, computed: Bool) -> String {
        #"""
        {
          "poolId": "pool", "gamblerId": "gambler", "gamblerUsername": "gambler",
          "matchId": "\#(matchId)",
          "homeTeamId": "es", "homeTeamName": "España", "awayTeamId": "ar", "awayTeamName": "Argentina",
          "homeTeamScore": \#(computed ? "1" : "null"), "awayTeamScore": \#(computed ? "0" : "null"),
          "homeTeamBet": 2, "awayTeamBet": 1,
          "score": \#(computed ? "3" : "null"),
          "matchDateTime": "2026-06-12T17:00:00Z",
          "isLocked": true, "isComputed": \#(computed)
        }
        """#
    }

    private func instant(_ text: String) -> Date {
        ISO8601DateFormatter().date(from: text)!
    }

    private func body(_ code: String) -> Data {
        Data(#"{"error":"\#(code)","message":"text"}"#.utf8)
    }
}

private struct PassThroughHandler: NetworkErrorHandler {
    func handle<Value>(_ perform: () async throws -> Value) async -> Result<Value, Error> {
        do {
            return .success(try await perform())
        } catch {
            return .failure(error)
        }
    }
}

private final class RejectingFeedDataSource: PoolGamblerBetRemoteDataSource {
    func getFinishedPoolGamblerBets(poolId: String, gamblerId: String, next: String?) async throws -> CursorPage<KickoffGroupResponse> {
        throw KickoffGroupFeedError.cursorRejected
    }

    func getGamblerBetsTimeline(poolId: String, gamblerId: String, next: String?) async throws -> CursorPage<KickoffGroupResponse> {
        throw KickoffGroupFeedError.cursorRejected
    }

    func getPendingPoolGamblerBets(poolId: String, gamblerId: String, next: String?, searchText: String?) async throws -> CursorPage<PoolGamblerBetResponse> {
        fatalError("not used")
    }

    func getLivePoolGamblerBets(poolId: String, gamblerId: String, next: String?, searchText: String?) async throws -> CursorPage<PoolGamblerBetResponse> {
        fatalError("not used")
    }

    func getRankedPoolMatchGamblerBets(poolId: String, matchId: String, next: String?) async throws -> CursorPage<PoolGamblerBetResponse> {
        fatalError("not used")
    }

    func getPoolGamblerBet(poolId: String, gamblerId: String, matchId: String) async throws -> PoolGamblerBetResponse {
        fatalError("not used")
    }

    func bet(betRequest: BetRequest) async throws -> PoolGamblerBetResponse {
        fatalError("not used")
    }
}
