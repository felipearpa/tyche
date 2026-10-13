import Core
import SwiftUI
import Testing
import UIKit
@testable import Pool

/// The leaderboard row at accessibility text sizes: the rank rail never clips its movement, and
/// the username wraps instead of truncating. Measured with `UIHostingController.sizeThatFits`, so
/// no ViewInspector is involved.
@MainActor
struct GamblerScoreItemLargeTextTests {
    private let rowWidth: CGFloat = 393

    @Test(arguments: [DynamicTypeSize.accessibility3, .accessibility5])
    func aLongUsernameWrapsAtAccessibilitySizes(size: DynamicTypeSize) {
        let short = height(of: row(username: "ana"), size: size)
        let long = height(of: row(username: "Juan Pablo Rojas de la Santísima Trinidad y Montoya"), size: size)

        // Wrapping adds lines; a one-line ellipsized name would keep the row's height.
        #expect(long > short)
    }

    @Test
    func atOrdinarySizesTheUsernameStaysOnOneLine() {
        let short = height(of: row(username: "ana"), size: .large)
        let long = height(of: row(username: "Juan Pablo Rojas de la Santísima Trinidad y Montoya"), size: .large)

        #expect(long == short)
    }

    @Test(arguments: [DynamicTypeSize.large, .accessibility3, .accessibility5])
    func aPlainRailIsAtLeastAsWideAsItsMovement(size: DynamicTypeSize) {
        let movement = Text(verbatim: "↑33").font(.caption2)
        let natural = fittingSize(of: movement, size: size).width
        let rail = fittingSize(
            of: movement.modifier(RankRailWidth(hasLabel: false, tileSize: 44, labelWidth: 64)),
            size: size
        )
        .width

        #expect(rail >= natural)
        #expect(rail >= 44)
    }

    @Test
    func aLabeledRailTakesTheLabelWidthSoItsTextWraps() {
        let label = Text(verbatim: "1 of 2 computed").font(.caption2).fixedSize(horizontal: false, vertical: true)
        let rail = fittingSize(of: label.modifier(RankRailWidth(hasLabel: true, tileSize: 44, labelWidth: 64)), size: .large)

        #expect(rail.width == 64)
    }

    private func row(username: String) -> GamblerScoreItem {
        GamblerScoreItem(
            poolGamblerScore: PoolGamblerScoreModel(
                poolId: "pool",
                poolName: "Pool",
                gamblerId: "gambler",
                gamblerUsername: username,
                position: 12,
                beforePosition: 15,
                score: 1_250,
                gamblerCount: nil,
                trendGroup: KickoffGroupProgress(computedMatches: 2, totalMatches: 2)
            ),
            isCurrentUser: false
        )
    }

    private func height(of view: some View, size: DynamicTypeSize) -> CGFloat {
        UIHostingController(rootView: view.environment(\.dynamicTypeSize, size))
            .sizeThatFits(in: CGSize(width: rowWidth, height: .greatestFiniteMagnitude))
            .height
    }

    private func fittingSize(of view: some View, size: DynamicTypeSize) -> CGSize {
        UIHostingController(rootView: view.environment(\.dynamicTypeSize, size))
            .sizeThatFits(in: CGSize(width: CGFloat.greatestFiniteMagnitude, height: .greatestFiniteMagnitude))
    }
}
