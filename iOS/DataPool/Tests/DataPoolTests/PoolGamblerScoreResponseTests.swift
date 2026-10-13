import Core
import Foundation
import Testing
@testable import DataPool

/// The score row contract shared by the leaderboard, My pools, and score-by-id
/// (`PoolGamblerScoreResponse.fs` on the server): `trendGroup` is an object, JSON null, or absent,
/// and `position`/`beforePosition` keep their meaning.
@Suite("Pool gambler score response")
struct PoolGamblerScoreResponseTests {
    @Test("a trend group decodes its kickoff instant and counts beside the positions")
    func trendGroupDecodes() throws {
        let score = try decodeScore(
            trendGroup: #"{"kickoffDateTime":"2026-06-12T17:00:00Z","computedMatches":1,"totalMatches":2}"#
        )

        #expect(score.position == 5)
        #expect(score.beforePosition == 8)
        #expect(
            score.trendGroup
                == TrendGroup(
                    kickoffDateTime: ISO8601DateFormatter().date(from: "2026-06-12T17:00:00Z")!,
                    computedMatches: 1,
                    totalMatches: 2
                )
        )
    }

    @Test("a null trend group decodes as none")
    func nullTrendGroupDecodesAsNil() throws {
        #expect(try decodeScore(trendGroup: "null").trendGroup == nil)
    }

    @Test("an absent trend group decodes as none")
    func absentTrendGroupDecodesAsNil() throws {
        #expect(try decodeScore(trendGroup: nil).trendGroup == nil)
    }

    @Test("a missing baseline still decodes with its trend group")
    func missingBeforePositionDecodes() throws {
        let score = try decodeScore(
            beforePosition: nil,
            trendGroup: #"{"kickoffDateTime":"2026-06-12T17:00:00Z","computedMatches":2,"totalMatches":2}"#
        )

        #expect(score.beforePosition == nil)
        #expect(score.trendGroup?.computedMatches == 2)
    }

    @Test("a page of leaderboard rows decodes the same trend group on every row")
    func pageDecodes() throws {
        let row = scoreJSON(
            beforePosition: 8,
            trendGroup: #"{"kickoffDateTime":"2026-06-12T17:00:00Z","computedMatches":1,"totalMatches":2}"#
        )
        let page = try JSONDecoder().withISODate()
            .decode(CursorPage<PoolGamblerScoreResponse>.self, from: Data(#"{"items":[\#(row),\#(row)],"next":null}"#.utf8))
            .map { $0.toPoolGamblerScore() }

        #expect(page.items.count == 2)
        #expect(page.items[0].trendGroup == page.items[1].trendGroup)
    }

    private func decodeScore(beforePosition: Int? = 8, trendGroup: String?) throws -> PoolGamblerScore {
        try JSONDecoder().withISODate()
            .decode(
                PoolGamblerScoreResponse.self,
                from: Data(scoreJSON(beforePosition: beforePosition, trendGroup: trendGroup).utf8)
            )
            .toPoolGamblerScore()
    }

    private func scoreJSON(beforePosition: Int?, trendGroup: String?) -> String {
        let trendGroupField = trendGroup.map { #","trendGroup":\#($0)"# } ?? ""
        return #"""
        {"poolId":"pool","poolName":"Pool","gamblerId":"gambler","gamblerUsername":"gambler",\#
        "position":5,"beforePosition":\#(beforePosition.map(String.init) ?? "null"),\#
        "score":12,"beforeScore":9,"gamblerCount":20\#(trendGroupField)}
        """#
    }
}
