import Core
import Testing
@testable import UI

/// The spoken movement every surface reuses. English literals resolved through the test runner's
/// language, so they need an English-language simulator.
struct RankMovementAccessibilityTests {
    @Test
    func directionsUseFullSingularAndPluralWords() {
        #expect(RankMovement.moved(places: 1, partialProgress: nil).accessibilityText == "Up 1 place")
        #expect(RankMovement.moved(places: 3, partialProgress: nil).accessibilityText == "Up 3 places")
        #expect(RankMovement.moved(places: -1, partialProgress: nil).accessibilityText == "Down 1 place")
        #expect(RankMovement.moved(places: -2, partialProgress: nil).accessibilityText == "Down 2 places")
        #expect(RankMovement.moved(places: 0, partialProgress: nil).accessibilityText == "Rank unchanged")
    }

    @Test
    func aPartialGroupFollowsTheDirectionWithItsComputedCount() {
        let progress = KickoffGroupProgress(computedMatches: 1, totalMatches: 2)

        #expect(
            RankMovement.moved(places: 3, partialProgress: progress).accessibilityText
                == "Up 3 places, 1 of 2 matches computed"
        )
    }

    @Test
    func pendingAndUnavailableAreExplicitStates() {
        #expect(RankMovement.pending.accessibilityText == "Movement pending")
        #expect(RankMovement.unavailable.accessibilityText == "Movement unavailable")
    }
}
