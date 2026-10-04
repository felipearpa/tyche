import SwiftUI
import Testing
import ViewInspector
@testable import Bet

/// The points summary's retry, for History's own total and another gambler's Timeline total: after
/// an initial failure and after a failed refresh that keeps the last total, the summary shows its
/// failure text with an icon-only Retry beside it. The button is named "Retry" for assistive
/// technology, keeps a 44-point minimum target, and runs only the summary's retry.
@MainActor
struct HistoryPointsRetryTests {
    struct Case: CustomTestStringConvertible, Sendable {
        let owner: HistoryOwner
        let state: HistoryPointsSummaryState
        let message: String
        let testDescription: String
    }

    nonisolated private static let cases: [Case] = [
        Case(
            owner: .signedInGambler,
            state: .initial.failed(),
            message: "Couldn't load your points.",
            testDescription: "History, initial failure"
        ),
        Case(
            owner: .signedInGambler,
            state: .initial.loaded(.earned(12)).failed(),
            message: "Couldn't update your points. Showing the last total.",
            testDescription: "History, refresh failure"
        ),
        Case(
            owner: .selectedGambler(name: "El mono"),
            state: .initial.failed(),
            message: "Couldn't load points.",
            testDescription: "Timeline, initial failure"
        ),
        Case(
            owner: .selectedGambler(name: "El mono"),
            state: .initial.loaded(.earned(12)).failed(),
            message: "Couldn't update points. Showing the last total.",
            testDescription: "Timeline, refresh failure"
        ),
    ]

    @Test(arguments: cases)
    func failureShowsAnIconOnlyRetryBesideItsText(_ testCase: Case) throws {
        let header = HistoryPointsHeader(
            state: testCase.state,
            owner: testCase.owner,
            showsRefreshProgress: false,
            onRetry: {}
        )

        let row = try header.inspect().find(text: testCase.message).parent()
        let button = try row.find(HistoryPointsRetryButton.self).find(ViewType.Button.self)

        #expect(try button.accessibilityLabel().string() == "Retry")
        // The label is the glyph alone: no visible text competes with the list's Retry.
        let glyph = try button.labelView().image()
        #expect(try glyph.flexFrame().minWidth == 44)
        #expect(try glyph.flexFrame().minHeight == 44)
    }

    @Test(arguments: cases)
    func retryRunsOnlyTheSummaryRetryOnce(_ testCase: Case) throws {
        var retries = 0
        let header = HistoryPointsHeader(
            state: testCase.state,
            owner: testCase.owner,
            showsRefreshProgress: false,
            onRetry: { retries += 1 }
        )

        try header.inspect().find(HistoryPointsRetryButton.self).find(ViewType.Button.self).tap()

        #expect(retries == 1)
    }

    @Test
    func aRefreshFailureKeepsTheLastTotalAboveTheRetry() throws {
        let header = HistoryPointsHeader(
            state: .initial.loaded(.earned(12)).failed(),
            owner: .selectedGambler(name: "El mono"),
            showsRefreshProgress: false,
            onRetry: {}
        )

        let summary = try header.inspect().find(HistoryPointsSummary.self)
        #expect(try summary.actualView().points == .earned(12))
        _ = try header.inspect().find(HistoryPointsRetryButton.self)
    }

    @Test
    func aCurrentTotalOffersNoRetry() throws {
        let header = HistoryPointsHeader(
            state: .initial.loaded(.earned(12)),
            showsRefreshProgress: false,
            onRetry: {}
        )

        #expect(throws: (any Error).self) { try header.inspect().find(HistoryPointsRetryButton.self) }
    }
}
