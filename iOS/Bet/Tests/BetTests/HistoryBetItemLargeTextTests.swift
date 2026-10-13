import Core
import SwiftUI
import Testing
import UIKit
@testable import Bet

/// History rows at the largest accessibility text size on a 320-pt screen: nothing grows past
/// the row's width, so labels and pills wrap instead of clipping. Measured with
/// `UIHostingController.sizeThatFits`, so no ViewInspector is involved.
@MainActor
struct HistoryBetItemLargeTextTests {
    /// A 320-pt screen less History's gutters and the group's rail.
    private let rowWidth: CGFloat = 270

    @Test(arguments: [HistoryOwner.signedInGambler, .selectedGambler(name: "El mono")])
    func aPendingRowFitsANarrowWidthAtTheLargestTextSize(owner: HistoryOwner) {
        let pending = timelineBetPreviewModels()[0]

        #expect(fittingWidth(of: HistoryBetItem(poolGamblerBet: pending, owner: owner)) <= rowWidth)
    }

    @Test
    func rowsWithLongNamesFitANarrowWidthAtTheLargestTextSize() {
        for bet in historyBetPreviewModels() {
            #expect(fittingWidth(of: HistoryBetItem(poolGamblerBet: bet)) <= rowWidth, "\(bet.matchId)")
        }
    }

    @Test
    func nameWordsSplitOnWhitespace() {
        #expect(historyNameWords("República Democrática  del Congo") == ["República", "Democrática", "del", "Congo"])
        #expect(historyNameWords("Inglaterra") == ["Inglaterra"])
    }

    private func fittingWidth(of view: some View) -> CGFloat {
        UIHostingController(rootView: view.environment(\.dynamicTypeSize, .accessibility5))
            .sizeThatFits(in: CGSize(width: rowWidth, height: .greatestFiniteMagnitude))
            .width
    }
}
