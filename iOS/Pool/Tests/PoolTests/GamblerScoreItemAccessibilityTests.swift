import SwiftUI
import Testing
import UI
@testable import Pool

/// Exact-match guard on the leaderboard row's composed announcement.
///
/// This exists because the only other assertion on `GamblerScoreItem.accessibilityLabel`
/// lives in `GamblerScoreItemRankPreviewTests`, which cannot run on local Xcode — that class
/// uses ViewInspector, which dies with `unsafeBitCast` on 26.0.1/27.0 and passes only on CI.
/// Nothing here touches ViewInspector, so the leaderboard row's field order, separators, and
/// plural agreement stay guarded locally, the way `GamblerScoreListTest` guards them on Android.
///
/// Order under test: rank, username, "You" when signed in, points, movement.
///
/// These assertions are English literals resolved through the *simulator's* language, so they
/// require an English-language simulator — the same trap the repo already records for Android's
/// instrumented tests (en-US AVD). They pin the plural tables' `one`/`other` selection, not the
/// Spanish wording, which has no coverage on either platform.
@MainActor
struct GamblerScoreItemAccessibilityTests {
    @Test
    func signedInRowAnnouncesRankIdentityYouPointsAndMovement() {
        // position 3 from beforePosition 4 is a one-place gain.
        let label = GamblerScoreItem(
            poolGamblerScore: model(position: 3, beforePosition: 4, score: 150),
            isCurrentUser: true
        )
        .accessibilityLabel

        #expect(label == "Rank 3, ElGoleador, You, 150 points, Up 1 place")
    }

    @Test
    func otherGamblerOmitsTheYouMarker() {
        let label = GamblerScoreItem(
            poolGamblerScore: model(position: 3, beforePosition: 4, score: 150),
            isCurrentUser: false
        )
        .accessibilityLabel

        #expect(label == "Rank 3, ElGoleador, 150 points, Up 1 place")
    }

    @Test
    func missingRankAndScoreAreAnnouncedAsUnavailable() {
        let label = GamblerScoreItem(
            poolGamblerScore: model(position: nil, beforePosition: nil, score: nil),
            isCurrentUser: false
        )
        .accessibilityLabel

        #expect(label == "Rank unavailable, ElGoleador, Points unavailable")
    }

    @Test
    func singularAndPluralCountsAgreeWithTheirValues() {
        let singular = GamblerScoreItem(
            poolGamblerScore: model(position: 3, beforePosition: 4, score: 1),
            isCurrentUser: false
        )
        .accessibilityLabel
        let plural = GamblerScoreItem(
            poolGamblerScore: model(position: 3, beforePosition: 5, score: 2),
            isCurrentUser: false
        )
        .accessibilityLabel

        #expect(singular == "Rank 3, ElGoleador, 1 point, Up 1 place")
        #expect(plural == "Rank 3, ElGoleador, 2 points, Up 2 places")
    }

    @Test
    func unchangedRankAnnouncesTheSteadyPhraseLast() {
        let label = GamblerScoreItem(
            poolGamblerScore: model(position: 3, beforePosition: 3, score: 150),
            isCurrentUser: false
        )
        .accessibilityLabel

        #expect(label == "Rank 3, ElGoleador, 150 points, Rank unchanged")
    }

    @Test
    func placeholderRowContributesNoAnnouncement() {
        let item = GamblerScoreItem(
            poolGamblerScore: poolGamblerScorePlaceholderModel(),
            isCurrentUser: false,
            placeholderModifier: ShimmerModifier()
        )

        #expect(item.isPlaceholder)
        #expect(item.accessibilityLabel.isEmpty)
    }

    private func model(position: Int?, beforePosition: Int?, score: Int?) -> PoolGamblerScoreModel {
        PoolGamblerScoreModel(
            poolId: "A3C2E1",
            poolName: "Neptune World Series 2023",
            gamblerId: "YF23H1",
            gamblerUsername: "ElGoleador",
            position: position,
            beforePosition: beforePosition,
            score: score,
            gamblerCount: nil
        )
    }
}
