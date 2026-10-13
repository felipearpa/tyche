import Testing
@testable import Core

@Suite("RankMovement")
struct RankMovementTests {
    private let complete = KickoffGroupProgress(computedMatches: 2, totalMatches: 2)
    private let partial = KickoffGroupProgress(computedMatches: 1, totalMatches: 2)

    @Test("a score row without a trend group is pending, whatever its positions")
    func scoreWithoutTrendGroupIsPending() {
        #expect(RankMovement.trendGroup(nil, beforePosition: 8, position: 5) == .pending)
        #expect(RankMovement.trendGroup(nil, beforePosition: nil, position: nil) == .pending)
    }

    @Test("a score row with a trend group but a missing position is unavailable")
    func scoreWithMissingPositionIsUnavailable() {
        #expect(RankMovement.trendGroup(complete, beforePosition: nil, position: 5) == .unavailable)
        #expect(RankMovement.trendGroup(complete, beforePosition: 8, position: nil) == .unavailable)
    }

    @Test("a complete trend group moves by the baseline minus the position, zero included")
    func completeTrendGroupIsNumeric() {
        #expect(RankMovement.trendGroup(complete, beforePosition: 8, position: 5) == .moved(places: 3, partialProgress: nil))
        #expect(RankMovement.trendGroup(complete, beforePosition: 4, position: 6) == .moved(places: -2, partialProgress: nil))
        #expect(RankMovement.trendGroup(complete, beforePosition: 3, position: 3) == .moved(places: 0, partialProgress: nil))
    }

    @Test("a partial trend group keeps its computed and total counts")
    func partialTrendGroupCarriesProgress() {
        #expect(RankMovement.trendGroup(partial, beforePosition: 8, position: 5) == .moved(places: 3, partialProgress: partial))
    }

    @Test("a feed group without a movement is pending until the gambler has a computed match")
    func feedGroupWithoutMovement() {
        let pendingProgress = KickoffGroupProgress(computedMatches: 0, totalMatches: 2)
        #expect(
            RankMovement.kickoffGroup(beforePosition: nil, position: nil, progress: pendingProgress, hasComputedMatch: false)
                == .pending
        )
        #expect(
            RankMovement.kickoffGroup(beforePosition: nil, position: nil, progress: partial, hasComputedMatch: true)
                == .unavailable
        )
    }

    @Test("a feed group's movement comes from its snapshots and carries progress only when partial")
    func feedGroupWithMovement() {
        #expect(
            RankMovement.kickoffGroup(beforePosition: 8, position: 6, progress: partial, hasComputedMatch: true)
                == .moved(places: 2, partialProgress: partial)
        )
        #expect(
            RankMovement.kickoffGroup(beforePosition: 8, position: 6, progress: complete, hasComputedMatch: true)
                == .moved(places: 2, partialProgress: nil)
        )
    }
}
