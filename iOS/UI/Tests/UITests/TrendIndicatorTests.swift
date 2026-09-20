import SwiftUI
import ViewInspector
import XCTest
@testable import UI

final class TrendIndicatorTests: XCTestCase {
    func testPositiveDifferenceShowsAbsoluteValue() throws {
        let indicator = TrendIndicator(difference: 3, textStyle: .caption2)

        XCTAssertEqual(
            try indicator.inspect().find(ViewType.Text.self).string(),
            "3"
        )
    }

    func testNegativeDifferenceShowsAbsoluteValue() throws {
        let indicator = TrendIndicator(difference: -2, textStyle: .caption2)

        XCTAssertEqual(
            try indicator.inspect().find(ViewType.Text.self).string(),
            "2"
        )
    }

    func testSteadyDifferenceUsesSymbol() throws {
        let indicator = TrendIndicator(difference: 0, textStyle: .caption2)

        XCTAssertNoThrow(
            try indicator.inspect().find(ViewType.Image.self)
        )
    }
}
