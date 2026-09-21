import SwiftUI
import Testing
import UI
@testable import Pool

/// The "My pools" row announces one logical standing per pool, composed from
/// `PoolGamblerScoreModel` rather than scraped from the abbreviated visible copy.
///
/// These assert the string `PoolScoreItem.body` hands to `.accessibilityLabel` rather than
/// the rendered modifier: ViewInspector 0.10.2 resolves SwiftUI's private
/// `AccessibilityAttachmentModifier` by reflection path and has no branch for current iOS,
/// the same limitation documented in `GamblerScoreItemRankPreviewTests`. The binding itself
/// is the single line `.accessibilityLabel(accessibilityLabel)` in `PoolScoreItem.body`.
///
/// The expected strings are identical to the ones `PoolScoreItemAccessibilityTest` asserts
/// on Android — the two platforms' leaderboard phrasings were reconciled, so a change to
/// either side's string table now reds a test on that side.
///
/// These assertions are English literals resolved through the *simulator's* language, so they
/// require an English-language simulator — the same trap the repo already records for Android's
/// instrumented tests (en-US AVD). They pin the plural tables' `one`/`other` selection, not the
/// Spanish wording, which has no coverage on either platform.
@MainActor
struct PoolScoreItemAccessibilityTests {
    @Test
    func completeRowAnnouncesNameRankPointsMembersAndMovement() {
        // position 4 from beforePosition 3 is a one-place drop.
        let label = PoolScoreItem(
            poolGamblerScore: model(position: 4, beforePosition: 3, score: 8, gamblerCount: 101),
            onOpen: {},
            onJoin: {}
        )
        .accessibilityLabel

        #expect(
            label == "Neptune World Series 2023, Rank 4, 8 points, 101 members, Down 1 place"
        )
    }

    @Test
    func missingRankAndScoreAreAnnouncedAsUnavailable() {
        let label = PoolScoreItem(
            poolGamblerScore: model(position: nil, beforePosition: nil, score: nil, gamblerCount: 101),
            onOpen: {},
            onJoin: {}
        )
        .accessibilityLabel

        // The pool is still identified and counted; only rank and points report unavailable,
        // and with no rank there is nothing to compare, so no movement is announced.
        #expect(
            label == "Neptune World Series 2023, Rank unavailable, Points unavailable, 101 members"
        )
    }

    @Test
    func rankWithoutAPreviousRankAnnouncesNoMovement() {
        let label = PoolScoreItem(
            poolGamblerScore: model(position: 4, beforePosition: nil, score: 8, gamblerCount: 101),
            onOpen: {},
            onJoin: {}
        )
        .accessibilityLabel

        #expect(label == "Neptune World Series 2023, Rank 4, 8 points, 101 members")
    }

    @Test
    func unchangedRankAnnouncesTheSteadyPhraseLast() {
        let label = PoolScoreItem(
            poolGamblerScore: model(position: 4, beforePosition: 4, score: 8, gamblerCount: 101),
            onOpen: {},
            onJoin: {}
        )
        .accessibilityLabel

        #expect(
            label == "Neptune World Series 2023, Rank 4, 8 points, 101 members, Rank unchanged"
        )
    }

    @Test
    func missingMemberCountIsOmittedRatherThanAnnouncedEmpty() {
        let label = PoolScoreItem(
            poolGamblerScore: model(position: 4, beforePosition: 3, score: 8, gamblerCount: nil),
            onOpen: {},
            onJoin: {}
        )
        .accessibilityLabel

        #expect(label == "Neptune World Series 2023, Rank 4, 8 points, Down 1 place")
    }

    @Test
    func singularValuesUseSingularNouns() {
        // n == 1 is the modal case on a board that updates every match: one point,
        // one member, one place moved. Each noun must agree with its count.
        let label = PoolScoreItem(
            poolGamblerScore: model(position: 3, beforePosition: 4, score: 1, gamblerCount: 1),
            onOpen: {},
            onJoin: {}
        )
        .accessibilityLabel

        #expect(label == "Neptune World Series 2023, Rank 3, 1 point, 1 member, Up 1 place")
    }

    @Test
    func pluralValuesUsePluralNouns() {
        let label = PoolScoreItem(
            poolGamblerScore: model(position: 3, beforePosition: 5, score: 2, gamblerCount: 2),
            onOpen: {},
            onJoin: {}
        )
        .accessibilityLabel

        #expect(label == "Neptune World Series 2023, Rank 3, 2 points, 2 members, Up 2 places")
    }

    @Test
    func zeroValuesUsePluralNouns() {
        // English and Spanish both take the "other" category at zero.
        let label = PoolScoreItem(
            poolGamblerScore: model(position: 3, beforePosition: 3, score: 0, gamblerCount: 0),
            onOpen: {},
            onJoin: {}
        )
        .accessibilityLabel

        #expect(label == "Neptune World Series 2023, Rank 3, 0 points, 0 members, Rank unchanged")
    }

    @Test
    func placeholderRowContributesNoAnnouncement() {
        // `.accessibilityHidden(isPlaceholder)` takes the same flag one line below
        // `.accessibilityLabel` in `body`, so an empty label and a true flag together
        // mean the filler model is never spoken.
        let item = PoolScoreItem(
            poolGamblerScore: poolGamblerScorePlaceholderModel(),
            onOpen: {},
            onJoin: {},
            placeholderModifier: ShimmerModifier()
        )

        #expect(item.isPlaceholder)
        #expect(item.accessibilityLabel.isEmpty)
    }

    private func model(
        position: Int?,
        beforePosition: Int?,
        score: Int?,
        gamblerCount: Int?
    ) -> PoolGamblerScoreModel {
        PoolGamblerScoreModel(
            poolId: "A3C2E1",
            poolName: "Neptune World Series 2023",
            gamblerId: "YF23H1",
            gamblerUsername: "neptune-player",
            position: position,
            beforePosition: beforePosition,
            score: score,
            gamblerCount: gamblerCount
        )
    }
}
