import Core
import SwiftUI
import ViewInspector
import XCTest
@testable import UI

final class TrendIndicatorTests: XCTestCase {
    func testPositiveDifferenceShowsAbsoluteValue() throws {
        let indicator = TrendIndicator(movement: .moved(places: 3, partialProgress: nil), textStyle: .caption2)

        XCTAssertEqual(
            try indicator.inspect().find(ViewType.Text.self).string(),
            "3"
        )
    }

    func testNegativeDifferenceShowsAbsoluteValue() throws {
        let indicator = TrendIndicator(movement: .moved(places: -2, partialProgress: nil), textStyle: .caption2)

        XCTAssertEqual(
            try indicator.inspect().find(ViewType.Text.self).string(),
            "2"
        )
    }

    func testSteadyDifferenceUsesSymbol() throws {
        let indicator = TrendIndicator(movement: .moved(places: 0, partialProgress: nil), textStyle: .caption2)

        XCTAssertNoThrow(
            try indicator.inspect().find(ViewType.Image.self)
        )
    }

    func testPartialMovementShowsItsQualifierBelowTheValue() throws {
        let indicator = TrendIndicator(
            movement: .moved(places: 3, partialProgress: KickoffGroupProgress(computedMatches: 1, totalMatches: 2)),
            textStyle: .caption2
        )

        let strings = try indicator.inspect().findAll(ViewType.Text.self).map { try $0.string() }
        XCTAssertEqual(strings.suffix(2), ["3", "1 of 2 computed"])
    }

    func testPendingAndUnavailableShowLabelsWithoutTheSteadySymbol() throws {
        for (movement, label) in [(RankMovement.pending, "Pending"), (.unavailable, "Unavailable")] {
            let indicator = TrendIndicator(movement: movement, textStyle: .caption2)

            XCTAssertEqual(try indicator.inspect().find(ViewType.Text.self).string(), label)
            XCTAssertThrowsError(try indicator.inspect().find(ViewType.Image.self))
        }
    }
}
